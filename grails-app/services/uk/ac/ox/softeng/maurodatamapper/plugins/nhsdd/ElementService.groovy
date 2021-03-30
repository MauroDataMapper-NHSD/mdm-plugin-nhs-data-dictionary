package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue

import grails.gorm.transactions.Transactional
import groovy.util.slurpersupport.GPathResult
import org.apache.commons.lang3.StringUtils
import uk.nhs.digital.maurodatamapper.datadictionary.DDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

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
                            ((EnumerationType)getElementAttributes(dataDictionary).get(0).catalogueItem.dataType).getEnumerationValues()
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

}
