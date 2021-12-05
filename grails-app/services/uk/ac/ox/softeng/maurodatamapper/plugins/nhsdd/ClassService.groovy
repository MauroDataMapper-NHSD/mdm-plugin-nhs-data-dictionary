package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceTypeService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import grails.gorm.transactions.Transactional
import groovy.util.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.ClassLink
import uk.nhs.digital.maurodatamapper.datadictionary.DDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.DDClass
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Paragraph
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STEntry
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STHead
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STRow
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.SimpleTable
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Topic

@Transactional
class ClassService extends DataDictionaryComponentService <DataClass> {

    ReferenceTypeService referenceTypeService


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

    void ingestFromXml(def xml, Folder dictionaryFolder, DataModel coreDataModel, String currentUserEmailAddress,
                       NhsDataDictionary nhsDataDictionary) {

        DataClass parentDataClass = new DataClass(
            label: NhsDataDictionary.DATA_CLASSES_CLASS_NAME,
            createdBy: currentUserEmailAddress,
            dataModel: coreDataModel)
        coreDataModel.addToDataClasses(parentDataClass)

        DataClass retiredDataClass = new DataClass(
            label: "Retired",
            createdBy: currentUserEmailAddress,
            dataModel: coreDataModel)
        coreDataModel.addToDataClasses(retiredDataClass)
        parentDataClass.addToDataClasses(retiredDataClass)

        Map<String, DataClass> allClasses = [:]
        xml.DDClass.each {ddClass ->

            String label = ddClass.TitleCaseName.text()
            String uin = ddClass.uin.text()
            if(!allClasses[label] || ddClass.isRetired.text() != "true") {
                DataClass dataClass = new DataClass(
                    label: label,
                    description: DDHelperFunctions.parseHtml(ddClass.definition),
                    createdBy: currentUserEmailAddress,
                    parentDataClass: parentDataClass
                )
                if(ddClass.isRetired.text() == "true") {
                    retiredDataClass.addToDataClasses(dataClass)
                } else {
                    parentDataClass.addToDataClasses(dataClass)
                }
                coreDataModel.addToDataClasses(dataClass)

                // Now link the attributes from the properties

                ddClass.property.each { property ->
                    String attributeUin = property.referencedElement.text()
                    DataElement attributeElement = nhsDataDictionary.attributeElementsByUin[attributeUin]
                    if(attributeElement) {
                        attributeElement.addToImportingDataClasses(dataClass)
                    } else {
                        System.err.println("Cannot find attributeElement with Uin: " + attributeUin)
                    }
                }


                addMetadataFromXml(dataClass, ddClass, currentUserEmailAddress)
                allClasses[label] = dataClass
                nhsDataDictionary.classesByUin[uin] = dataClass
                List<ClassLink> classLinks = ddClass.link.collect{it -> new ClassLink(it)}
                nhsDataDictionary.classLinks.addAll(classLinks)
            }
        }

    }

    void processLinks(NhsDataDictionary nhsDataDictionary) {

        nhsDataDictionary.classLinks.each {classLink ->
            classLink.findTargetClass(nhsDataDictionary)
        }

    }

    void linkReferences(NhsDataDictionary nhsDataDictionary, String currentUserEmailAddress) {

        nhsDataDictionary.classLinks.each {link ->
            if(link.metaclass == "KernelAssociation20") {

                if (link.supplierClass) {
                    DataClass thisDataClass = link.clientClass
                    // Get the target class
                    DataClass targetDataClass = link.supplierClass

                    // Get a reference type (create if it doesn't exist)
                    ReferenceType targetReferenceType = nhsDataDictionary.classReferenceTypesByName[targetDataClass.label]
                    if (!targetReferenceType) {

                        targetReferenceType = new ReferenceType(
                            label: "${targetDataClass.label} Reference",
                            createdBy: currentUserEmailAddress,
                            referenceClass: targetDataClass)
                        targetDataClass.addToReferenceTypes(targetReferenceType)
                        nhsDataDictionary.coreDataModel.addToDataTypes(targetReferenceType)
                        nhsDataDictionary.classReferenceTypesByName[targetDataClass.label] = targetReferenceType
                    }

                    ReferenceType sourceReferenceType = nhsDataDictionary.classReferenceTypesByName[thisDataClass.label]
                    if (!sourceReferenceType) {
                        sourceReferenceType = new ReferenceType(
                            label: "${thisDataClass.label} Reference",
                            createdBy: currentUserEmailAddress,
                            referenceClass: thisDataClass)
                        thisDataClass.addToReferenceTypes(sourceReferenceType)
                        nhsDataDictionary.coreDataModel.addToDataTypes(sourceReferenceType)
                        nhsDataDictionary.classReferenceTypesByName[thisDataClass.label] = sourceReferenceType
                    }

                    // create a data element in the class
                    String sourceLabel = link.clientRole
                    int count = 0
                    if (thisDataClass.dataElements) {
                        count = thisDataClass.dataElements.findAll {it.label.startsWith(sourceLabel.trim())}.size()
                    }
                    if (count > 0) {
                        sourceLabel += " (${count})"
                    }
                    DataElement sourceDataElement = new DataElement(label: sourceLabel, dataType: targetReferenceType,
                                                                    createdBy: currentUserEmailAddress)
                    link.addMetadata(sourceDataElement)
                    DDHelperFunctions.addMetadata(sourceDataElement, "direction", "client")
                    thisDataClass.addToDataElements(sourceDataElement)

                    String targetLabel = link.supplierRole
                    count = 0
                    if (targetDataClass.dataElements) {
                        count = targetDataClass.dataElements.findAll {it.label.startsWith(targetLabel.trim())}.size()
                    }
                    if (count > 0) {
                        targetLabel += " (${count})"
                    }
                    DataElement targetDataElement = new DataElement(label: targetLabel, dataType: sourceReferenceType,
                                                                    createdBy: currentUserEmailAddress)
                    link.addMetadata(targetDataElement)
                    DDHelperFunctions.addMetadata(targetDataElement, "direction", "supplier")
                    targetDataClass.addToDataElements(targetDataElement)


                }
            } else if (link.metaclass == "Generalization20") {
                DataClass thisDataClass = link.clientClass
                // Get the target class
                DataClass targetDataClass = link.supplierClass
                thisDataClass.addToExtendedDataClasses(targetDataClass)
            }
        }
    }
}