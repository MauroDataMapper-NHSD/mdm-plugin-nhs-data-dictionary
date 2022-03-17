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
package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType

import groovy.util.logging.Slf4j
import groovy.xml.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Paragraph
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STEntry
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STHead
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STRow
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.SimpleTable
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Topic

@Slf4j
class DDClass extends DataDictionaryComponent<DataClass> {


    //def clazz
    def property

    //List<GPathResult> properties = []

    Map<DDAttribute, Map<String, String>> attributes = [:]

    List<ClassLink> classLinks = []



    @Override
    void fromXml(Object xml) {

        //clazz = xml."class"
        definition = xml.definition
        property = xml.property

        //properties = clazz.property


        super.fromXml(xml)

        classLinks = xml.link.collect{it -> new ClassLink(it)}
    }

    void createAttributes(Collection<DDAttribute> allAttributes) {

        property.each { property ->
            DDAttribute referencedAttribute = allAttributes.find{ it.uin == property.referencedElement.text() }
            if(referencedAttribute) {
                DDAttribute existingAttribute = attributes.keySet().find {it.name == referencedAttribute.name}
                if(!existingAttribute) {
                    Map<String, String> properties = [:]
                    properties["isUnique"] = property.isUnique.text()
                    properties["isOrdered"] = property.isOrdered.text()

                    attributes[referencedAttribute] = properties
                }
                else {
                    log.error("Already existing data element '${existingAttribute.name}' in class '${name}'")
                }
            } else {
                log.error("Cannot de-reference attribute from class: " + property.referencedElement.text())
            }

        }

    }

    @Override
    DataClass toCatalogueItem(DataDictionary dataDictionary) {
        catalogueItem = new DataClass(label: DDHelperFunctions.tidyLabel(name))
        catalogueItem.description = DDHelperFunctions.parseHtml(definition)

        attributes.keySet().each {
            DataElement dataElement = it.toCatalogueItem(dataDictionary)
            ((DataClass)catalogueItem).addToDataElements(dataElement)
            attributes[it].each {key, value ->
                DDHelperFunctions.addMetadata(dataElement, key, value)
            }
        }

        addAllMetadata(catalogueItem)
        return catalogueItem
    }

