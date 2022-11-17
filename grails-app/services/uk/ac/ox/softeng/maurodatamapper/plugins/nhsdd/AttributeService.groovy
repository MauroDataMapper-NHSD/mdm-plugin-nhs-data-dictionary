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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ModelDataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDCode
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDElement
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary

@Slf4j
@Transactional
class AttributeService extends DataDictionaryComponentService<DataElement, NhsDDAttribute> {

    ElementService elementService

    @Override
    NhsDDAttribute show(UUID versionedFolderId, String id) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.newDataDictionary()
        DataElement attributeElement = dataElementService.get(id)
        NhsDDAttribute attribute = getNhsDataDictionaryComponentFromCatalogueItem(attributeElement, dataDictionary)
        attribute.instantiatedByElements.addAll (elementService.getAllForAttribute(versionedFolderId, attribute))
        attribute.definition = convertLinksInDescription(versionedFolderId, attribute.getDescription())
        attribute.codes.each {code ->
            if(code.webPresentation) {
                code.webPresentation = convertLinksInDescription(versionedFolderId, code.webPresentation)
            }
        }
        return attribute
    }

    Set<NhsDDAttribute> getAllForElement(UUID versionedFolderId, NhsDDElement nhsDDElement) {
        DataModel coreModel = nhsDataDictionaryService.getCoreModel(versionedFolderId)
        DataClass attributesClass = coreModel.dataClasses.find {it.label == NhsDataDictionary.ATTRIBUTES_CLASS_NAME}
        List<DataElement> dataElements = DataElement.by().inList('dataClass.id', attributesClass.id).list()
        List<String> linkedAttributeList = elementService.getLinkedAttributesFromMetadata(nhsDDElement.catalogueItem)
        linkedAttributeList.collect {elementName ->
            dataElements.find {it.label == elementName}
        }.collect {
            getNhsDataDictionaryComponentFromCatalogueItem(it, nhsDataDictionaryService.newDataDictionary())
        }

    }


    @Override
    Set<DataElement> getAll(UUID versionedFolderId, boolean includeRetired = false) {

        DataModel coreModel = nhsDataDictionaryService.getCoreModel(versionedFolderId)
        DataClass attributesClass = coreModel.dataClasses.find {it.label == NhsDataDictionary.ATTRIBUTES_CLASS_NAME}

        List<UUID> classIds = [attributesClass.id]
        if(includeRetired) {
            DataClass retiredAttributesClass = attributesClass.dataClasses.find {it.label == "Retired"}
            classIds.add(retiredAttributesClass.id)
        }
        List<DataElement> attributes = DataElement.by().inList('dataClass.id', classIds).list()

        attributes.findAll {dataElement ->
            includeRetired || !catalogueItemIsRetired(dataElement)
        }

    }

    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".attribute"
    }

    @Override
    NhsDDAttribute getNhsDataDictionaryComponentFromCatalogueItem(DataElement catalogueItem, NhsDataDictionary dataDictionary) {
        NhsDDAttribute attribute = new NhsDDAttribute()
        nhsDataDictionaryComponentFromItem(catalogueItem, attribute)
        if (catalogueItem.dataType instanceof ModelDataType) {
            List<Term> terms = termService.findAllByTerminologyId(((ModelDataType) catalogueItem.dataType).modelResourceId)
            List<NhsDDCode> codes = getCodesForTerms(terms, dataDictionary)
            codes.each {code ->
                code.owningAttribute = attribute
                attribute.codes.add(code)
            }
        }
        attribute.dataDictionary = dataDictionary
        return attribute

    }

    void persistAttributes(NhsDataDictionary dataDictionary,
                           VersionedFolder dictionaryFolder, DataModel coreDataModel, String currentUserEmailAddress,
                           Map<String, Terminology> attributeTerminologiesByName, Map<String, DataElement> attributeElementsByName) {

        Folder attributeTerminologiesFolder =
            new Folder(label: "Attribute Terminologies", createdBy: currentUserEmailAddress)
        dictionaryFolder.addToChildFolders(attributeTerminologiesFolder)

        if (!folderService.validate(attributeTerminologiesFolder)) {
            throw new ApiInvalidModelException('NHSDD', 'Invalid model', attributeTerminologiesFolder.errors)
        }
        folderService.save(attributeTerminologiesFolder)

        DataClass allAttributesClass = new DataClass(label: NhsDataDictionary.ATTRIBUTES_CLASS_NAME, createdBy: currentUserEmailAddress)

        DataClass retiredAttributesClass = new DataClass(label: "Retired", createdBy: currentUserEmailAddress,
                                                         parentDataClass: allAttributesClass)
        allAttributesClass.addToDataClasses(retiredAttributesClass)
        coreDataModel.addToDataClasses(allAttributesClass)
        coreDataModel.addToDataClasses(retiredAttributesClass)


        PrimitiveType stringDataType = coreDataModel.getPrimitiveTypes().find {it.label == "String"}
        if (!stringDataType) {
            stringDataType = new PrimitiveType(label: "String", createdBy: currentUserEmailAddress)
            coreDataModel.addToDataTypes(stringDataType)
        }

        Map<String, PrimitiveType> primitiveTypes = [:]
        Map<String, Folder> folders = [:]
        int idx = 0
        List<Terminology> terminologies = []
        dataDictionary.attributes.each { name, attribute ->

            if (attribute.codes.size() > 0) {
                String folderName = attribute.name.substring(0, 1).toUpperCase()
                Folder subFolder = folders[folderName]
                if (!subFolder) {
                    subFolder = new Folder(label: folderName, createdBy: currentUserEmailAddress)
                    attributeTerminologiesFolder.addToChildFolders(subFolder)
                    if (!folderService.validate(subFolder)) {
                        throw new ApiInvalidModelException('NHSDD', 'Invalid model', subFolder.errors)
                    }
                    folderService.save(subFolder)
                    folders[folderName] = subFolder
                }
                // attributeTerminologiesFolder.save()

                Terminology terminology = new Terminology(
                        label: name,
                        folder: subFolder,
                        createdBy: currentUserEmailAddress,
                        authority: authorityService.defaultAuthority,
                        branchName: dataDictionary.branchName)
                terminology.addToMetadata(new Metadata(namespace: "uk.nhs.datadictionary.terminology", key: "version", value: attribute.codesVersion))

                attribute.codes.each { code ->
                    Term term = new Term(
                            code: code.code,
                            definition: code.definition,
                            createdBy: currentUserEmailAddress,
                            label: "${code.code} : ${code.definition}",
                            depth: 1,
                            terminology: terminology
                    )

                    code.propertiesAsMap().each { key, value ->
                        if (value) {
                            term.addToMetadata(new Metadata(namespace: "uk.nhs.datadictionary.term",
                                    key: key,
                                    value: value,
                                    createdBy: currentUserEmailAddress))
                        }
                    }
                    terminology.addToTerms(term)

                }
                if (terminologyService.validate(terminology)) {
                    terminologies.add(terminology)
                    //terminology = terminologyService.saveModelWithContent(terminology)
                    //                    terminology.terms.size() // Required to reload the terms back into the session
                    attributeTerminologiesByName[name] = terminology
                } else {
                    GormUtils.outputDomainErrors(messageSource, terminology) // TODO throw exception???
                    log.error("Cannot save terminology: ${name}")
                }
            }
        }
        terminologies = terminologyService.saveModelsWithContent(terminologies, 1000)
        attributeTerminologiesByName.putAll(terminologies.collectEntries{ [it.label, it]})

        dataDictionary.attributes.each { name, attribute ->
            DataType dataType

            if (attribute.codes.size() > 0) {

                Terminology terminology = attributeTerminologiesByName[name]
                //nhsDataDictionary.attributeTerminologiesByName[name] = terminology
                dataType = new ModelDataType(label: "${name} Attribute Type",
                                             modelResourceDomainType: terminology.getDomainType(),
                                             modelResourceId: terminology.id,
                                             createdBy: currentUserEmailAddress)
                coreDataModel.addToDataTypes(dataType)
            } else {
                // no "code-system" nodes
                dataType = stringDataType
            }

            DataElement attributeDataElement = new DataElement(
                label: name,
                description: attribute.definition,
                createdBy: currentUserEmailAddress,
                dataType: dataType,
                index: idx++)

            addMetadataFromComponent(attributeDataElement, attribute, currentUserEmailAddress)

            if (attribute.isRetired()) {
                retiredAttributesClass.addToDataElements(attributeDataElement)
            } else {
                allAttributesClass.addToDataElements(attributeDataElement)
            }
            attributeElementsByName[name] = attributeDataElement

            // Reload all terms into the session
            attributeTerminologiesByName.each {k, v ->
                v.terms.size()
            }
        }

    }

    NhsDDAttribute getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        nhsDataDictionary.attributes.values().find {
            it.catalogueItem.id == catalogueItemId
        }
    }

}
