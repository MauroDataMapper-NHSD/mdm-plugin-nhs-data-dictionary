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
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ModelDataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import uk.nhs.digital.maurodatamapper.datadictionary.old.DDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.old.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.old.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDCode
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDElement
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary

@Slf4j
@Transactional
class ElementService extends DataDictionaryComponentService<DataElement, NhsDDElement> {

    AttributeService attributeService

    @Override
    NhsDDElement show(UUID versionedFolderId, String id) {
        DataElement elementElement = dataElementService.get(id)
        NhsDDElement element = getNhsDataDictionaryComponentFromCatalogueItem(elementElement, nhsDataDictionaryService.newDataDictionary())
        element.instantiatesAttributes.addAll(attributeService.getAllForElement(versionedFolderId, element))
        element.definition = convertLinksInDescription(versionedFolderId, element.getDescription())
        return element
    }

    /*    @Override
        def show(UUID versionedFolderId, String id) {
            DataElement dataElement = dataElementService.get(id)

            String description = convertLinksInDescription(branch, dataElement.description)

            DataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(branch)
            String shortDesc = replaceLinksInShortDescription(getShortDescription(dataElement, null))
            def result = [
                    catalogueId: dataElement.id.toString(),
                    name: dataElement.label,
                    stereotype: "element",
                    shortDescription: shortDesc,
                    description: description,
                    alsoKnownAs: getAliases(dataElement)
            ]
            String formatLength = DDHelperFunctions.getMetadataValue(dataElement, "format-length")

            if(!isRetired(dataElement) && formatLength) {
                result["formatLength"] = formatLength
            }

            List<DataElement> elementAttributes = getElementAttributes(dataElement, dataDictionary)

           if (dataElement.dataType instanceof EnumerationType) {
                List<EnumerationValue> enumValues = ((EnumerationType) dataElement.dataType).enumerationValues.sort {
                    enumValue ->
                        String webOrderString = DDHelperFunctions.getMetadataValue(enumValue, "Web Order")
                        if (webOrderString) {
                            return Integer.parseInt(webOrderString)
                        } else return 0
                }
                List<EnumerationValue> permittedNationalCodes = enumValues.findAll {
                    DDHelperFunctions.getMetadataValue(it, "Permitted National Code") == "true"
                }
                if (permittedNationalCodes.size() > 0) {
                    String nationalCodesTitle = "permittedNationalCodes"
                    if(elementAttributes.size() == 1 &&
                            elementAttributes.get(0).dataType.getClass() == EnumerationType.class) {
                        List<EnumerationValue> attributeValues =
                                ((EnumerationType)elementAttributes.get(0).dataType).getEnumerationValues()
                        if(attributeValues.size() == permittedNationalCodes.size()) {
                            nationalCodesTitle = "nationalCodes"
                        }
                    }

                    result[nationalCodesTitle] = generateCodeList(permittedNationalCodes)
                }
                List<EnumerationValue> nationalCodes = enumValues.findAll {
                    !DDHelperFunctions.getMetadataValue(it, "Permitted National Code") &&
                            DDHelperFunctions.getMetadataValue(it, "Web Order") != "0"
                }
                if (nationalCodes.size() > 0) {
                    String nationalCodesTitle = "permittedNationalCodes"
                    if(elementAttributes.size() == 1 &&
                            elementAttributes.get(0).dataType.getClass() == EnumerationType.class) {
                        Set<EnumerationValue> attributeValues =
                                ((EnumerationType)getElementAttributes(dataElement, dataDictionary).get(0).dataType).getEnumerationValues()
                        if(attributeValues.size() == nationalCodes.size()) {
                            nationalCodesTitle = "nationalCodes"
                        }
                    }
                    result[nationalCodesTitle] = generateCodeList(nationalCodes)
                }
                List<EnumerationValue> defaultCodes = enumValues.findAll {
                    //DDHelperFunctions.getMetadataValue(it, "Default Code") == "true"
                    DDHelperFunctions.getMetadataValue(it, "Web Order") == "0"
                }
                if (defaultCodes.size() > 0) {
                    result["defaultCodes"] = generateCodeList(defaultCodes)
                }
            }

            if(elementAttributes.size() > 0) {
                List<Map> elementAttributesList = []
                elementAttributes.each {attribute ->
                    Map attributeMap = [
                        catalogueId: attribute.id.toString(),
                        name: attribute.label,
                        stereotype: "attribute",
                    ]
                    elementAttributesList.add(attributeMap)
                }
                result["attributes"] = elementAttributesList
            }
            return result
        }
    */

