package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue

import groovy.util.logging.Slf4j
import groovy.xml.slurpersupport.GPathResult
import org.apache.commons.lang3.StringUtils
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Li
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STEntry
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STHead
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STRow
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.SimpleTable
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Topic
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Ul

@Slf4j
class DDElement extends DataDictionaryComponent<DataElement> {

    def code_system
    def value_set
    def default_codes
    def national_codes
    def permitted_national_codes
    //def extraFieldHeads

    List<Object> defaultCodes = []

    DataDictionaryComponent formatLinkMatchedItem
    @Override
    void fromXml(Object xml) {

        super.fromXml(xml)

        code_system = xml."code-system"
        value_set = xml."value-set"
        definition = xml.definition
        default_codes = xml."default-codes"
        national_codes = xml."national-codes"
        permitted_national_codes = xml."permitted-national-codes"
        stringValues["element_attributes"] = StringUtils.join(xml."link".collect {link ->
            link.participant.find{p -> p["@role"] == "Supplier"}["@referencedUin"]

        }, ",")
        // extraFieldHeads = xml.ExtraFieldhead

        if(xml.DefaultCode.size() > 0) {
            xml.DefaultCode.each { DefaultCode ->
                defaultCodes.add(DefaultCode)
            }
        }
    }

    @Override
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


