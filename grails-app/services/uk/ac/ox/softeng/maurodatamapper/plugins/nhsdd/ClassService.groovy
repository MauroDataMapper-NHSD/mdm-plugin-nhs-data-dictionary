/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClassLink
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClassRelationship
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary

import javax.lang.model.type.PrimitiveType

@Slf4j
@Transactional
class ClassService extends DataDictionaryComponentService<DataClass, NhsDDClass> {

    AttributeService attributeService

    @Override
    NhsDDClass show(UUID versionedFolderId, String id) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.newDataDictionary()
        DataClass dataClass = dataClassService.get(id)
        NhsDDClass nhsClass = getNhsDataDictionaryComponentFromCatalogueItem(dataClass, dataDictionary)
        nhsClass.definition = convertLinksInDescription(versionedFolderId, nhsClass.getDescription())

        List<NhsDDAttribute> attributes = getAttributesForShow(nhsClass)
        // Assign the attribute by key and non-key types. The NhsDDClass.allAttributes() method will combine them
        nhsClass.keyAttributes = attributes.findAll { it.isKey }.sort { it.name }
        nhsClass.otherAttributes = attributes.findAll { !it.isKey }.sort { it.name }

        List<NhsDDClassRelationship> relationships = getRelationshipsForShow(nhsClass, dataDictionary)
        List<NhsDDClassRelationship> keyRelationships = relationships
            .findAll { it.isKey }
            .sort { it.targetClass.name }
        List<NhsDDClassRelationship> otherRelationships = relationships
            .findAll { !it.isKey }
            .sort { it.targetClass.name }
        nhsClass.classRelationships = keyRelationships + otherRelationships

        return nhsClass
    }

    List<NhsDDAttribute> getAttributesForShow(NhsDDClass nhsClass) {
        Set<DataElement> attributeDataElements = nhsClass.catalogueItem.dataElements.findAll {
            !(it.dataType instanceof ReferenceType)
        }

        // Get a cut-down version of the NhsDDAttribute list, we don't need national codes for previewing an NhsDDClass
        attributeDataElements.collect {dataElement ->
            NhsDDAttribute nhsAttribute = new NhsDDAttribute()
            attributeService.nhsDataDictionaryComponentFromItem(dataElement, nhsAttribute, dataElement.metadata.toList())
            nhsAttribute
        }
    }

    List<NhsDDClassRelationship> getRelationshipsForShow(NhsDDClass nhsClass, NhsDataDictionary dataDictionary) {
        Set<DataElement> relationshipDataElements = nhsClass.catalogueItem.dataElements.findAll {
            it.dataType instanceof ReferenceType
        }

        relationshipDataElements.collect { dataElement ->
            DataClass referencedClass = ((ReferenceType)dataElement.dataType).referenceClass
            NhsDDClass referencedNhsClass = getNhsDataDictionaryComponentFromCatalogueItem(referencedClass, dataDictionary)

            NhsDDClassRelationship relationship = new NhsDDClassRelationship(targetClass: referencedNhsClass)
                .tap {
                    setDescription(dataElement)
                    isKey = dataElement.metadata.find {it.key == "isKey"}
                }
            relationship
        }
    }