    @Override
    Set<DataElement> getAll(UUID versionedFolderId, boolean includeRetired = false) {

        DataModel coreModel = nhsDataDictionaryService.getCoreModel(versionedFolderId)
        DataClass elementsClass = coreModel.dataClasses.find {it.label == NhsDataDictionary.DATA_ELEMENTS_CLASS_NAME}

        List<UUID> classIds = [elementsClass.id]
        if (includeRetired) {
            DataClass retiredElementsClass = elementsClass.dataClasses.find {it.label == "Retired"}
            classIds.add(retiredElementsClass.id)
        }
        List<DataElement> dataElements = DataElement.by().inList('dataClass.id', classIds).list()

        dataElements.findAll {dataElement ->
            includeRetired || !catalogueItemIsRetired(dataElement)
        }

    }

    Set<NhsDDElement> getAllForAttribute(UUID versionedFolderId, NhsDDAttribute nhsDDAttribute) {
        DataModel coreModel = nhsDataDictionaryService.getCoreModel(versionedFolderId)
        DataClass elementsClass = coreModel.dataClasses.find {it.label == NhsDataDictionary.DATA_ELEMENTS_CLASS_NAME}
        List<DataElement> dataElements = DataElement.by().inList('dataClass.id', elementsClass.id).list()
        dataElements.findAll {dataElement ->
            attributeListIncludesName(dataElement, nhsDDAttribute.name)
        }.collect {
            getNhsDataDictionaryComponentFromCatalogueItem(it, nhsDataDictionaryService.newDataDictionary())
        }
    }

    boolean attributeListIncludesName(CatalogueItem catalogueItem, String name) {
        List<String> linkedAttributeList = getLinkedAttributesFromMetadata(catalogueItem)
        if (!linkedAttributeList) {
            return false
        }
        return linkedAttributeList.contains(name)
    }

