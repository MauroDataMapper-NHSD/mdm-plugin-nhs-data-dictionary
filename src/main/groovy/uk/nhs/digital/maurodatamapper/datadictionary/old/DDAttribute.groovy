/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 */
package uk.nhs.digital.maurodatamapper.datadictionary.old

import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue

import groovy.util.logging.Slf4j
import groovy.xml.slurpersupport.GPathResult
import org.apache.commons.lang3.StringUtils

@Deprecated
@Slf4j
class DDAttribute {

    def code_system
    def value_set


    void fromXml(Object xml) {

        super.fromXml(xml)


        code_system = xml."code-system"
        value_set = xml."value-set"
        definition = xml.definition
    }

    DataElement toCatalogueItem(DataDictionary dataDictionary) {

        DataType dataType = dataDictionary.dataTypes["Default String"]

        //log.debug(code_system)
        //String valueSetTotal = code_system.Bundle.total.@value.text()
        //if( valueSetTotal && Integer.parseInt(valueSetTotal) > 0) {
        if( code_system?.Bundle?.entry?.resource?.CodeSystem?.concept?.size() > 0) {

            String enumerationTypeLabel = "Enumeration - ${DDHelperFunctions.tidyLabel(name)} (Attribute)"
            if(dataDictionary.dataTypes[enumerationTypeLabel]) {
                dataType = dataDictionary.dataTypes[enumerationTypeLabel]
            } else {
                dataType = new EnumerationType(label: enumerationTypeLabel, enumerationValues: [])
                code_system.Bundle.entry.resource.CodeSystem.concept.each { concept ->
                    String webOrder = concept.property.find { property ->
                        property.code.@value == "Web Order"
                    }.valueInteger.@value
                    String status = concept.property.find { property ->
                        property.code.@value == "Status"
                    }.valueString.@value
                    String webPresentation = concept.property.find { property ->
                        property.code.@value == "Web Presentation"
                    }.valueString.@value
                    List<String> dataElements = concept.property.findAll { property ->
                        property.code.@value == "Data Element"
                    }.collect { property ->
                        property.valueString.@value
                    }

                    if (webOrder != "0") {
                        EnumerationValue enumerationValue = new EnumerationValue()
                        enumerationValue.key = concept.code.@value
                        enumerationValue.value = concept.display.@value
                        String unquotedString = DDHelperFunctions.unquoteString(webPresentation)

                        DDHelperFunctions.addMetadata(enumerationValue, "Web Order", webOrder)
                        DDHelperFunctions.addMetadata(enumerationValue, "Status", status)

                        DDHelperFunctions.addMetadata(enumerationValue, "Web Presentation", unquotedString)
                        DDHelperFunctions.addMetadata(enumerationValue, "Data Elements", StringUtils.join(dataElements, ";"))
                        ((EnumerationType) dataType).addToEnumerationValues(enumerationValue)
                    }
                }
                if(dataType.enumerationValues.size() == 0) {
                    log.error("No enumeration Value!  Adding dummy to attribute: " + name)
                    ((EnumerationType)dataType).addToEnumerationValues(new EnumerationValue(key: "Dummy Enumeration", value: "Dummy"))
                }
                dataDictionary.dataTypes[enumerationTypeLabel] = dataType
            }
        } else if (stringValues["format_length"]) {
            String format_length = stringValues["format_length"]
            dataType = dataDictionary.dataTypes[format_length]
            if(!dataType) {
                dataType = new PrimitiveType(label: format_length)
                dataDictionary.dataTypes[format_length] = dataType
            }
        }
        if(!dataType.label) {
            log.error("Null dataType label: " + name)
        }

        catalogueItem = new DataElement(label: DDHelperFunctions.tidyLabel(name), dataType: dataType)

        catalogueItem.description = DDHelperFunctions.parseHtml(definition)

        addAllMetadata(catalogueItem)

        catalogueItem = catalogueItem

        return catalogueItem

    }
/*
    @Override
    List<Topic> toDitaTopics(DataDictionary dataDictionary) {

        Topic descriptionTopic = createDescriptionTopic()


        List<Topic> returnTopics = [descriptionTopic]

        if(!isPrepatory) {

            if (catalogueItem.dataType instanceof EnumerationType) {
                SimpleTable enumTable = new SimpleTable(relColWidth: "1* 4*")
                enumTable.stHead = new STHead(stEntries: [new STEntry(value: "Code"), new STEntry(value: "Description")])
                ((EnumerationType) catalogueItem.dataType).enumerationValues.sort {
                    enumValue ->
                        String webOrderString = DDHelperFunctions.getMetadataValue(enumValue, "Web Order")
                        if (webOrderString) {
                            return Integer.parseInt(webOrderString)
                        } else return 0

                }.each { enumValue ->

                    STEntry valueEntry = new STEntry(value: enumValue.value)
                    String webPresentation = DDHelperFunctions.getMetadataValue(enumValue, "Web Presentation")
                    if (webPresentation && webPresentation != "") {
                        valueEntry = new STEntry(bodyElement: new Html(content: webPresentation))
                    }

                    STRow row = new STRow(stEntries: [new STEntry(value: enumValue.key), valueEntry])
                    if (DDHelperFunctions.getMetadataValue(enumValue, "Status") == "retired") {
                        row.outputClass = "retired"
                    }
                    enumTable.stRows.add(row)
                }
                Topic nationalCodesTopic = createSubTopic("National Codes")
                nationalCodesTopic.body.bodyElements.add(enumTable)
                returnTopics.add(nationalCodesTopic)
            } else if (catalogueItem.dataType instanceof PrimitiveType && catalogueItem.dataType.label != "Default String") {
                Topic typeTopic = createSubTopic("Type")
                typeTopic.body.bodyElements.add(new Html(content: "<p>${((PrimitiveType) catalogueItem.dataType).label}</p>"))
                returnTopics.add(typeTopic)
            }
        }

        Topic aliasTopic = getAliasTopic()
        if(aliasTopic) {
            returnTopics.add(aliasTopic)
        }

        Topic whereUsedTopic = generateWhereUsedTopic(dataDictionary)
        if(whereUsedTopic) {
            returnTopics.add(whereUsedTopic)
        }

        Topic dataElementsTopic = createSubTopic("Data Elements")
        Ul ul = new Ul()
        dataDictionary.elements.values().each { element ->
            if (element.stringValues["element_attributes"] && element.stringValues["element_attributes"].contains(this.uin)) {
                ul.entries.add(new Li(xref: element.getXRef()))
            }
        }
        if(ul.entries.size() > 0) {
            dataElementsTopic.body.bodyElements.add(ul)
            returnTopics.add(dataElementsTopic)
        }

        return returnTopics
    }
*/

