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
package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceTypeService

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClassLink
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary

import javax.lang.model.type.PrimitiveType

@Slf4j
@Transactional
class ClassService extends DataDictionaryComponentService<DataClass, NhsDDClass> {

    ReferenceTypeService referenceTypeService

    @Override
    NhsDDClass show(UUID versionedFolderId, String id) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.newDataDictionary()
        DataClass classDataClass = dataClassService.get(id)
        NhsDDClass dataClass = getNhsDataDictionaryComponentFromCatalogueItem(classDataClass, dataDictionary)
        dataClass.definition = convertLinksInDescription(versionedFolderId, dataClass.getDescription())
        return dataClass
    }
    /*
    @Override
    def show(UUID versionedFolderId, String id) {
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
*/
    @Override
    Set<DataClass> getAll(UUID versionedFolderId, boolean includeRetired = false) {
        DataModel coreModel = nhsDataDictionaryService.getCoreModel(versionedFolderId)
        DataClass classesClass = coreModel.dataClasses.find {it.label == NhsDataDictionary.DATA_CLASSES_CLASS_NAME}

        List<DataClass> allClasses = []
        allClasses.addAll(DataClass.byParentDataClassId(classesClass.id).list())

        if(includeRetired) {
            DataClass retiredClassesClass = allClasses.find {it.label == "Retired"}
            allClasses.addAll(DataClass.byParentDataClassId(retiredClassesClass.id).list())
        }

        allClasses.findAll {dataClass ->
            dataClass.label != "Retired" && (
                includeRetired || !catalogueItemIsRetired(dataClass))
        }
    }

    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".class"
    }

    @Override
    NhsDDClass getNhsDataDictionaryComponentFromCatalogueItem(DataClass catalogueItem, NhsDataDictionary dataDictionary) {
        NhsDDClass clazz = new NhsDDClass()
        nhsDataDictionaryComponentFromItem(catalogueItem, clazz)
        clazz.dataDictionary = dataDictionary
        return clazz
    }

    void persistClasses(NhsDataDictionary dataDictionary,
                        VersionedFolder dictionaryFolder, DataModel coreDataModel, String currentUserEmailAddress,
                        Map<String, DataElement> attributeElementsByName) {

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

        TreeMap<String, DataClass> classesByName = new TreeMap<>()
        TreeMap<String, DataClass> classesByUin = new TreeMap<>()
        dataDictionary.classes.each {name, clazz ->
            if (!classesByName[name] || !clazz.isRetired()) {
                DataClass dataClass = new DataClass(
                    label: name,
                    description: clazz.definition,
                    createdBy: currentUserEmailAddress,
                    parentDataClass: parentDataClass
                )

                // TODO unnecessary as the or statement above excludes all non-retired DCs
                if (clazz.isRetired()) {
                    retiredDataClass.addToDataClasses(dataClass)
                } else {
                    parentDataClass.addToDataClasses(dataClass)
                }
                coreDataModel.addToDataClasses(dataClass)

                // Now link the attributes from the properties

                clazz.allAttributes().each {attribute ->
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
        TreeMap<String, ReferenceType> classReferenceTypesByName = new TreeMap<>()
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
        Map<String, String> metadata = [
            "uin": classLink.uin,
            "metaclass": classLink.metaclass,
            "clientRole": classLink.clientRole,
            "supplierRole": classLink.supplierRole,
            "clientCardinality": classLink.clientCardinality,
            "supplierCardinality": classLink.supplierCardinality,
            "name": classLink.name,
            "partOfClientKey": classLink.partOfClientKey,
            "partOfSupplierKey": classLink.partOfSupplierKey,
            "supplierUin": classLink.supplierUin,
            "clientUin": classLink.clientUin,
            "relationSupplierExclusivity": classLink.relationSupplierExclusivity,
            "relationClientExclusivity": classLink.relationClientExclusivity,
            "direction": classLink.direction
        ]
        metadata.each {key, value ->
            addToMetadata(dataElement, key, value, currentUserEmailAddress)
        }
    }

    NhsDDClass classFromDataClass(DataClass dc, NhsDataDictionary dataDictionary) {
        NhsDDClass clazz = getNhsDataDictionaryComponentFromCatalogueItem(dc, dataDictionary)
        dc.dataElements.each {dataElement ->
            if(dataElement.dataType instanceof PrimitiveType) {
                NhsDDAttribute foundAttribute = dataDictionary.attributes[dataElement.label]
                if (foundAttribute) {
                    clazz.keyAttributes.add(foundAttribute)
                } else {
                    log.error("Cannot find attribute: {}", dataElement.label)
                }
            }
        }
        return clazz
    }

    NhsDDClass getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        nhsDataDictionary.classes.values().find {
            it.catalogueItem.id == catalogueItemId
        }
    }

}