    @Override
    List<Topic> toDitaTopics(DataDictionary dataDictionary) {

        //log.debug("definition: ")
        //log.debug(definition)
        //log.debug(DDHelperFunctions.parseHtml(definition).toString())

        Topic descriptionTopic = createDescriptionTopic()
        //log.debug(((Html)descriptionTopic.body.bodyElements[0]).content)

        Topic attributesTopic = createSubTopic("Attributes")
        Collection<DataElement> basicElements = catalogueItem.dataElements.
            findAll {!(it.dataType instanceof ReferenceType)}
        //log.debug(catalogueItem.label)
        //log.debug(basicElements.size())
        catalogueItem.dataElements.each {
            //log.debug(it.dataType.getClass())
        }
        if (basicElements && basicElements.size() > 0) {
            SimpleTable attributeTable = new SimpleTable(relColWidth: "1* 9*")
            attributeTable.stHead = new STHead(stEntries: [new STEntry(value: "Key"), new STEntry(value: "Attribute Name") /*, new STEntry(value: "Attribute Type")*/])

            basicElements.sort {a, b ->
                DDHelperFunctions.getMetadataValue(a, "isUnique") <=> DDHelperFunctions.getMetadataValue(b, "isUnique") ?:
                b.label <=> a.label
            }.reverse().each { de ->
                String uin = DDHelperFunctions.getMetadataValue(de, "uin")
                if (uin) {
                    DDAttribute ddAttribute = dataDictionary.attributes[uin]
                    if (ddAttribute) {
                        String key = ""
                        if(DDHelperFunctions.getMetadataValue(de, "isUnique")) {
                            key = "Key"
                        }
                        attributeTable.stRows.add(new STRow(stEntries: [
                            new STEntry(value: key),
                            new STEntry(xref: ddAttribute.getXRef())
                            /*, new STEntry(value: de.dataType.label)*/]))

                    } else {
                        log.error("Cannot lookup attribute: " + uin)
                    }
                } else {
                    log.error("Cannot find uin: " + de.label)
                }
            }

            attributesTopic.body.bodyElements.add(attributeTable)

        } else {
            attributesTopic.body.bodyElements.add(new Paragraph(content: "This class has no attributes"))
        }
        List<Topic> returnTopics = [descriptionTopic, attributesTopic]

        if(classLinks && classLinks.size() > 0) {
            Topic relationshipsTopic = createSubTopic("Relationships")
            SimpleTable attributeTable = new SimpleTable(relColWidth: "1* 5* 5*")
            attributeTable.stHead = new STHead(stEntries: [new STEntry(value: "Key"), new STEntry(value: "Relationship"), new STEntry(value: "Class")])

            List<STRow> keyRows = []
            List<STRow> otherRows = []
            catalogueItem.dataElements.
                    findAll { it.dataType instanceof ReferenceType }.
                    findAll {
                        (DDHelperFunctions.getMetadataValue(it, "direction") == "supplier" &&
                                DDHelperFunctions.getMetadataValue(it, "partOfClientKey") == "true") ||
                                (DDHelperFunctions.getMetadataValue(it, "direction") == "client" &&
                                        DDHelperFunctions.getMetadataValue(it, "partOfSupplierKey") == "true")
                    }.
                    each { dataElement ->
                        DDClass targetClass = dataDictionary.classes.values().find {
                            it.uin == DDHelperFunctions.getMetadataValue(dataElement, "clientUin")
                        }
                        String relationshipLabel = dataElement.label
                        int bracketLocation = relationshipLabel.indexOf("(")
                        if( bracketLocation >=0 ) {
                            relationshipLabel = relationshipLabel.substring(0, bracketLocation)
                        }
                        keyRows.add(new STRow(stEntries: [
                            new STEntry(value: "Key"),
                            new STEntry(value: ClassLink.getCardinalityText(DDHelperFunctions.getMetadataValue(dataElement, "clientCardinality"), relationshipLabel)),
                            new STEntry(xref: targetClass.getXRef())]))

                    }
            catalogueItem.dataElements.
                    findAll { it.dataType instanceof ReferenceType }.
                    findAll {
                        !(DDHelperFunctions.getMetadataValue(it, "direction") == "supplier" &&
                                DDHelperFunctions.getMetadataValue(it, "partOfClientKey") == "true") &&
                                !(DDHelperFunctions.getMetadataValue(it, "direction") == "client" &&
                                        DDHelperFunctions.getMetadataValue(it, "partOfSupplierKey") == "true")

                    }.
                    each { dataElement ->
                        DDClass targetClass = dataDictionary.classes.values().find {
                            (DDHelperFunctions.getMetadataValue(dataElement, "direction") == "supplier" &&
                                    it.uin == DDHelperFunctions.getMetadataValue(dataElement, "clientUin")) ||
                                    (DDHelperFunctions.getMetadataValue(dataElement, "direction") == "client" &&
                                            it.uin == DDHelperFunctions.getMetadataValue(dataElement, "supplierUin"))
                        }
                        String relationshipLabel = dataElement.label
                        int bracketLocation = relationshipLabel.indexOf("(")
                        if( bracketLocation >=0 ) {
                            relationshipLabel = relationshipLabel.substring(0, bracketLocation)
                        }
                        otherRows.add(new STRow(stEntries: [
                            new STEntry(value: DDHelperFunctions.getMetadataValue(dataElement, "relationSupplierExclusivity")),
                            new STEntry(value: DDHelperFunctions.getMetadataValue(dataElement, "relationClientExclusivity")),

                            new STEntry(value: ClassLink.getCardinalityText(DDHelperFunctions.getMetadataValue(dataElement, "clientCardinality"), relationshipLabel)),
                            new STEntry(xref: targetClass.getXRef())]))

                    }
            keyRows.sort{it.stEntries[2].xref.keyref}.each {row ->
                attributeTable.stRows.add(row)
            }
            otherRows.sort{a, b ->
                a.stEntries[0].value <=> b.stEntries[0].value ?:
                        a.stEntries[1].value <=> b.stEntries[1].value ?:
                                b.stEntries[3].xref.keyref <=> a.stEntries[3].xref.keyref
            }.reverse().each {row ->
                if(!attributeTable.stRows.contains(row)) {
                    attributeTable.stRows.add(row)
                    String relationSupplierExclusivity = row.stEntries[0].value
                    if(relationSupplierExclusivity && relationSupplierExclusivity != "") {
                        otherRows.findAll{it.stEntries[0].value == relationSupplierExclusivity && it != row}.
                                sort{it.stEntries[3].xref.keyref}.
                                each{linkedRow ->
                                    attributeTable.stRows.add(linkedRow)
                                    linkedRow.stEntries.remove(0)
                                    //linkedRow.stEntries.value = ""
                                    linkedRow.stEntries[1].value = "or " + linkedRow.stEntries[1].value
                                    linkedRow.stEntries[0].value = ""

                                }

                    }
                    String relationClientExclusivity = row.stEntries[1].value
                    if(relationClientExclusivity && relationClientExclusivity != "") {
                        otherRows.findAll{it.stEntries[1].value == relationClientExclusivity && it != row}.
                                sort{it.stEntries[3].xref.keyref}.
                                each{linkedRow ->
                                    attributeTable.stRows.add(linkedRow)
                                    linkedRow.stEntries.remove(0)
                                    //linkedRow.stEntries.value = ""
                                    linkedRow.stEntries[1].value = "or " + linkedRow.stEntries[1].value
                                    linkedRow.stEntries[0].value = ""

                                }

                    }
                    row.stEntries.remove(0)

                }
            }
            attributeTable.stRows.each {row ->
                if(row.stEntries[0].value != "Key") {
                    row.stEntries[0].value = ""
                }
            }
            relationshipsTopic.body.bodyElements.add(new Paragraph(content: "Each ${name}"))
            relationshipsTopic.body.bodyElements.add(attributeTable)
            returnTopics.add(relationshipsTopic)

        }
        Topic whereUsedTopic = generateWhereUsedTopic()
        if(whereUsedTopic) {
            returnTopics.add(whereUsedTopic)
        }
        Topic aliasTopic = getAliasTopic()
        if(aliasTopic) {
            returnTopics.add(aliasTopic)
        }
        return returnTopics

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

        this.catalogueItem = new DataClass(catalogueXml)

        catalogueItem.dataElements.findAll {
            it.dataType.domainType.equalsIgnoreCase("PrimitiveType") ||
            it.dataType.domainType.equalsIgnoreCase("EnumerationType")}
            .each { dataElement ->
            //DataElement dataElement = new DataElement(dataElementXml)
            //this.dataClass.addToDataElements(dataElement)
            String classElementUin = DDHelperFunctions.getMetadataValue(dataElement,"uin")

            DDAttribute matchedAttribute = dataDictionary.attributes[classElementUin]
            if (matchedAttribute) {
                Map<String, String> properties = [:]
                this.attributes[matchedAttribute] = properties
            } else {
                log.error("Couldn't match up attribute: ${classElementUin}")
            }

        }
        catalogueXml.dataElements.findAll { it.dataType.domainType.equalsIgnoreCase("ReferenceType") }.each { dataElementXml ->
            DataElement dataElement = new DataElement(dataElementXml)

            classLinks.add(new ClassLink(dataElement))
        }
    }

