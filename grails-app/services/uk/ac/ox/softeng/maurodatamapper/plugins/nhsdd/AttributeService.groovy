package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType

import grails.gorm.transactions.Transactional
import groovy.util.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionaryOptions
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Li
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STEntry
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STHead
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STRow
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.SimpleTable
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Topic
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Ul

@Transactional
class AttributeService extends DataDictionaryComponentService<DataElement> {

    @Override
    Map indexMap(DataElement object) {
        [
            catalogueId: object.id.toString(),
            name: object.label,
            stereotype: "attribute"
        ]
    }

    @Override
    def show(String branch, String id) {
        DataElement dataElement = dataElementService.get(id)

        String description = convertLinksInDescription(branch, dataElement.description)
        String shortDescription = replaceLinksInShortDescription(getShortDescription(dataElement))
        def result = [
                catalogueId: dataElement.id.toString(),
                name: dataElement.label,
                stereotype: "attribute",
                shortDescription: shortDescription,
                description: description,
                alsoKnownAs: getAliases(dataElement)
        ]

        if (dataElement.dataType instanceof EnumerationType) {

            List<Map> enumerationValues = []

            ((EnumerationType) dataElement.dataType).enumerationValues.sort {
                enumValue ->
                    String webOrderString = DDHelperFunctions.getMetadataValue(enumValue, "Web Order")
                    if (webOrderString) {
                        return Integer.parseInt(webOrderString)
                    } else return 0

            }.each { enumValue ->

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

        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(DataDictionary.DATA_DICTIONARY_CORE_MODEL_NAME)
        String uin = DDHelperFunctions.getMetadataValue(dataElement, "uin")
        coreModel.dataClasses.find{it.label == DataDictionary.DATA_DICTIONARY_DATA_FIELD_NOTES_CLASS_NAME}.dataElements.each {relatedDataElement ->
            String elementAttributes = DDHelperFunctions.getMetadataValue(relatedDataElement, "element_attributes")
            if(elementAttributes && elementAttributes.contains(uin)) {
                Map relatedElement = [
                        catalogueId: relatedDataElement.id.toString(),
                        name: relatedDataElement.label,
                        stereotype: "element",
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
    String getShortDescription(DataElement attribute) {
        if(isPreparatory(attribute)) {
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

}
