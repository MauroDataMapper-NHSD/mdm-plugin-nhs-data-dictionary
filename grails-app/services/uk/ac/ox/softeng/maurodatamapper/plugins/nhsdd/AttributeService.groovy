package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ModelDataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

@Slf4j
@Transactional
class AttributeService extends DataDictionaryComponentService<DataElement> {

    FolderService folderService

    @Override
    Map indexMap(DataElement object) {
        [
            catalogueId: object.id.toString(),
            name       : object.label,
            stereotype : "attribute"
        ]
    }

    @Override
    def show(String branch, String id) {
        DataElement dataElement = dataElementService.get(id)

        String description = convertLinksInDescription(branch, dataElement.description)
        String shortDesc = replaceLinksInShortDescription(getShortDescription(dataElement, null))
        def result = [
            catalogueId     : dataElement.id.toString(),
            name            : dataElement.label,
            stereotype      : "attribute",
            shortDescription: shortDesc,
            description     : description,
            alsoKnownAs     : getAliases(dataElement)
        ]

        if (dataElement.dataType instanceof EnumerationType) {

            List<Map> enumerationValues = []

            ((EnumerationType) dataElement.dataType).enumerationValues.sort {
                enumValue ->
                    String webOrderString = DDHelperFunctions.getMetadataValue(enumValue, "Web Order")
                    if (webOrderString) {
                        return Integer.parseInt(webOrderString)
                    } else return 0

            }.each {enumValue ->

                Map enumValueMap = [:]

                enumValueMap["code"] = enumValue.key
                enumValueMap["description"] = enumValue.value
                String webPresentation = DDHelperFunctions.getMetadataValue(enumValue, "Web Presentation")
                if (webPresentation && webPresentation != "") {
                    enumValueMap["description"] = webPresentation
                }

                if (DDHelperFunctions.getMetadataValue(enumValue, "Status") == "retired") {
                    enumValueMap["retired"] = "true"
                }
                enumerationValues.add(enumValueMap)
            }
            result["nationalCodes"] = enumerationValues
        } else if (dataElement.dataType instanceof PrimitiveType && dataElement.dataType.label != "Default String") {
            result["type"] = ((PrimitiveType) dataElement.dataType).label
        }

        List<Map> dataElements = []

        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(DataDictionary.CORE_MODEL_NAME)
        String uin = DDHelperFunctions.getMetadataValue(dataElement, "uin")
        coreModel.dataClasses.find {it.label == DataDictionary.DATA_FIELD_NOTES_CLASS_NAME}.dataElements.each {relatedDataElement ->
            String elementAttributes = DDHelperFunctions.getMetadataValue(relatedDataElement, "element_attributes")
            if (elementAttributes && elementAttributes.contains(uin)) {
                Map relatedElement = [
                    catalogueId: relatedDataElement.id.toString(),
                    name       : relatedDataElement.label,
                    stereotype : "element",
                ]
                dataElements.add(relatedElement)
            }
        }

        result["dataElements"] = dataElements

        return result
    }

    @Override
    Set<DataElement> getAll() {
        nhsDataDictionaryService.getAttributes(false)
    }

    @Override
    DataElement getItem(UUID id) {
        dataElementService.get(id)
    }

    @Override
    String getMetadataNamespace() {
        DDHelperFunctions.metadataNamespace + ".attribute"
    }

