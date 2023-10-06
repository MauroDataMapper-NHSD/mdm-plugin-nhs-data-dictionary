/*
 * Copyright 2020-2023 University of Oxford and NHS England
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

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ModelDataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles.DDCodeSetProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDCode
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDElement
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.utils.DDHelperFunctions

@Slf4j
@Transactional
class ElementService extends DataDictionaryComponentService<DataElement, NhsDDElement> {

    AttributeService attributeService
    @Autowired
    DDCodeSetProfileProviderService ddCodeSetProfileProviderService

    @Override
    NhsDDElement show(UUID versionedFolderId, String id) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.newDataDictionary()
        DataElement elementElement = dataElementService.get(id)
        NhsDDElement element = getNhsDataDictionaryComponentFromCatalogueItem(elementElement, dataDictionary)
        element.instantiatesAttributes.addAll(attributeService.getAllForElement(versionedFolderId, element))
        element.definition = convertLinksInDescription(versionedFolderId, element.getDescription())
        element.codes.each {code ->
            if(code.webPresentation) {
                code.webPresentation = convertLinksInDescription(versionedFolderId, code.webPresentation)
            }
        }
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
        DataModel coreModel = nhsDataDictionaryService.getElementsModel(versionedFolderId)

        return coreModel.allDataElements.findAll {dataElement ->
            includeRetired || !catalogueItemIsRetired(dataElement)
        }
    }

    Set<NhsDDElement> getAllForAttribute(UUID versionedFolderId, NhsDDAttribute nhsDDAttribute) {
        SemanticLink.byTargetMultiFacetAwareItemId(nhsDDAttribute.catalogueItem.id).list().collect { link ->
            DataElement.get(link.multiFacetAwareItemId)
        }.collect { dataElement ->
            dataElement.metadata.size() // For later conversion to stereotyped item and to find out if retired
            getNhsDataDictionaryComponentFromCatalogueItem(dataElement, nhsDataDictionaryService.newDataDictionary())
        }
    }

    @Deprecated
    boolean attributeListIncludesName(CatalogueItem catalogueItem, String name) {
        List<String> linkedAttributeList = getLinkedAttributes(catalogueItem)
        if (!linkedAttributeList) {
            return false
        }
        return linkedAttributeList.contains(name)
    }

    List<String> getLinkedAttributes(CatalogueItem catalogueItem) {
        catalogueItem.semanticLinks.collect {link ->
            DataElement.get(link.targetMultiFacetAwareItemId).label
        }
    }


    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".element"
    }

    @Override
    NhsDDElement getNhsDataDictionaryComponentFromCatalogueItem(DataElement catalogueItem, NhsDataDictionary dataDictionary, List<Metadata> metadata = null) {
        NhsDDElement element = new NhsDDElement()
        nhsDataDictionaryComponentFromItem(catalogueItem, element, metadata)
        catalogueItem.semanticLinks.each {
            if(it.linkType == SemanticLinkType.REFINES) {
                NhsDDAttribute linkedAttribute = dataDictionary.attributesByCatalogueId[it.targetMultiFacetAwareItemId]
                if(linkedAttribute) {
                    element.instantiatesAttributes.add(linkedAttribute)
                    linkedAttribute.instantiatedByElements.add(element)
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

    void persistElements(NhsDataDictionary dataDictionary,
                         VersionedFolder dictionaryFolder, DataModel elementsDataModel, String currentUserEmailAddress,
                         Map<String, Terminology> attributeTerminologiesByName) {

        PrimitiveType stringDataType = new PrimitiveType(label: "String", createdBy: currentUserEmailAddress)
        elementsDataModel.addToDataTypes(stringDataType)


        Folder dataElementCodeSetsFolder =
            new Folder(label: "Data Element CodeSets", createdBy: currentUserEmailAddress)
        dictionaryFolder.addToChildFolders(dataElementCodeSetsFolder)
        if (!folderService.validate(dataElementCodeSetsFolder)) {
            throw new ApiInvalidModelException('NHSDD', 'Invalid model', dataElementCodeSetsFolder.errors)
        }
        folderService.save(dataElementCodeSetsFolder)


        DataClass retiredElementsClass = new DataClass(label: "Retired", createdBy: currentUserEmailAddress)
        elementsDataModel.addToDataClasses(retiredElementsClass)


        Map<String, Folder> folders = [:]
        int idx = 0
        // Would sort, but assume already sorted
        dataDictionary.elements.each {name, element ->
            DataType dataType
            if (element.codes.size() > 0 && !element.isRetired()) {
                Folder subFolder = DDHelperFunctions.getSubfolderFromName(folderService, dataElementCodeSetsFolder, name, currentUserEmailAddress)

                CodeSet codeSet = new CodeSet(
                    label: name,
                    folder: subFolder,
                    createdBy: currentUserEmailAddress,
                    authority: authorityService.defaultAuthority,
                    branchName: dataDictionary.branchName)
                codeSet.addToMetadata(new Metadata(namespace: ddCodeSetProfileProviderService.metadataNamespace, key: "version", value: element.codeSetVersion))


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
                elementsDataModel.addToDataTypes(dataType)
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


            element.instantiatesAttributes.each {attribute ->
                if(attribute.catalogueItem) {
                    SemanticLink semanticLink = new SemanticLink(
                            targetMultiFacetAwareItem: attribute.catalogueItem,
                            linkType: SemanticLinkType.REFINES,
                            createdBy: currentUserEmailAddress
                    )
                    elementDataElement.addToSemanticLinks(semanticLink)
                }
            }

            //String elementAttributes = StringUtils.join(element.instantiatesAttributes.collect {it.name}, ";")
            //addToMetadata(elementDataElement, "linkedAttributes", elementAttributes, currentUserEmailAddress)

            DataClass parentClass
            if (element.isRetired()) {
                retiredElementsClass.addToDataElements(elementDataElement)
            } else {
                parentClass = DDHelperFunctions.getChildClassFromName(elementsDataModel, name, currentUserEmailAddress)
                parentClass.addToDataElements(elementDataElement)
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