    @Override
    void fromCatalogueItem(DataDictionary dataDictionary, DataClass dataClass, UUID parentId, UUID containerId, MetadataService metadataService) {

        //clazz = xml."class"

        definition = dataClass.description
        name = dataClass.label

        catalogueId = dataClass.id
        this.parentCatalogueId = parentId
        this.containerCatalogueId = containerId

        super.fromCatalogueItem(dataDictionary, dataClass, parentId, containerId, metadataService)

        catalogueItem = dataClass
        dataClass.dataElements.findAll {
            it.dataType.domainType.equalsIgnoreCase("PrimitiveType") ||
                    it.dataType.domainType.equalsIgnoreCase("EnumerationType")}
                .each { dataElement ->
                    //DataElement dataElement = new DataElement(dataElementXml)
                    //this.dataClass.addToDataElements(dataElement)
                    String classElementUin = metadataService.findAllByCatalogueItemId(dataElement.id).find {
                        it.namespace.contains(DDHelperFunctions.metadataNamespace) &&
                                it.key == "uin"
                    }.value

                    DDAttribute matchedAttribute = dataDictionary.attributes[classElementUin]
                    if (matchedAttribute) {
                        Map<String, String> properties = [:]
                        this.attributes[matchedAttribute] = properties
                    } else {
                        log.error("Couldn't match up attribute: ${classElementUin}")
                    }

                }
        dataClass.dataElements.findAll { it.dataType.domainType.equalsIgnoreCase("ReferenceType") }.each { dataElement ->
            classLinks.add(new ClassLink(dataElement))
        }

    }

/*    @Override
    void updateDescriptionInCatalogue(BindingMauroDataMapperClient mdmClient) {
        mdmClient.updateDataClassDescription(containerCatalogueId, parentCatalogueId, catalogueId, definition)
    }
*/
    @Override
    String getDitaKey() {
        return "class_" + DDHelperFunctions.makeValidDitaName(name)
    }

