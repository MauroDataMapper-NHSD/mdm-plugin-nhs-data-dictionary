package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ModelDataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import org.apache.commons.lang3.StringUtils
import uk.nhs.digital.maurodatamapper.datadictionary.DDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDCode
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDElement

@Slf4j
@Transactional
class ElementService extends DataDictionaryComponentService<DataElement> {

    AttributeService attributeService

    @Override
    Map indexMap(DataElement object) {
        [
            catalogueId: object.id.toString(),
            name: object.label,
            stereotype: "element"
        ]
    }

    @Override
    def show(String branch, String id) {
        DataElement dataElement = dataElementService.get(id)

        String description = convertLinksInDescription(branch, dataElement.description)

        DataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(branch)
        String shortDesc = replaceLinksInShortDescription(getShortDescription(dataElement, null))
        def result = [
                catalogueId: dataElement.id.toString(),
                name: dataElement.label,
                stereotype: "element",
                shortDescription: shortDesc,
                description: description,
                alsoKnownAs: getAliases(dataElement)
        ]
        String formatLength = DDHelperFunctions.getMetadataValue(dataElement, "format-length")

        if(!isRetired(dataElement) && formatLength) {
            result["formatLength"] = formatLength
        }

        List<DataElement> elementAttributes = getElementAttributes(dataElement, dataDictionary)

       if (dataElement.dataType instanceof EnumerationType) {
            List<EnumerationValue> enumValues = ((EnumerationType) dataElement.dataType).enumerationValues.sort {
                enumValue ->
                    String webOrderString = DDHelperFunctions.getMetadataValue(enumValue, "Web Order")
                    if (webOrderString) {
                        return Integer.parseInt(webOrderString)
                    } else return 0
            }
            List<EnumerationValue> permittedNationalCodes = enumValues.findAll {
                DDHelperFunctions.getMetadataValue(it, "Permitted National Code") == "true"
            }
            if (permittedNationalCodes.size() > 0) {
                String nationalCodesTitle = "permittedNationalCodes"
                if(elementAttributes.size() == 1 &&
                        elementAttributes.get(0).dataType.getClass() == EnumerationType.class) {
                    List<EnumerationValue> attributeValues =
                            ((EnumerationType)elementAttributes.get(0).dataType).getEnumerationValues()
                    if(attributeValues.size() == permittedNationalCodes.size()) {
                        nationalCodesTitle = "nationalCodes"
                    }
                }

                result[nationalCodesTitle] = generateCodeList(permittedNationalCodes)
            }
            List<EnumerationValue> nationalCodes = enumValues.findAll {
                !DDHelperFunctions.getMetadataValue(it, "Permitted National Code") &&
                        DDHelperFunctions.getMetadataValue(it, "Web Order") != "0"
            }
            if (nationalCodes.size() > 0) {
                String nationalCodesTitle = "permittedNationalCodes"
                if(elementAttributes.size() == 1 &&
                        elementAttributes.get(0).dataType.getClass() == EnumerationType.class) {
                    Set<EnumerationValue> attributeValues =
                            ((EnumerationType)getElementAttributes(dataElement, dataDictionary).get(0).dataType).getEnumerationValues()
                    if(attributeValues.size() == nationalCodes.size()) {
                        nationalCodesTitle = "nationalCodes"
                    }
                }
                result[nationalCodesTitle] = generateCodeList(nationalCodes)
            }
            List<EnumerationValue> defaultCodes = enumValues.findAll {
                //DDHelperFunctions.getMetadataValue(it, "Default Code") == "true"
                DDHelperFunctions.getMetadataValue(it, "Web Order") == "0"
            }
            if (defaultCodes.size() > 0) {
                result["defaultCodes"] = generateCodeList(defaultCodes)
            }
        }

        if(elementAttributes.size() > 0) {
            List<Map> elementAttributesList = []
            elementAttributes.each {attribute ->
                Map attributeMap = [
                    catalogueId: attribute.id.toString(),
                    name: attribute.label,
                    stereotype: "attribute",
                ]
                elementAttributesList.add(attributeMap)
            }
            result["attributes"] = elementAttributesList
        }
        return result
    }

    @Override
    Set<DataElement> getAll() {
        nhsDataDictionaryService.getDataFieldNotes(false)
    }

    @Override
    DataElement getItem(UUID id) {
        dataElementService.get(id)
    }

    @Override
    String getMetadataNamespace() {
        uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary.METADATA_NAMESPACE +  ".element"
    }

    List<DataElement> getElementAttributes(DataElement dataElement, DataDictionary dataDictionary) {
        List<DataElement> attributeElements = []
        String elementAttributeList = DDHelperFunctions.getMetadataValue(dataElement, "element_attributes")
        if(elementAttributeList) {
            StringUtils.split(elementAttributeList, ",").each { uin ->
                DDAttribute attribute = dataDictionary.attributes[uin]
                if (attribute) {
                    attributeElements.add(attribute.catalogueItem)
                } else {
                    log.error("Cannot find uin: " + uin)
                }
            }
        }
        return attributeElements
    }