    void fromCatalogueExport(DataDictionary dataDictionary, def catalogueXml, UUID parentId, UUID containerId) {

        //clazz = xml."class"

        definition = catalogueXml.description
        name = catalogueXml.label

        catalogueId = UUID.fromString(catalogueXml.id)
        this.parentCatalogueId = parentId
        this.containerCatalogueId = containerId

        super.fromCatalogueExport(dataDictionary, catalogueXml, parentId, containerId)

        catalogueItem = new DataElement(catalogueXml)


    }


    void fromCatalogueItem(DataDictionary dataDictionary, DataElement dataElement, UUID parentId, UUID containerId, MetadataService metadataService) {

        //clazz = xml."class"

        definition = dataElement.description
        name = dataElement.label

        catalogueId = dataElement.id
        this.parentCatalogueId = parentId
        this.containerCatalogueId = containerId

        super.fromCatalogueItem(dataDictionary, dataElement, parentId, containerId, metadataService)

        catalogueItem = dataElement
    }


/*    @Override
    void updateDescriptionInCatalogue(BindingMauroDataMapperClient mdmClient) {
        mdmClient.updateDataElementDescription(containerCatalogueId, parentCatalogueId, catalogueId, definition.toString())
    }
*/


    String getDitaKey() {
        return "attribute_" + DDHelperFunctions.makeValidDitaName(name)
    }

    String getOutputClassForLink() {
        String outputClass = "attribute"
        if(stringValues["isRetired"] == "true") {
            outputClass += " retired"
        }
        return outputClass

    }


    String getOutputClassForTable() {
        return "Attribute"
    }


    String getTypeText() {
        return "attribute"
    }

    String getTypeTitle() {
        return "Attribute"
    }

    String getShortDesc(DataDictionary dataDictionary) {
        if(isPrepatory) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                GPathResult xml
                xml = xmlSlurper.parseText("<xml>" + DDHelperFunctions.parseHtml(definition.toString()) + "</xml>")
                String firstParagraph = xml.p[0].text()
                if (xml.p.size() == 0) {
                    firstParagraph = xml.text()
                }
                String firstSentence = firstParagraph.substring(0, firstParagraph.indexOf(".") + 1)
                return firstSentence
            } catch (Exception e) {
                log.error("Couldn't parse: " + definition)
                return name
            }
        }
    }

    String getInternalLink() {
        if(isRetired) {
            return "](dm:${DataDictionary.CORE_MODEL_NAME}|dc:${DataDictionary.ATTRIBUTES_CLASS_NAME}|dc:Retired|de:${DDHelperFunctions.tidyLabel(name)})"
        } else {
            return "](dm:${DataDictionary.CORE_MODEL_NAME}|dc:${DataDictionary.ATTRIBUTES_CLASS_NAME}|de:${DDHelperFunctions.tidyLabel(name)})"
        }
    }


}
