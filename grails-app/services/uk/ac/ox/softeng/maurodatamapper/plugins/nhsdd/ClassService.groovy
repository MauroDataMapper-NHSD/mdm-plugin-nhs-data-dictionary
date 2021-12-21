package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceTypeService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.ClassLink
import uk.nhs.digital.maurodatamapper.datadictionary.DDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.DDClass
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDClassLink
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDElement

@Slf4j
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
        uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary.METADATA_NAMESPACE + ".class"
    }

    void persistClasses(uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary dataDictionary,
                         VersionedFolder dictionaryFolder, DataModel coreDataModel, String currentUserEmailAddress,
                         Map<String, DataElement> attributeElementsByName) {

        DataClass parentDataClass = new DataClass(
            label: uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary.DATA_CLASSES_CLASS_NAME,
            createdBy: currentUserEmailAddress,
            dataModel: coreDataModel)
        coreDataModel.addToDataClasses(parentDataClass)

        DataClass retiredDataClass = new DataClass(
            label: "Retired",
            createdBy: currentUserEmailAddress,
            dataModel: coreDataModel)
        coreDataModel.addToDataClasses(retiredDataClass)
        parentDataClass.addToDataClasses(retiredDataClass)

        Map<String, DataClass> classesByName = [:]
        Map<String, DataClass> classesByUin = [:]
        dataDictionary.classes.each {name, clazz ->
            if(!classesByName[name] || !clazz.isRetired()) {
                DataClass dataClass = new DataClass(
                    label: name,
                    description: clazz.definition,
                    createdBy: currentUserEmailAddress,
                    parentDataClass: parentDataClass
                )
                if(clazz.isRetired()) {
                    retiredDataClass.addToDataClasses(dataClass)
                } else {
                    parentDataClass.addToDataClasses(dataClass)
                }
                coreDataModel.addToDataClasses(dataClass)

                // Now link the attributes from the properties

                clazz.allAttributes().each { attribute ->
                    DataElement attributeElement = attributeElementsByName[attribute.name]
                    if(attributeElement) {
                        attributeElement.addToImportingDataClasses(dataClass)
                    } else {
                        log.error("Cannot find attributeElement with name: " + attribute.name)
                    }
                }


                addMetadataFromComponent(dataClass, clazz, currentUserEmailAddress)

                classesByName[name] = dataClass
                classesByUin[clazz.getUin()] = dataClass


            }

        }

        // Now link the references
        Map<String, ReferenceType> classReferenceTypesByName = [:]
        dataDictionary.classes.each {name, clazz ->
            clazz.classLinks.each {classLink ->
                if (classLink.metaclass == "KernelAssociation20") {

                    if (classLink.supplierClass) {
                        DataClass thisDataClass = classesByUin[classLink.clientClass.getUin()]
                        // Get the target class
                        DataClass targetDataClass = classesByUin[classLink.supplierClass.getUin()]

                        // Get a reference type (create if it doesn't exist)
                        ReferenceType targetReferenceType = classReferenceTypesByName[targetDataClass.label]
                        if (!targetReferenceType) {

                            targetReferenceType = new ReferenceType(
                                label: "${targetDataClass.label} Reference",
                                createdBy: currentUserEmailAddress,
                                referenceClass: targetDataClass)
                            targetDataClass.addToReferenceTypes(targetReferenceType)
                            coreDataModel.addToDataTypes(targetReferenceType)
                            classReferenceTypesByName[targetDataClass.label] = targetReferenceType
                        }

                        ReferenceType sourceReferenceType = classReferenceTypesByName[thisDataClass.label]
                        if (!sourceReferenceType) {
                            sourceReferenceType = new ReferenceType(
                                label: "${thisDataClass.label} Reference",
                                createdBy: currentUserEmailAddress,
                                referenceClass: thisDataClass)
                            thisDataClass.addToReferenceTypes(sourceReferenceType)
                            coreDataModel.addToDataTypes(sourceReferenceType)
                            classReferenceTypesByName[thisDataClass.label] = sourceReferenceType
                        }

                        // create a data element in the class
                        String sourceLabel = classLink.clientRole
                        int count = 0
                        if (thisDataClass.dataElements) {
                            count = thisDataClass.dataElements.findAll {it.label.startsWith(sourceLabel.trim())}.size()
                        }
                        if (count > 0) {
                            sourceLabel += " (${count})"
                        }
                        DataElement sourceDataElement = new DataElement(label: sourceLabel, dataType: targetReferenceType,
                                                                        createdBy: currentUserEmailAddress)
                        addMetadataForLink(classLink, sourceDataElement, currentUserEmailAddress)
                        addToMetadata(sourceDataElement, "direction", "client", currentUserEmailAddress)
                        thisDataClass.addToDataElements(sourceDataElement)

                        String targetLabel = classLink.supplierRole
                        count = 0
                        if (targetDataClass.dataElements) {
                            count = targetDataClass.dataElements.findAll {it.label.startsWith(targetLabel.trim())}.size()
                        }
                        if (count > 0) {
                            targetLabel += " (${count})"
                        }
                        DataElement targetDataElement = new DataElement(label: targetLabel, dataType: sourceReferenceType,
                                                                        createdBy: currentUserEmailAddress)
                        addMetadataForLink(classLink, targetDataElement, currentUserEmailAddress)
                        addToMetadata(targetDataElement, "direction", "supplier", currentUserEmailAddress)
                        targetDataClass.addToDataElements(targetDataElement)
                    }
                } else if (classLink.metaclass == "Generalization20") {
                    DataClass thisDataClass = classesByUin[classLink.clientClass.getUin()]
                    // Get the target class
                    DataClass targetDataClass = classesByUin[classLink.supplierClass.getUin()]
                    thisDataClass.addToExtendedDataClasses(targetDataClass)
                }
            }
        }
    }

    void addMetadataForLink(NhsDDClassLink classLink, DataElement dataElement, String currentUserEmailAddress) {
        addToMetadata(dataElement, "uin", classLink.uin, currentUserEmailAddress)
        addToMetadata(dataElement, "metaclass", classLink.metaclass, currentUserEmailAddress)
        addToMetadata(dataElement, "clientRole", classLink.clientRole, currentUserEmailAddress)
        addToMetadata(dataElement, "supplierRole", classLink.supplierRole, currentUserEmailAddress)
        addToMetadata(dataElement, "clientCardinality", classLink.clientCardinality, currentUserEmailAddress)
        addToMetadata(dataElement, "supplierCardinality", classLink.supplierCardinality, currentUserEmailAddress)
        addToMetadata(dataElement, "name", classLink.name, currentUserEmailAddress)
        addToMetadata(dataElement, "partOfClientKey", classLink.partOfClientKey, currentUserEmailAddress)
        addToMetadata(dataElement, "partOfSupplierKey", classLink.partOfSupplierKey, currentUserEmailAddress)
        addToMetadata(dataElement, "supplierUin", classLink.supplierUin, currentUserEmailAddress)
        addToMetadata(dataElement, "clientUin", classLink.clientUin, currentUserEmailAddress)
        addToMetadata(dataElement, "relationSupplierExclusivity", classLink.relationSupplierExclusivity, currentUserEmailAddress)
        addToMetadata(dataElement, "relationClientExclusivity", classLink.relationClientExclusivity, currentUserEmailAddress)
        addToMetadata(dataElement, "direction", classLink.direction, currentUserEmailAddress)

    }

    NhsDDClass classFromDataClass(DataClass dc) {
        NhsDDClass clazz = new NhsDDClass()
        nhsDataDictionaryComponentFromItem(dc, clazz)
        return clazz
    }

}