package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
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
        DDHelperFunctions.metadataNamespace + ".element"
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

    @Override
    String getShortDescription(DataElement dataElement, DataDictionary dataDictionary) {
        if(isPreparatory(dataElement)) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + DDHelperFunctions.parseHtml(dataElement.description.toString()) + "</xml>")
                String firstParagraph = xml.p[0].text()
                if (xml.p.size() == 0) {
                    firstParagraph = xml.text()
                }
                String firstSentence = firstParagraph.substring(0, firstParagraph.indexOf(".") + 1)
                if (firstSentence.toLowerCase().contains("is the same as")) {
                    List<DataElement> attributes = getElementAttributes(dataElement, dataDictionary)
                    if (attributes.size() == 1) {
                        firstSentence = attributeService.getShortDescription(attributes[0], dataDictionary)
                    } else {
                        firstSentence = dataElement.label
                    }
                }
                return firstSentence
            } catch (Exception e) {
                log.error("Couldn't parse: " + dataElement.description)
                return dataElement.label
            }
        }
    }

    void ingestFromXml(def xml, Folder dictionaryFolder, DataModel coreDataModel, String currentUserEmailAddress,
                      NhsDataDictionary nhsDataDictionary) {

        Map<String, PrimitiveType> primitiveTypes = [:]
        coreDataModel.dataTypes.each {
            if (it instanceof PrimitiveType) {
                primitiveTypes[it.label] = it
            }
        }

        Folder dataElementCodeSetsFolder =
            new Folder(label: "Data Element CodeSets", createdBy: currentUserEmailAddress)
        dictionaryFolder.addToChildFolders(dataElementCodeSetsFolder)
        if (!folderService.validate(dataElementCodeSetsFolder)) {
            throw new ApiInvalidModelException('NHSDD', 'Invalid model', dataElementCodeSetsFolder.errors)
        }
        folderService.save(dataElementCodeSetsFolder)

        DataClass allElementsClass = new DataClass(label: NhsDataDictionary.DATA_FIELD_NOTES_CLASS_NAME, createdBy: currentUserEmailAddress)

        DataClass retiredElementsClass = new DataClass(label: "Retired", createdBy: currentUserEmailAddress,
                                                       parentDataClass: allElementsClass)
        allElementsClass.addToDataClasses(retiredElementsClass)
        coreDataModel.addToDataClasses(allElementsClass)
        coreDataModel.addToDataClasses(retiredElementsClass)


        Map<String, Folder> folders = [:]
        int idx = 0
        xml.DDDataElement.sort {it.TitleCaseName.text()}.each {ddDataElement ->
            DataType dataType
            String elementName = ddDataElement.TitleCaseName[0].text()
            String description = DDHelperFunctions.parseHtml(ddDataElement.definition)
            boolean isRetired = ddDataElement.isRetired.text() == "true"
                if (ddDataElement."value-set".size() > 0 > 0 && !isRetired) {
                    String codeSetName = elementName
                    String capitalizedCodeSetName = ddDataElement.name.text()
                    String folderName = codeSetName.substring(0, 1)
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
                    label: elementName,
                    description: "",
                    folder: subFolder,
                    createdBy: currentUserEmailAddress,
                    authority: authorityService.defaultAuthority)
                String version = ddDataElement."value-set".Bundle.entry.expansion.parameter.Bundle.entry.resource.CodeSystem.version."@value".text()
                codeSet.addToMetadata(new Metadata(namespace: "uk.nhs.datadictionary.codeset", key: "version", value: version))


                    String terminologyUin = ddDataElement.link.participant.find {it -> it.@role == 'Supplier'}.@referencedUin
                    Terminology attributeTerminology = nhsDataDictionary.attributeTerminologiesByName[terminologyUin]
                    if(!attributeTerminology) {
                        log.error("No terminology with UIN ${terminologyUin} found for element ${elementName}")
                    } else {
                        ddDataElement."value-set".Bundle.entry.expansion.parameter.Bundle.entry.resource.CodeSystem.concept.each {concept ->
                            if (concept.property.find {property ->
                                property.code.@value.toString() == "Data Element" &&
                                property.valueString.@value.toString() == capitalizedCodeSetName
                            }) {
                                Term t = attributeTerminology.terms.find {it -> it.code == concept.code.@value.toString()}
                                if (!t) {
                                    log.error("Cannot find term: ${concept.code.@value}")
                                } else {
                                    codeSet.addToTerms(t)
                                }
                            }
                        }
                    }
                    if (codeSet.validate()) {
                        codeSet = codeSetService.saveModelWithContent(codeSet)
                    } else {
                        GormUtils.outputDomainErrors(messageSource, codeSet) // TODO throw exception???
                    }
                    dataType = new ModelDataType(label: "${elementName} Element Type",
                                                 modelResourceDomainType: codeSet.getDomainType(),
                                                 modelResourceId: codeSet.id,
                                                 createdBy: currentUserEmailAddress)
                    coreDataModel.addToDataTypes(dataType)
                } else {
                    // no "value-set" nodes
                    String formatLength = ddDataElement."format-length".text()
                    if (!formatLength) {
                        formatLength = "String"
                    }
                    dataType = primitiveTypes[formatLength]
                    if (!dataType) {
                        dataType = new PrimitiveType(label: formatLength, createdBy: currentUserEmailAddress)
                        primitiveTypes[formatLength] = dataType
                        coreDataModel.addToDataTypes(dataType)
                    }
                }
                DataElement elementDataElement = new DataElement(
                    label: elementName,
                    description: description,
                    createdBy: currentUserEmailAddress,
                    dataType: dataType,
                    index: idx++)

            addMetadataFromXml(elementDataElement, ddDataElement, currentUserEmailAddress)

            String elementAttributes = StringUtils.join(ddDataElement."link".collect {link ->
                link.participant.find{p -> p["@role"] == "Supplier"}["@referencedUin"]
            }.collect {
                nhsDataDictionary.attributeElementsByUin[it]
            }.collect {
                it.label
            }, ";")

            addToMetadata(elementDataElement, "linkedAttributes", elementAttributes, currentUserEmailAddress)

            String shortDescription = getShortDesc(description, elementDataElement, nhsDataDictionary)
            addToMetadata(elementDataElement, "shortDescription", shortDescription, currentUserEmailAddress)

            nhsDataDictionary.elementsByUrl[ddDataElement.DD_URL.text()] = elementDataElement
            if (isRetired) {
                retiredElementsClass.addToDataElements(elementDataElement)
            } else {
                allElementsClass.addToDataElements(elementDataElement)
            }
        }
    }

    String getShortDesc(String definition, DataElement dataElement, NhsDataDictionary dataDictionary) {
        boolean isPreparatory = dataElement.metadata.any { it.key == "isPreparatory" && it.value == "true" }

        if(isPreparatory) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + definition + "</xml>")
                String firstParagraph = xml.p[0].text()
                if (xml.p.size() == 0) {
                    firstParagraph = xml.text()
                }
                String firstSentence = firstParagraph.substring(0, firstParagraph.indexOf(".") + 1)
                if (firstSentence.toLowerCase().contains("is the same as")) {
                    String linkedAttributesString = dataElement.metadata.find { it.key == "linkedAttributes" }?.value
                    String[] attributes = []
                    if(linkedAttributesString) {
                        attributes = linkedAttributesString.split(";")
                    }
                    if (attributes.length == 1) {
                        firstSentence = dataDictionary.attributeElementsByName[attributes[0]].metadata.find {it.key == "shortDescription"}?.value
                    } else {
                        firstSentence = dataElement.label
                    }
                }
                return firstSentence
            } catch (Exception e) {
                e.printStackTrace()
                log.error("Couldn't parse: ${definition}")
                return dataElement.label
            }
        }
    }


}
