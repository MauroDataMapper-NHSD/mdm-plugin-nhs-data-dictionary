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
        dataDictionary.containingVersionedFolder = versionedFolderService.get(versionedFolderId)

        DataClass dataClass = dataClassService.get(id)
        NhsDDClass nhsClass = getNhsDataDictionaryComponentFromCatalogueItem(dataClass, dataDictionary)
        nhsClass.definition = convertLinksInDescription(versionedFolderId, nhsClass.getDescription())

        List<NhsDDAttribute> attributes = getAttributesForShow(nhsClass, dataDictionary)
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

    List<NhsDDAttribute> getAttributesForShow(NhsDDClass nhsClass, NhsDataDictionary dataDictionary) {
        Set<DataElement> attributeDataElements = nhsClass.catalogueItem.dataElements.findAll {
            !(it.dataType instanceof ReferenceType)
        }

        // Get a cut-down version of the NhsDDAttribute list, we don't need national codes for previewing an NhsDDClass
        attributeDataElements
            .collect {dataElement ->
                NhsDDAttribute nhsAttribute = new NhsDDAttribute()
                attributeService.nhsDataDictionaryComponentFromItem(dataDictionary, dataElement, nhsAttribute, dataElement.metadata.toList())
                nhsAttribute
            }
        .findAll { nhsAttribute ->
            // Do not include retired attributes in the list
            !nhsAttribute.isRetired()
        }
    }

    List<NhsDDClassRelationship> getRelationshipsForShow(NhsDDClass nhsClass, NhsDataDictionary dataDictionary) {
        Set<DataElement> relationshipDataElements = nhsClass.catalogueItem.dataElements.findAll {
            it.dataType instanceof ReferenceType
        }

        relationshipDataElements.collect { dataElement ->
            DataClass referencedClass = ((ReferenceType)dataElement.dataType).referenceClass
            NhsDDClass referencedNhsClass = getNhsDataDictionaryComponentFromCatalogueItem(referencedClass, dataDictionary)

            NhsDDClassRelationship relationship = new NhsDDClassRelationship(dataElement, referencedNhsClass)
            relationship
        }
    }

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
        nhsDataDictionaryComponentFromItem(dataDictionary, catalogueItem, clazz, metadata)
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
                        NhsDDClassLink.setMultiplicityToDataElement(sourceDataElement, classLink.supplierCardinality)
                        addMetadataForLink(classLink, sourceDataElement, currentUserEmailAddress)
                        addToMetadata(sourceDataElement, NhsDDClassLink.IS_KEY_METADATA_KEY, classLink.isPartOfSupplierKey().toString(), currentUserEmailAddress)
                        addToMetadata(sourceDataElement, NhsDDClassLink.IS_CHOICE_METADATA_KEY, classLink.hasRelationClientExclusivity().toString(), currentUserEmailAddress)
                        addToMetadata(sourceDataElement, NhsDDClassLink.DIRECTION_METADATA_KEY, NhsDDClassLink.CLIENT_DIRECTION, currentUserEmailAddress)
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
                        NhsDDClassLink.setMultiplicityToDataElement(targetDataElement, classLink.clientCardinality)
                        addMetadataForLink(classLink, targetDataElement, currentUserEmailAddress)
                        addToMetadata(targetDataElement, NhsDDClassLink.IS_KEY_METADATA_KEY, classLink.isPartOfClientKey().toString(), currentUserEmailAddress)
                        addToMetadata(targetDataElement, NhsDDClassLink.IS_CHOICE_METADATA_KEY, classLink.hasRelationSupplierExclusivity().toString(), currentUserEmailAddress)
                        addToMetadata(targetDataElement, NhsDDClassLink.DIRECTION_METADATA_KEY, NhsDDClassLink.SUPPLIER_DIRECTION, currentUserEmailAddress)
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