    @Override
    String getShortDescription(DataElement attribute, DataDictionary dataDictionary) {
        if (isPreparatory(attribute)) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + DDHelperFunctions.parseHtml(attribute.description.toString()) + "</xml>")
                String firstParagraph = xml.p[0].text()
                if (xml.p.size() == 0) {
                    firstParagraph = xml.text()
                }
                String firstSentence = firstParagraph.substring(0, firstParagraph.indexOf(".") + 1)
                return firstSentence
            } catch (Exception e) {
                log.error("Couldn't parse: " + attribute.description)
                return attribute.label
            }
        }
    }

    void ingestFromXml(def xml, Folder dictionaryFolder, DataModel coreDataModel, String currentUserEmailAddress,
                       NhsDataDictionary nhsDataDictionary) {
        Folder attributeTerminologiesFolder =
            new Folder(label: "Attribute Terminologies", createdBy: currentUserEmailAddress)
        dictionaryFolder.addToChildFolders(attributeTerminologiesFolder)
        if (!folderService.validate(attributeTerminologiesFolder)) {
            throw new ApiInvalidModelException('NHSDD', 'Invalid model', attributeTerminologiesFolder.errors)
        }
        folderService.save(attributeTerminologiesFolder)

        DataClass allAttributesClass = new DataClass(label: NhsDataDictionary.ATTRIBUTES_CLASS_NAME, createdBy: currentUserEmailAddress)

        DataClass retiredAttributesClass = new DataClass(label: "Retired", createdBy: currentUserEmailAddress,
                                                         parentDataClass: allAttributesClass)
        allAttributesClass.addToDataClasses(retiredAttributesClass)
        coreDataModel.addToDataClasses(allAttributesClass)
        coreDataModel.addToDataClasses(retiredAttributesClass)

        Map<String, PrimitiveType> primitiveTypes = [:]
        Map<String, Folder> folders = [:]
        int idx = 0
        xml.DDAttribute.sort {it.TitleCaseName.text()}.each {ddAttribute ->
            DataType dataType
            String attributeName = ddAttribute.TitleCaseName.text()
            String uin = ddAttribute.uin.text()
            // if(attributeName.toLowerCase().startsWith('a')) {
            if (ddAttribute."code-system".size() > 0) {
                String folderName = attributeName.substring(0, 1)
                Folder subFolder = folders[folderName]
                if (!subFolder) {
                    subFolder = new Folder(label: folderName, createdBy: currentUserEmailAddress)
                    attributeTerminologiesFolder.addToChildFolders(subFolder)
                    if (!folderService.validate(subFolder)) {
                        throw new ApiInvalidModelException('NHSDD', 'Invalid model', subFolder.errors)
                    }
                    folderService.save(subFolder)
                    folders[folderName] = subFolder
                }
                // attributeTerminologiesFolder.save()

                Terminology terminology = new Terminology(
                    label: attributeName,
                    folder: subFolder,
                    createdBy: currentUserEmailAddress,
                    authority: authorityService.defaultAuthority)
                String version = ddAttribute."code-system"[0].Bundle.entry.resource.CodeSystem.version."@value".text()
                terminology.addToMetadata(new Metadata(namespace: "uk.nhs.datadictionary.terminology", key: "version", value: version))

                ddAttribute."code-system"[0].Bundle.entry.resource.CodeSystem.concept.each {concept ->
                    Term term = new Term(
                        code: concept.code.@value.toString(),
                        definition: concept.display.@value.toString(),
                        createdBy: currentUserEmailAddress,
                        label: concept.code.@value.toString() + " : " + concept.display.@value.toString(),
                        depth: 1,
                        terminology: terminology
                    )
                    String publishDate = concept.property.find {it.code."@value" == "Publish Date"}?.valueDateTime?."@value"?.text()
                    Boolean isRetired = concept.property.find {it.code."@value" == "Status"}?.valueString?."@value"?.text() == "retired"
                    String retiredDate = concept.property.find {it.code."@value" == "Retired Date"}?.valueDateTime?."@value"?.text()
                    String webOrder = concept.property.find {it.code."@value" == "Web Order"}?.valueInteger?."@value"?.text()
                    String webPresentation = concept.property.find {it.code."@value" == "Web Presentation"}?.valueString?."@value"?.text()
                    Boolean isDefault = (webOrder == "0")
                    if (publishDate) {
                        term.addToMetadata(new Metadata(namespace: "uk.nhs.datadictionary.term",
                                                        key: "publishDate",
                                                        value: publishDate))
                    }
                    term.addToMetadata(new Metadata(namespace: "uk.nhs.datadictionary.term",
                                                    key: "isRetired",
                                                    value: isRetired))
                    if (isRetired) {
                        term.addToMetadata(new Metadata(namespace: "uk.nhs.datadictionary.term",
                                                        key: "retiredDate",
                                                        value: retiredDate))
                    }
                    term.addToMetadata(new Metadata(namespace: "uk.nhs.datadictionary.term",
                                                    key: "webOrder",
                                                    value: webOrder))
                    if (webPresentation) {
                        term.addToMetadata(new Metadata(namespace: "uk.nhs.datadictionary.term",
                                                        key: "webPresentation",
                                                        value: webPresentation))
                    }
                    term.addToMetadata(new Metadata(namespace: "uk.nhs.datadictionary.term",
                                                    key: "isDefault",
                                                    value: isDefault))

                    //log.info("    " + term.label)
                    terminology.addToTerms(term)
                }
                if (terminology.validate()) {
                    terminology = terminologyService.saveModelWithContent(terminology)
                    terminology.terms.each {
                        //
                    }
                } else {
                    log.debug(terminology.errors)
                }


                nhsDataDictionary.attributeTerminologiesByName[uin] = terminology
                dataType = new ModelDataType(label: "${attributeName} Attribute Type",
                                             modelResourceDomainType: terminology.getDomainType(),
                                             modelResourceId: terminology.id,
                                             createdBy: currentUserEmailAddress)
                coreDataModel.addToDataTypes(dataType)
            } else {
                // no "code-system" nodes
                String formatLength = ddAttribute."format-length".text()
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

            DataElement attributeDataElement = new DataElement(
                label: attributeName,
                description: DDHelperFunctions.parseHtml(ddAttribute.definition),
                createdBy: currentUserEmailAddress,
                dataType: dataType,
                index: idx++)

            addMetadataFromXml(attributeDataElement, ddAttribute, currentUserEmailAddress)

            if (ddAttribute.isRetired.text() == "true") {
                retiredAttributesClass.addToDataElements(attributeDataElement)
            } else {
                allAttributesClass.addToDataElements(attributeDataElement)
            }
            nhsDataDictionary.attributeElementsByUin[uin] = attributeDataElement

            //}
        } // for each ddAttribute

    }

}