//    @Override
//    NhsDDClass show(UUID versionedFolderId, String id) {
//        NhsDataDictionary dataDictionary = nhsDataDictionaryService.newDataDictionary()
//        DataClass dataClass = dataClassService.get(id)
//
//        NhsDDClass nhsClass = getNhsDataDictionaryComponentFromCatalogueItem(dataClass, dataDictionary)
//        nhsClass.definition = convertLinksInDescription(versionedFolderId, dataClass.description)
//
//        List<NhsDDAttribute> attributes = null
//        nhsClass.keyAttributes = attributes.findAll { attribute -> attribute.k}
//
//
////        String description = convertLinksInDescription(branch, dataClass.description)
////        DataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(branch)
//
////        String shortDesc = replaceLinksInShortDescription(getShortDescription(dataClass, dataDictionary))
////        def result = [
////            catalogueId     : dataClass.id.toString(),
////            name            : dataClass.label,
////            stereotype      : "class",
////            shortDescription: shortDesc,
////            description     : description,
////            alsoKnownAs     : getAliases(dataClass)
////        ]
//
//        List<Map> attributes = []
//        Collection<DataElement> basicElements = dataClass.dataElements.
//            findAll { !(it.dataType instanceof ReferenceType) }
//
//        basicElements.sort { a, b ->
//            a.metadata.
//            DDHelperFunctions.getMetadataValue(a, "isUnique") <=> DDHelperFunctions.getMetadataValue(b, "isUnique") ?:
//            b.label <=> a.label
//        }.reverse().each { de ->
//
//            String uin = DDHelperFunctions.getMetadataValue(de, "uin")
//            DDAttribute ddAttribute = dataDictionary.attributes[uin]
//            if (ddAttribute) {
//
//                Map attributesMap = [:]
//                String key = ""
//                if (DDHelperFunctions.getMetadataValue(de, "isUnique")) {
//                    key = "Key"
//                }
//                attributesMap["key"] = key
//                attributesMap["catalogueId"] = ddAttribute.catalogueItem.id.toString()
//                attributesMap["name"] = ddAttribute.name
//                attributesMap["stereotype"] = "attribute"
//                attributes.add(attributesMap)
//            } else {
//                log.error("Cannot lookup attribute: " + uin)
//            }
//        }
//        result["attributes"] = attributes
//
//        List<Map> relationships = []
//
//        List<Map> keyRows = []
//        List<Map> otherRows = []
//        dataClass.dataElements.
//            findAll { it.dataType instanceof ReferenceType }.
//            findAll {
//                (DDHelperFunctions.getMetadataValue(it, "direction") == "supplier" &&
//                 DDHelperFunctions.getMetadataValue(it, "partOfClientKey") == "true") ||
//                (DDHelperFunctions.getMetadataValue(it, "direction") == "client" &&
//                 DDHelperFunctions.getMetadataValue(it, "partOfSupplierKey") == "true")
//            }.
//            each { dataElement ->
//                DDClass targetClass = dataDictionary.classes.values().find {
//                    it.uin == DDHelperFunctions.getMetadataValue(dataElement, "clientUin")
//                }
//                String relationshipLabel = dataElement.label
//                int bracketLocation = relationshipLabel.indexOf("(")
//                if (bracketLocation >= 0) {
//                    relationshipLabel = relationshipLabel.substring(0, bracketLocation)
//                }
//                keyRows.add([
//                    key         : "Key",
//                    relationship: ClassLink.getCardinalityText(DDHelperFunctions.getMetadataValue(dataElement, "clientCardinality"),
//                                                               relationshipLabel),
//                    catalogueId : targetClass.catalogueItem.id.toString(),
//                    name        : targetClass.name,
//                    stereotype  : "class"
//                ])
//            }
//        dataClass.dataElements.
//            findAll { it.dataType instanceof ReferenceType }.
//            findAll {
//                !(DDHelperFunctions.getMetadataValue(it, "direction") == "supplier" &&
//                  DDHelperFunctions.getMetadataValue(it, "partOfClientKey") == "true") &&
//                !(DDHelperFunctions.getMetadataValue(it, "direction") == "client" &&
//                  DDHelperFunctions.getMetadataValue(it, "partOfSupplierKey") == "true")
//
//            }.
//            each { dataElement ->
//                DDClass targetClass = dataDictionary.classes.values().find {
//                    (DDHelperFunctions.getMetadataValue(dataElement, "direction") == "supplier" &&
//                     it.uin == DDHelperFunctions.getMetadataValue(dataElement, "clientUin")) ||
//                    (DDHelperFunctions.getMetadataValue(dataElement, "direction") == "client" &&
//                     it.uin == DDHelperFunctions.getMetadataValue(dataElement, "supplierUin"))
//                }
//                String relationshipLabel = dataElement.label
//                int bracketLocation = relationshipLabel.indexOf("(")
//                if (bracketLocation >= 0) {
//                    relationshipLabel = relationshipLabel.substring(0, bracketLocation)
//                }
//                otherRows.add([
//                    relationSupplierExclusivity: DDHelperFunctions.getMetadataValue(dataElement, "relationSupplierExclusivity"),
//                    relationClientExclusivity  : DDHelperFunctions.getMetadataValue(dataElement, "relationClientExclusivity"),
//                    relationship               : ClassLink.getCardinalityText(DDHelperFunctions.getMetadataValue(dataElement, "clientCardinality"),
//                                                                              relationshipLabel),
//                    catalogueId                : targetClass.catalogueItem.id.toString(),
//                    name                       : targetClass.name,
//                    stereotype                 : "class"
//
//                ])
//            }
//        keyRows.sort { it.name }.each { row ->
//            relationships.add(row)
//        }
//        otherRows.sort { a, b ->
//            a.relationSupplierExclusivity <=> b.relationSupplierExclusivity ?:
//            a.relationClientExclusivity <=> b.relationClientExclusivity ?:
//            b.name <=> a.name
//        }.reverse().each { row ->
//            if (!relationships.contains(row)) {
//                relationships.add(row)
//                String relationSupplierExclusivity = row.relationSupplierExclusivity
//                if (relationSupplierExclusivity && relationSupplierExclusivity != "") {
//                    otherRows.findAll { it.relationSupplierExclusivity == relationSupplierExclusivity && it != row }.
//                        sort { it.name }.
//                        each { linkedRow ->
//                            relationships.add(linkedRow)
//                            //linkedRow.stEntries.remove(0)
//                            //linkedRow.stEntries.value = ""
//                            linkedRow.key = "or " + linkedRow.relationClientExclusivity
//                            //linkedRow.stEntries[0].value = ""
//
//                        }
//
//                }
//                String relationClientExclusivity = row.relationClientExclusivity
//                if (relationClientExclusivity && relationClientExclusivity != "") {
//                    otherRows.findAll { it.relationClientExclusivity == relationClientExclusivity && it != row }.
//                        sort { it.name }.
//                        each { linkedRow ->
//                            relationships.add(linkedRow)
//                            //linkedRow.stEntries.remove(0)
//                            //linkedRow.stEntries.value = ""
//                            linkedRow.key = "or " + linkedRow.relationClientExclusivity
//                            //linkedRow.stEntries[0].value = ""
//
//                        }
//
//                }
//            }
//        }
//        relationships.each { row ->
//            if (row.relationSupplierExclusivity != "Key") {
//                row.relationSupplierExclusivity = ""
//            }
//        }
//        result["relationships"] = relationships
//        return result
//    }

    @Override
    Set<DataClass> getAll(UUID versionedFolderId, boolean includeRetired = false) {
        DataModel coreModel = nhsDataDictionaryService.getClassesModel(versionedFolderId)
        List<DataClass> classesClass = DataClass.byDataModelId(coreModel.id).toList()

        classesClass.findAll {dataClass ->
            dataClass.label != "Retired" && (
                includeRetired || !catalogueItemIsRetired(dataClass))
        }
    }

    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".class"
    }

    @Override
    NhsDDClass getNhsDataDictionaryComponentFromCatalogueItem(DataClass catalogueItem, NhsDataDictionary dataDictionary, List<Metadata> metadata = null) {
        NhsDDClass clazz = new NhsDDClass()
        nhsDataDictionaryComponentFromItem(catalogueItem, clazz, metadata)
        clazz.dataDictionary = dataDictionary
        return clazz
    }

    void persistClasses(NhsDataDictionary dataDictionary, DataModel classesDataModel, String currentUserEmailAddress,
                        Map<String, DataClass> attributeClassesByUin, Set<String> attributeUinIsKey) {

        DataClass retiredDataClass = new DataClass(
            label: "Retired",
            createdBy: currentUserEmailAddress,
            dataModel: classesDataModel)
        classesDataModel.addToDataClasses(retiredDataClass)

        TreeMap<String, DataClass> classesByUin = new TreeMap<>()

        dataDictionary.classes.each {name, clazz ->
            DataClass dataClass = new DataClass(
                label: name,
                description: clazz.definition,
                createdBy: currentUserEmailAddress,
            )

            // TODO unnecessary as the or statement above excludes all non-retired DCs
            if (clazz.isRetired()) {
                classesDataModel.addToDataClasses(dataClass)
                retiredDataClass.addToDataClasses(dataClass)
            } else {
                classesDataModel.addToDataClasses(dataClass)
            }
            // We used to link the attributes here, but now that's all done in the attribute
            // service because they're stored directly there.
            // However, since the classes contain the information about which attribute appears in which class,
            // we'll maintain a map here

            clazz.allAttributes().each {
                attributeClassesByUin[it.uin] = dataClass
            }
            clazz.keyAttributes.each {
                attributeUinIsKey.add(it.uin)
            }
            addMetadataFromComponent(dataClass, clazz, currentUserEmailAddress)

            classesByUin[clazz.getUin()] = dataClass
        }

        // Now link the references - we can only do this once all the classes are in place
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
                            classesDataModel.addToDataTypes(targetReferenceType)
                            classReferenceTypesByName[targetDataClass.label] = targetReferenceType
                        }

                        ReferenceType sourceReferenceType = classReferenceTypesByName[thisDataClass.label]
                        if (!sourceReferenceType) {
                            sourceReferenceType = new ReferenceType(
                                label: "${thisDataClass.label} Reference",
                                createdBy: currentUserEmailAddress,
                                referenceClass: thisDataClass)
                            thisDataClass.addToReferenceTypes(sourceReferenceType)
                            classesDataModel.addToDataTypes(sourceReferenceType)
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
            "direction": classLink.direction,
            "isKey": classLink.partOfClientKey,
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