    @Override
    String getOutputClassForLink() {
        String outputClass = "class"
        if(stringValues["isRetired"] == "true") {
            outputClass += " retired"
        }
        return outputClass

    }

    @Override
    String getOutputClassForTable() {
        return "Class"
    }
    @Override
    String getTypeText() {
        return "class"
    }
    @Override
    String getTypeTitle() {
        return "Class"
    }


    void linkReferences(DataDictionary dataDictionary, DataModel dataModel) {
        classLinks.findAll{it.metaclass == "KernelAssociation20"}.each { link ->
            if(link.supplierClass) {
                DataClass thisDataClass = ((DataClass) this.catalogueItem)
                // Get the target class
                DataClass targetDataClass = ((DataClass) link.supplierClass.catalogueItem)

                // Get a reference type (create if it doesn't exist)
                ReferenceType targetReferenceType = (ReferenceType) dataModel.dataTypes.find { dataType ->
                    dataType instanceof ReferenceType &&
                            ((ReferenceType) dataType).label.equalsIgnoreCase(targetDataClass.label + " Reference")
                }
                if (!targetReferenceType) {
                    targetReferenceType = new ReferenceType(label: "${targetDataClass.label} Reference")
                    targetReferenceType.referenceClass = targetDataClass
                    //dataModel.addToDataTypes(targetReferenceType)
                }

                ReferenceType sourceReferenceType = (ReferenceType) dataModel.dataTypes.find { dataType ->
                    dataType instanceof ReferenceType &&
                            ((ReferenceType) dataType).label.equalsIgnoreCase(this.catalogueItem.label + " Reference")
                }
                if (!sourceReferenceType) {
                    sourceReferenceType = new ReferenceType(label: "${this.catalogueItem.label} Reference")
                    sourceReferenceType.setReferenceClass((DataClass) this.catalogueItem)
                    //dataModel.addToDataTypes(sourceReferenceType)
                }

                // create a data element in the class
                String sourceLabel = link.clientRole
                int count = 0
                if (thisDataClass.dataElements) {
                    count = thisDataClass.dataElements.findAll { it.label.startsWith(sourceLabel.trim()) }.size()
                }
                if (count > 0) {
                    sourceLabel += " (${count})"
                }
                DataElement sourceDataElement = new DataElement(label: sourceLabel, dataType: targetReferenceType)
                link.addMetadata(sourceDataElement)
                DDHelperFunctions.addMetadata(sourceDataElement, "direction", "client")
                thisDataClass.addToDataElements(sourceDataElement)

                String targetLabel = link.supplierRole
                count = 0
                if (targetDataClass.dataElements) {
                    count = targetDataClass.dataElements.findAll { it.label.startsWith(targetLabel.trim()) }.size()
                }
                if (count > 0) {
                    targetLabel += " (${count})"
                }
                DataElement targetDataElement = new DataElement(label: targetLabel, dataType: sourceReferenceType)
                link.addMetadata(targetDataElement)
                DDHelperFunctions.addMetadata(targetDataElement, "direction", "supplier")
                targetDataClass.addToDataElements(targetDataElement)
            }
        }


    }

    void linkRelationships(DataDictionary dataDictionary) {
        classLinks.each {it.findTargetClass(dataDictionary)}
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
                if (firstSentence.toLowerCase().contains("a subtype of")) {
                    String secondParagraph = xml.p[1].text()
                    String secondSentence = secondParagraph.substring(0, secondParagraph.indexOf(".") + 1)
                    return secondSentence
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
            return "](dm:${DataDictionary.CORE_MODEL_NAME}|dc:${DataDictionary.DATA_CLASSES_CLASS_NAME}|dc:Retired|dc:${DDHelperFunctions.tidyLabel(name)})"
        } else {
            return "](dm:${DataDictionary.CORE_MODEL_NAME}|dc:${DataDictionary.DATA_CLASSES_CLASS_NAME}|dc:${DDHelperFunctions.tidyLabel(name)})"
        }
    }


}