    List<Map> generateCodeList(List<EnumerationValue> codes) {
        List<Map> returnCodesList = []
        codes.each {code ->
            Map codeMap = [:]
            String label = DDHelperFunctions.getMetadataValue(code, "label")
            codeMap["code"] = label?:code.key
            String webPresentation = DDHelperFunctions.getMetadataValue(code, "Web Presentation")
            codeMap["value"] = webPresentation?:code.value
            String retired = DDHelperFunctions.getMetadataValue(code, "isRetired")
            codeMap["retired"] = retired

            returnCodesList.add(codeMap)
        }
        return returnCodesList
    }

    void persistElements(uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary dataDictionary,
                           VersionedFolder dictionaryFolder, DataModel coreDataModel, String currentUserEmailAddress,
                        Map<String, Terminology> attributeTerminologiesByName) {

        PrimitiveType stringDataType = coreDataModel.getPrimitiveTypes().find { it.label == "String" }
        if(!stringDataType) {
            stringDataType = new PrimitiveType(label: "String", createdBy: currentUserEmailAddress)
            coreDataModel.addToDataTypes(stringDataType)
        }

        Folder dataElementCodeSetsFolder =
            new Folder(label: "Data Element CodeSets", createdBy: currentUserEmailAddress)
        dictionaryFolder.addToChildFolders(dataElementCodeSetsFolder)
        if (!folderService.validate(dataElementCodeSetsFolder)) {
            throw new ApiInvalidModelException('NHSDD', 'Invalid model', dataElementCodeSetsFolder.errors)
        }
        folderService.save(dataElementCodeSetsFolder)

        DataClass allElementsClass = new DataClass(label: uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary.DATA_FIELD_NOTES_CLASS_NAME, createdBy:
            currentUserEmailAddress)

        DataClass retiredElementsClass = new DataClass(label: "Retired", createdBy: currentUserEmailAddress,
                                                       parentDataClass: allElementsClass)
        allElementsClass.addToDataClasses(retiredElementsClass)
        coreDataModel.addToDataClasses(allElementsClass)
        coreDataModel.addToDataClasses(retiredElementsClass)


        Map<String, Folder> folders = [:]
        int idx = 0
        // Would sort, but assume already sorted
        dataDictionary.elements.each {name, element ->
            DataType dataType
            if (element.codes.size() > 0 && !element.isRetired()) {
                String folderName = name.substring(0, 1).toUpperCase()
                Folder subFolder = folders[folderName]
                if (!subFolder) {
                    subFolder = new Folder(label: folderName, createdBy: currentUserEmailAddress)
                    dataElementCodeSetsFolder.addToChildFolders(subFolder)
                    if (!folderService.validate(subFolder)) {
                        throw new ApiInvalidModelException('NHSDD', 'Invalid model', subFolder.errors)
                    }
                    folderService.save(subFolder)
                    folders[folderName] = subFolder
                }


                CodeSet codeSet = new CodeSet(
                    label: name,
                    description: "",
                    folder: subFolder,
                    createdBy: currentUserEmailAddress,
                    authority: authorityService.defaultAuthority)
                codeSet.addToMetadata(new Metadata(namespace: "uk.nhs.datadictionary.codeset", key: "version", value: element.codeSetVersion))


                // String terminologyUin = ddDataElement.link.participant.find {it -> it.@role == 'Supplier'}.@referencedUin
                // Terminology attributeTerminology = dataDictionary.attributeTerminologiesByName[terminologyUin]
                element.codes.each {code ->
                    Terminology attributeTerminology = attributeTerminologiesByName[code.owningAttribute.name]
                    if(!attributeTerminology) {
                        log.error("No terminology with name ${code.owningAttribute.name} found for element ${name}")
                    } else {
                        Term t = attributeTerminology.terms.find {term -> term.code == code.code}
                        if (!t) {
                            log.error("Cannot find term: ${code.code}")
                        } else {
                            codeSet.addToTerms(t)
                        }
                    }

                }
                if (codeSet.validate()) {
                    codeSet = codeSetService.saveModelWithContent(codeSet)
                } else {
                    GormUtils.outputDomainErrors(messageSource, codeSet) // TODO throw exception???
                }
                dataType = new ModelDataType(label: "${name} Element Type",
                                             modelResourceDomainType: codeSet.getDomainType(),
                                             modelResourceId: codeSet.id,
                                             createdBy: currentUserEmailAddress)
                coreDataModel.addToDataTypes(dataType)
            } else {
                // no "value-set" nodes
                dataType = stringDataType
            }
            DataElement elementDataElement = new DataElement(
                label: name,
                description: element.definition,
                createdBy: currentUserEmailAddress,
                dataType: dataType,
                index: idx++)

            addMetadataFromComponent(elementDataElement, element, currentUserEmailAddress)

            String elementAttributes = StringUtils.join(element.instantiatesAttributes.collect {it.name }, ";")

            addToMetadata(elementDataElement, "linkedAttributes", elementAttributes, currentUserEmailAddress)


            if (element.isRetired()) {
                retiredElementsClass.addToDataElements(elementDataElement)
            } else {
                allElementsClass.addToDataElements(elementDataElement)
            }
            dataDictionary.elementsByUrl[element.otherProperties["ddUrl"]] = elementDataElement
        }

    }

    NhsDDElement elementFromDataElement(DataElement de) {
        NhsDDElement element = new NhsDDElement()
        nhsDataDictionaryComponentFromItem(de, element)
        return element
    }

}