    List<String> getLinkedAttributesFromMetadata(CatalogueItem catalogueItem) {
        List<Metadata> allRelevantMetadata = Metadata
            .byMultiFacetAwareItemIdAndNamespace(catalogueItem.id, getMetadataNamespace())
            .eq('key', "linkedAttributes")
            .list()
        if (allRelevantMetadata) {
            String stringList = allRelevantMetadata.first().value
            return StringUtils.split(stringList, ';')
        } else {
            return []
        }

    }


    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".element"
    }

    @Override
    NhsDDElement getNhsDataDictionaryComponentFromCatalogueItem(DataElement catalogueItem, NhsDataDictionary dataDictionary) {
        NhsDDElement element = new NhsDDElement()
        nhsDataDictionaryComponentFromItem(catalogueItem, element)
        String linkedAttributeMetadata = element.otherProperties["linkedAttributes"]
        if (linkedAttributeMetadata) {
            String[] linkedAttributeNames = StringUtils.split(linkedAttributeMetadata, ';')
            linkedAttributeNames.each {
                NhsDDAttribute linkedAttribute = dataDictionary.attributes[it]
                if (linkedAttribute) {
                    element.instantiatesAttributes.add(linkedAttribute)
                }
            }
        }
        if (catalogueItem.dataType instanceof ModelDataType) {
            List<Term> terms = termService.findAllByCodeSetId(((ModelDataType) catalogueItem.dataType).modelResourceId)
            List<NhsDDCode> codes = getCodesForTerms(terms, dataDictionary)
            codes.each {code ->
                code.usedByElements.add(element)
                element.codes.add(code)
            }
        }
        element.dataDictionary = dataDictionary
        return element
    }

    List<DataElement> getElementAttributes(DataElement dataElement, DataDictionary dataDictionary) {
        List<DataElement> attributeElements = []
        String elementAttributeList = DDHelperFunctions.getMetadataValue(dataElement, "element_attributes")
        if (elementAttributeList) {
            StringUtils.split(elementAttributeList, ",").each {uin ->
                DDAttribute attribute = dataDictionary.attributes[uin]
                if (attribute) {
                    attributeElements.add(attribute.catalogueItem)
                } else {
                    log.error("Cannot find uin: " + uin)
                }
            }
        }
        return attributeElements
    }

    List<Map> generateCodeList(List<EnumerationValue> codes) {
        List<Map> returnCodesList = []
        codes.each {code ->
            Map codeMap = [:]
            String label = DDHelperFunctions.getMetadataValue(code, "label")
            codeMap["code"] = label ?: code.key
            String webPresentation = DDHelperFunctions.getMetadataValue(code, "Web Presentation")
            codeMap["value"] = webPresentation ?: code.value
            String retired = DDHelperFunctions.getMetadataValue(code, "isRetired")
            codeMap["retired"] = retired

            returnCodesList.add(codeMap)
        }
        return returnCodesList
    }

    void persistElements(NhsDataDictionary dataDictionary,
                         VersionedFolder dictionaryFolder, DataModel coreDataModel, String currentUserEmailAddress,
                         Map<String, Terminology> attributeTerminologiesByName) {

        PrimitiveType stringDataType = coreDataModel.getPrimitiveTypes().find {it.label == "String"}
        if (!stringDataType) {
            stringDataType = new PrimitiveType(label: "String", createdBy: currentUserEmailAddress)
            coreDataModel.addToDataTypes(stringDataType)
        }

        Folder dataElementCodeSetsFolder =
            new Folder(label: "Data Element CodeSets", createdBy: currentUserEmailAddress)
        dictionaryFolder.addToChildFolders(dataElementCodeSetsFolder)
        if (!folderService.validate(dataElementCodeSetsFolder)) {
            throw new ApiInvalidModelException('NHSDD', 'Invalid model', dataElementCodeSetsFolder.errors)
        }
        folderService.save(dataElementCodeSetsFolder)

        DataClass allElementsClass = new DataClass(label: NhsDataDictionary.DATA_ELEMENTS_CLASS_NAME, createdBy:
            currentUserEmailAddress)

        DataClass retiredElementsClass = new DataClass(label: "Retired", createdBy: currentUserEmailAddress,
                                                       parentDataClass: allElementsClass)
        allElementsClass.addToDataClasses(retiredElementsClass)
        coreDataModel.addToDataClasses(allElementsClass)
        coreDataModel.addToDataClasses(retiredElementsClass)


        Map<String, Folder> folders = [:]
        int idx = 0
        // Would sort, but assume already sorted
        dataDictionary.elements.each {name, element ->
            DataType dataType
            if (element.codes.size() > 0 && !element.isRetired()) {
                String folderName = name.substring(0, 1).toUpperCase()
                Folder subFolder = folders[folderName]
                if (!subFolder) {
                    subFolder = new Folder(label: folderName, createdBy: currentUserEmailAddress)
                    dataElementCodeSetsFolder.addToChildFolders(subFolder)
                    if (!folderService.validate(subFolder)) {
                        throw new ApiInvalidModelException('NHSDD', 'Invalid model', subFolder.errors)
                    }
                    folderService.save(subFolder)
                    folders[folderName] = subFolder
                }


                CodeSet codeSet = new CodeSet(
                    label: name,
                    folder: subFolder,
                    createdBy: currentUserEmailAddress,
                    authority: authorityService.defaultAuthority,
                    branchName: dataDictionary.branchName)
                codeSet.addToMetadata(new Metadata(namespace: "uk.nhs.datadictionary.codeset", key: "version", value: element.codeSetVersion))


                // String terminologyUin = ddDataElement.link.participant.find {it -> it.@role == 'Supplier'}.@referencedUin
                // Terminology attributeTerminology = dataDictionary.attributeTerminologiesByName[terminologyUin]
                element.codes.each {code ->
                    Terminology attributeTerminology = attributeTerminologiesByName[code.owningAttribute.name]
                    if (!attributeTerminology) {
                        log.error("No terminology with name ${code.owningAttribute.name} found for element ${name}")
                    } else {
                        Term t = attributeTerminology.findTermByCode(code.code)
                        if (!t) {
                            log.error("Cannot find term: ${code.code}")
                        } else {
                            codeSet.addToTerms(t)
                        }
                    }

                }
                if (codeSetService.validate(codeSet)) {
                    codeSet = codeSetService.saveModelWithContent(codeSet)
                } else {
                    GormUtils.outputDomainErrors(messageSource, codeSet) // TODO throw exception???
                }
                dataType = new ModelDataType(label: "${name} Element Type",
                                             modelResourceDomainType: codeSet.getDomainType(),
                                             modelResourceId: codeSet.id,
                                             createdBy: currentUserEmailAddress)
                coreDataModel.addToDataTypes(dataType)
            } else {
                // no "value-set" nodes
                dataType = stringDataType
            }
            DataElement elementDataElement = new DataElement(
                label: name,
                description: element.definition,
                createdBy: currentUserEmailAddress,
                dataType: dataType,
                index: idx++)

            addMetadataFromComponent(elementDataElement, element, currentUserEmailAddress)

            String elementAttributes = StringUtils.join(element.instantiatesAttributes.collect {it.name}, ";")

            addToMetadata(elementDataElement, "linkedAttributes", elementAttributes, currentUserEmailAddress)


            if (element.isRetired()) {
                retiredElementsClass.addToDataElements(elementDataElement)
            } else {
                allElementsClass.addToDataElements(elementDataElement)
            }
            dataDictionary.elementsByUrl[element.otherProperties["ddUrl"]] = elementDataElement
        }

    }

    NhsDDElement getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        nhsDataDictionary.elements.values().find {
            it.catalogueItem.id == catalogueItemId
        }
    }

}
