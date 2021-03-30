package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType

import grails.gorm.transactions.Transactional
import groovy.util.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.ClassLink
import uk.nhs.digital.maurodatamapper.datadictionary.DDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.DDClass
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Paragraph
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STEntry
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STHead
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STRow
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.SimpleTable
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Topic

@Transactional
class ClassService extends DataDictionaryComponentService <DataClass> {

    @Override
    Map indexMap(DataClass catalogueItem) {
        [
            catalogueId: catalogueItem.id.toString(),
            name: catalogueItem.label,
            stereotype: "class"

        ]
    }

    @Override
    def show(String branch, String id) {
        DataClass dataClass = dataClassService.get(id)

        String description = convertLinksInDescription(branch, dataClass.description)
        DataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(branch)
        String shortDesc = replaceLinksInShortDescription(getShortDescription(dataClass, dataDictionary))
        def result = [
                catalogueId     : dataClass.id.toString(),
                name            : dataClass.label,
                stereotype      : "class",
                shortDescription: shortDesc,
                description     : description,
                alsoKnownAs     : getAliases(dataClass)
        ]

        List<Map> attributes = []
        Collection<DataElement> basicElements = dataClass.dataElements.
                findAll { !(it.dataType instanceof ReferenceType) }

        basicElements.sort { a, b ->
            DDHelperFunctions.getMetadataValue(a, "isUnique") <=> DDHelperFunctions.getMetadataValue(b, "isUnique") ?:
                    b.label <=> a.label
        }.reverse().each { de ->

            String uin = DDHelperFunctions.getMetadataValue(de, "uin")
            DDAttribute ddAttribute = dataDictionary.attributes[uin]
            if (ddAttribute) {

                Map attributesMap = [:]
                String key = ""
                if (DDHelperFunctions.getMetadataValue(de, "isUnique")) {
                    key = "Key"
                }
                attributesMap["key"] = key
                attributesMap["catalogueId"] = ddAttribute.catalogueItem.id.toString()
                attributesMap["name"] = ddAttribute.name
                attributesMap["stereotype"] = "attribute"
                attributes.add(attributesMap)
            } else {
                log.error("Cannot lookup attribute: " + uin)
            }
        }
        result["attributes"] = attributes

        List<Map> relationships = []

        List<Map> keyRows = []
        List<Map> otherRows = []
        dataClass.dataElements.
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
                    if (bracketLocation >= 0) {
                        relationshipLabel = relationshipLabel.substring(0, bracketLocation)
                    }
                    keyRows.add([
                            key         : "Key",
                            relationship: ClassLink.getCardinalityText(DDHelperFunctions.getMetadataValue(dataElement, "clientCardinality"),
                                    relationshipLabel),
                            catalogueId : targetClass.catalogueItem.id.toString(),
                            name        : targetClass.name,
                            stereotype  : "class"
                    ])
                }
        dataClass.dataElements.
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
                    if (bracketLocation >= 0) {
                        relationshipLabel = relationshipLabel.substring(0, bracketLocation)
                    }
                    otherRows.add([
                            relationSupplierExclusivity: DDHelperFunctions.getMetadataValue(dataElement, "relationSupplierExclusivity"),
                            relationClientExclusivity  : DDHelperFunctions.getMetadataValue(dataElement, "relationClientExclusivity"),
                            relationship               : ClassLink.getCardinalityText(DDHelperFunctions.getMetadataValue(dataElement, "clientCardinality"),
                                    relationshipLabel),
                            catalogueId                : targetClass.catalogueItem.id.toString(),
                            name                       : targetClass.name,
                            stereotype                 : "class"

                    ])
                }
                keyRows.sort { it.name }.each { row ->
                    relationships.add(row)
                }
                otherRows.sort { a, b ->
                    a.relationSupplierExclusivity <=> b.relationSupplierExclusivity ?:
                            a.relationClientExclusivity <=> b.relationClientExclusivity ?:
                                    b.name <=> a.name
                }.reverse().each { row ->
                    if (!relationships.contains(row)) {
                        relationships.add(row)
                        String relationSupplierExclusivity = row.relationSupplierExclusivity
                        if (relationSupplierExclusivity && relationSupplierExclusivity != "") {
                            otherRows.findAll { it.relationSupplierExclusivity == relationSupplierExclusivity && it != row }.
                                    sort { it.name }.
                                    each { linkedRow ->
                                        relationships.add(linkedRow)
                                        //linkedRow.stEntries.remove(0)
                                        //linkedRow.stEntries.value = ""
                                        linkedRow.key = "or " + linkedRow.relationClientExclusivity
                                        //linkedRow.stEntries[0].value = ""

                                    }

                        }
                        String relationClientExclusivity = row.relationClientExclusivity
                        if (relationClientExclusivity && relationClientExclusivity != "") {
                            otherRows.findAll { it.relationClientExclusivity == relationClientExclusivity && it != row }.
                                    sort { it.name }.
                                    each { linkedRow ->
                                        relationships.add(linkedRow)
                                        //linkedRow.stEntries.remove(0)
                                        //linkedRow.stEntries.value = ""
                                        linkedRow.key = "or " + linkedRow.relationClientExclusivity
                                        //linkedRow.stEntries[0].value = ""

                                    }

                        }
                    }
                }
        relationships.each { row ->
            if (row.relationSupplierExclusivity != "Key") {
                row.relationSupplierExclusivity = ""
            }
        }
        result["relationships"] = relationships
        return result
    }
    @Override
    Set<DataClass> getAll() {
        nhsDataDictionaryService.getDataClasses(false)
    }

    @Override
    DataClass getItem(UUID id) {
        dataClassService.get(id)
    }

    @Override
    String getMetadataNamespace() {
        DDHelperFunctions.metadataNamespace + ".class"
    }

    @Override
    String getShortDescription(DataClass dataClass, DataDictionary dataDictionary) {
        if(isPreparatory(dataClass)) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + DDHelperFunctions.parseHtml(dataClass.description.toString()) + "</xml>")
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
                log.error("Couldn't parse: " + dataClass.description)
                return dataClass.label
            }
        }
    }
}