    @Override
    DataElement toCatalogueItem(DataDictionary dataDictionary) {

        DataType dataType = dataDictionary.dataTypes["Default String"]

        List<EnumerationValue> foundEnumerationValues = []

        if(value_set?.Bundle?.entry?.expansion?.parameter?.Bundle?.entry?.resource?.CodeSystem?.concept?.size() > 0) {
            value_set.Bundle.entry.expansion.parameter.Bundle.entry.resource.CodeSystem.concept.each { concept ->
                String webOrder = concept.property.find { property ->
                    property.code.@value == "Web Order"
                }.valueInteger.@value.toString()
                String status = concept.property.find { property ->
                    property.code.@value == "Status"
                }.valueString.@value.toString()
                String webPresentation = concept.property.find { property ->
                    property.code.@value == "Web Presentation"
                }.valueString.@value
                List<String> dataElements = concept.property.findAll { property ->
                    property.code.@value == "Data Element"
                }.collect { property ->
                    property.valueString.@value.toString()
                }


                if (dataElements.find{it.equalsIgnoreCase(name)}) {
                    //log.debug(concept.display.@value)
                    EnumerationValue enumerationValue = new EnumerationValue()
                    enumerationValue.key = concept.code.@value
                    enumerationValue.value = concept.display.@value
                    String unquotedString = DDHelperFunctions.unquoteString(webPresentation)

                    DDHelperFunctions.addMetadata(enumerationValue, "Web Order", webOrder)
                    DDHelperFunctions.addMetadata(enumerationValue, "Status", status)
                    DDHelperFunctions.addMetadata(enumerationValue, "Web Presentation", unquotedString)
                    DDHelperFunctions.addMetadata(enumerationValue, "Data Elements", StringUtils.join(dataElements, ";"))
                    if(webOrder == 0) {
                        DDHelperFunctions.addMetadata(enumerationValue, "Default Code", "true")
                    }
                    foundEnumerationValues.add(enumerationValue)
                }
            }
        }
/*        if (extraFieldHeads.size() > 0) {

            extraFieldHeads.sort {Integer.parseInt(it.webOrder.text())}
                    .each { extraFieldHead ->
                        String label = DDHelperFunctions.unquoteString(extraFieldHead.label.text())
                        String description = DDHelperFunctions.unquoteString(extraFieldHead.description.text())
                        String webOrder = extraFieldHead.webOrder.text()

                        EnumerationValue enumerationValue = new EnumerationValue()
                        enumerationValue.key = webOrder
                        enumerationValue.value = "Extra Field Head " + webOrder

                        DDHelperFunctions.addMetadata(enumerationValue, "Web Order", webOrder)
                        DDHelperFunctions.addMetadata(enumerationValue, "label", label)
                        DDHelperFunctions.addMetadata(enumerationValue, "description", description)
                        DDHelperFunctions.addMetadata(enumerationValue, "ExtraFieldHead", "true")
                        foundEnumerationValues.add(enumerationValue)
                    }


        }

 */
        if (permitted_national_codes && permitted_national_codes.table.tbody.tr.size() > 0) {

            permitted_national_codes.table.tbody.tr.eachWithIndex { tr, idx ->
                EnumerationValue enumerationValue = new EnumerationValue()
                enumerationValue.key = tr.td[0].text()
                enumerationValue.value = tr.td[1]
                DDHelperFunctions.addMetadata(enumerationValue, "Web Order", "${idx + 1}")
                DDHelperFunctions.addMetadata(enumerationValue, "Permitted National Code", "true")
                foundEnumerationValues.add(enumerationValue)
            }
        }
        if(defaultCodes.size() > 0) {
            defaultCodes.each { DefaultCode ->
                EnumerationValue enumerationValue = new EnumerationValue()
                EnumerationValue existingEnumerationValue = foundEnumerationValues.find{it.key == DefaultCode.code.text()}
                if(existingEnumerationValue) {
                    enumerationValue = existingEnumerationValue
                } else {
                    foundEnumerationValues.add(enumerationValue)
                    enumerationValue.key = DefaultCode.code.text()
                }

                String webOrder = DefaultCode.webOrder.text()
                enumerationValue.value = "Default Code " + webOrder
                DDHelperFunctions.addMetadata(enumerationValue, "Web Order", webOrder)
                DDHelperFunctions.addMetadata(enumerationValue, "Default Code", "true")
                String unquotedString = DDHelperFunctions.unquoteString(DefaultCode.description.text())
                DDHelperFunctions.addMetadata(enumerationValue, "Web Presentation", unquotedString)



            }
        }

        if(foundEnumerationValues.size() > 0) {
            String enumerationTypeLabel = "Enumeration - ${DDHelperFunctions.tidyLabel(name)} (Element)"
            if(dataDictionary.dataTypes[enumerationTypeLabel]) {
                log.error("Enumeration Type already exists!!")
                log.error(enumerationTypeLabel)
            } else {
                dataType = new EnumerationType(label: enumerationTypeLabel, enumerationValues: [])
                foundEnumerationValues.each {enumerationValue ->
                    ((EnumerationType) dataType).addToEnumerationValues(enumerationValue)
                }
                dataDictionary.dataTypes[enumerationTypeLabel] = dataType
            }
        }


        else if (stringValues["format_length"]) {
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

        return catalogueItem

    }

    void calculateFormatLinkMatchedItem(DataDictionary dataDictionary) {
        if(stringValues["format-link"]) {
            formatLinkMatchedItem = dataDictionary.elements.values().find {
                it.ddUrl == stringValues["format-link"]

            }
            if(!formatLinkMatchedItem) {
                formatLinkMatchedItem = dataDictionary.attributes.values().find {
                    it.ddUrl == stringValues["format-link"]
                }
            }
            if(!formatLinkMatchedItem) {
                log.error("Cannot match format-link: ${stringValues["format-link"]}")
            } else {
                formatLinkMatchedItem.whereMentioned.add(this)
            }
        }

    }

    @Override
    List<Topic> toDitaTopics(DataDictionary dataDictionary) {

        List<Topic> returnTopics = []

        if(!isRetired && stringValues["format-length"]) {
            Topic formatTopic = createSubTopic("Format / Length")
            String formatContent = "<p>${stringValues["format-length"]}</p>"
            if(formatLinkMatchedItem) {
                String formatXref = formatLinkMatchedItem.getXRef().outputAsString(formatLinkMatchedItem.name)
                formatContent = "<p>See " + formatXref + "</p>"
            }
            formatTopic.body.bodyElements.add(new Html(content: formatContent ))
            returnTopics.add(formatTopic)
        }

        Topic descriptionTopic = createDescriptionTopic()

        returnTopics.add(descriptionTopic)
        if(!isPrepatory) {

            if (catalogueItem.dataType instanceof EnumerationType) {
                List<EnumerationValue> enumValues = ((EnumerationType) catalogueItem.dataType).enumerationValues.sort {
                    enumValue ->
                        String webOrderString = DDHelperFunctions.getMetadataValue(enumValue, "Web Order")
                        if (webOrderString) {
                            return Integer.parseInt(webOrderString)
                        } else return 0
                }
                /*List<EnumerationValue> extraFieldHeads = enumValues.findAll {
                    DDHelperFunctions.getMetadataValue(it, "ExtraFieldHead") == "true"
                }
                if (extraFieldHeads.size() > 0) {
                    createEnumValuesTopic(extraFieldHeads, "National Codes", returnTopics)
                }
                */
                List<EnumerationValue> permittedNationalCodes = enumValues.findAll {
                    DDHelperFunctions.getMetadataValue(it, "Permitted National Code") == "true"
                }
                if (permittedNationalCodes.size() > 0) {
                    String nationalCodesTitle = "Permitted National Codes"
                    if(getElementAttributes(dataDictionary).size() == 1 &&
                            getElementAttributes(dataDictionary).get(0).catalogueItem.dataType.getClass() == EnumerationType.class) {
                        List<EnumerationValue> attributeValues =
                                ((EnumerationType)getElementAttributes(dataDictionary).get(0).catalogueItem.dataType).getEnumerationValues()
                        if(attributeValues.size() == permittedNationalCodes.size()) {
                            nationalCodesTitle = "National Codes"
                        }
                    }

                    createEnumValuesTopic(permittedNationalCodes, nationalCodesTitle, returnTopics)

                    //createEnumValuesTopic(permittedNationalCodes, "Permitted National Codes", returnTopics)
                }
                List<EnumerationValue> nationalCodes = enumValues.findAll {
                    !DDHelperFunctions.getMetadataValue(it, "Permitted National Code") &&
                            !DDHelperFunctions.getMetadataValue(it, "Default Code") /*&&
                            !DDHelperFunctions.getMetadataValue(it, "ExtraFieldHead")*/&&
                            DDHelperFunctions.getMetadataValue(it, "Web Order") != "0"
                }
                if (nationalCodes.size() > 0) {
                    String nationalCodesTitle = "Permitted National Codes"
                    if(getElementAttributes(dataDictionary).size() == 1 &&
                            getElementAttributes(dataDictionary).get(0).catalogueItem.dataType.getClass() == EnumerationType.class) {
                        Set<EnumerationValue> attributeValues =
                                ((EnumerationType)getElementAttributes(dataDictionary).get(0).catalogueItem.dataType).getEnumerationValues()
                        if(attributeValues.size() == nationalCodes.size()) {
                            nationalCodesTitle = "National Codes"
                        }
                    }

                    createEnumValuesTopic(nationalCodes, nationalCodesTitle, returnTopics)
                }
                List<EnumerationValue> defaultCodes = enumValues.findAll {
                    //DDHelperFunctions.getMetadataValue(it, "Default Code") == "true"
                    DDHelperFunctions.getMetadataValue(it, "Web Order") == "0"
                }
                if (defaultCodes.size() > 0) {
                    createEnumValuesTopic(defaultCodes, "Default Codes", returnTopics)
                }
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

        if(stringValues["element_attributes"]) {
            Topic attributesTopic = createSubTopic("Attribute")
            Ul ul = new Ul()
            List<DDAttribute> attributes = getElementAttributes(dataDictionary)
            attributes.each {attribute ->
                ul.entries.add(new Li(xref: attribute.getXRef()))
            }
            if(attributes) {
                attributesTopic.body.bodyElements.add(ul)
                returnTopics.add(attributesTopic)
            }
        }

        return returnTopics
    }

    void createEnumValuesTopic(List<EnumerationValue> enumValues, String title, List<Topic> returnTopics) {
        SimpleTable enumTable = new SimpleTable(relColWidth: "1* 4*")
        enumTable.stHead = new STHead(stEntries: [new STEntry(value: "Code"), new STEntry(value: "Description")])
        enumValues.each { enumValue ->
            STEntry keyEntry = new STEntry(bodyElement: new Html(content: enumValue.key))
            if(DDHelperFunctions.getMetadataValue(enumValue, "label")) {
                keyEntry = new STEntry(bodyElement: new Html(content: DDHelperFunctions.getMetadataValue(enumValue, "label")))

            }
            STEntry valueEntry = new STEntry(bodyElement: new Html(content: enumValue.value))
            String webPresentation = DDHelperFunctions.getMetadataValue(enumValue, "Web Presentation")
            if (webPresentation && webPresentation != "") {
                valueEntry = new STEntry(bodyElement: new Html(content: webPresentation))
            }
            String description = DDHelperFunctions.getMetadataValue(enumValue, "description")
            if (description) {
                valueEntry = new STEntry(bodyElement: new Html(content: description))
            }
            STRow row = new STRow(stEntries: [keyEntry, valueEntry])
            if (DDHelperFunctions.getMetadataValue(enumValue, "Status") == "retired") {
                row.outputClass = "retired"
            }
            enumTable.stRows.add(row)
        }
        Topic codesTopic = createSubTopic(title)
        codesTopic.body.bodyElements.add(enumTable)
        returnTopics.add(codesTopic)
    }

    List<DDAttribute> getElementAttributes(DataDictionary dataDictionary) {
        List<DDAttribute> attributes = []
        StringUtils.split(stringValues["element_attributes"], ",").each { uin ->
            DDAttribute attribute = dataDictionary.attributes[uin]
            if(attribute) {
                attributes.add(attribute)
            } else {
                log.error("Cannot find uin: " + uin)
            }
        }
        return attributes

    }


    @Override
    void fromCatalogueExport(DataDictionary dataDictionary, def catalogueXml, UUID parentId, UUID containerId) {

        //clazz = xml."class"

        definition = catalogueXml.description
        name = catalogueXml.label

        catalogueId = UUID.fromString(catalogueXml.id)
        parentCatalogueId = parentId
        containerCatalogueId = containerId

        super.fromCatalogueExport(dataDictionary, catalogueXml, parentId, containerId)

        catalogueItem = new DataElement(catalogueXml)

    }

    /*
    @Override
    void updateDescriptionInCatalogue(BindingMauroDataMapperClient mdmClient) {
        mdmClient.updateDataElementDescription(containerCatalogueId, parentCatalogueId, catalogueId, definition)
    }
*/
    @Override
    String getDitaKey() {
        return "element_" + DDHelperFunctions.makeValidDitaName(name)
    }


    @Override
    String getOutputClassForLink() {
        String outputClass = "element"
        if(stringValues["isRetired"] == "true") {
            outputClass += " retired"
        }
        return outputClass
    }

    @Override
    String getOutputClassForTable() {
        return "Data Element"
    }

    @Override
    String getTypeText() {
        return "data element"
    }

    @Override
    String getTypeTitle() {
        return "Data Element"
    }

    @Override
    String getShortDesc(DataDictionary dataDictionary) {
        if(isPrepatory) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + DDHelperFunctions.parseHtml(definition.toString()) + "</xml>")
                String firstParagraph = xml.p[0].text()
                if (xml.p.size() == 0) {
                    firstParagraph = xml.text()
                }
                String firstSentence = firstParagraph.substring(0, firstParagraph.indexOf(".") + 1)
                if (firstSentence.toLowerCase().contains("is the same as")) {
                    List<DDAttribute> attributes = getElementAttributes(dataDictionary)
                    if (attributes.size() == 1) {
                        firstSentence = attributes[0].getShortDesc(dataDictionary)
                    } else {
                        firstSentence = name
                    }
                }
                return firstSentence
            } catch (Exception e) {
                log.error("Couldn't parse: " + definition)
                return name
            }
        }
    }

    @Override
    String getInternalLink() {
        if(isRetired) {
            return "](dm:${DataDictionary.CORE_MODEL_NAME}|dc:${DataDictionary.DATA_FIELD_NOTES_CLASS_NAME}|dc:Retired|de:${DDHelperFunctions.tidyLabel(name)})"
        } else {
            return "](dm:${DataDictionary.CORE_MODEL_NAME}|dc:${DataDictionary.DATA_FIELD_NOTES_CLASS_NAME}|de:${DDHelperFunctions.tidyLabel(name)})"
        }
    }


}
