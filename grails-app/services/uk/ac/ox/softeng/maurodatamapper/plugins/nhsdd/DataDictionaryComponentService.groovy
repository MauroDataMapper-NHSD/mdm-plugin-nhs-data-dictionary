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

import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDCode
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.utils.StereotypedCatalogueItem

import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
@Transactional
abstract class DataDictionaryComponentService<T extends CatalogueItem, D extends NhsDataDictionaryComponent> {

    DataModelService dataModelService
    DataClassService dataClassService
    DataElementService dataElementService
    TerminologyService terminologyService
    TermService termService
    CodeSetService codeSetService
    FolderService folderService
    MetadataService metadataService
    AuthorityService authorityService

    NhsDataDictionaryService nhsDataDictionaryService

    @Autowired
    MessageSource messageSource

    List<T> index(UUID versionedFolderId, boolean includeRetired = false) {
        (getAll(versionedFolderId, includeRetired) as List).sort {it.label}
    }

    abstract def show(UUID versionedFolderId, String id)

    abstract Set<T> getAll(UUID versionedFolderId, boolean includeRetired = false)

    Map<String, String> getAliases(T catalogueItem) {
        Map<String, String> aliases = [:]
        catalogueItem.aliases
        NhsDataDictionary.aliasFields.each {aliasField ->
            Metadata foundMd = catalogueItem.metadata.find {it.namespace == getMetadataNamespace() && it.key == aliasField.key}
            if (foundMd) {
                aliases[aliasField.value] = foundMd.value
            }
        }
        return aliases
    }

    abstract String getMetadataNamespace()

    abstract D getNhsDataDictionaryComponentFromCatalogueItem(T catalogueItem, NhsDataDictionary dataDictionary)

    abstract D getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary)

    List<StereotypedCatalogueItem> getWhereUsed(UUID versionedFolderId, String id) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(versionedFolderId)
        NhsDataDictionaryComponent component = getByCatalogueItemId(UUID.fromString(id), dataDictionary)
        String itemLink = component.getMauroPath()
        List<NhsDataDictionaryComponent> whereUsedComponents = dataDictionary.getAllComponents().
            findAll {it.definition.contains(itemLink)}
        return whereUsedComponents.
            sort { it.name}.
            collect {new StereotypedCatalogueItem(it)}
    }

    //static Pattern pattern = Pattern.compile("\\[([^\\]]*)\\]\\(([^\\)]*)\\)")
    static Pattern pattern = Pattern.compile("<a[\\s]*href=\"([^\"]*)\"[\\s]*>([^<]*)</a>")


    String convertLinksInDescription(UUID branchId, String description) {

        String newDescription = description
        log.debug(newDescription)
        Matcher matcher = pattern.matcher(description)
        while (matcher.find()) {
            if(matcher.group(1).startsWith('http') ||
               matcher.group(1).startsWith('mailto')
            ) {
                // ignore
            } else {
                try {
                    String[] path = matcher.group(1).split("\\|")
                    CatalogueItem foundCatalogueItem = getByPath(path)
                    if (foundCatalogueItem) {
                        String stereotype = getStereotypeByPath(path)
                        String catalogueId = foundCatalogueItem.id.toString()
                        String cssClass = stereotype
                        String replacementLink = """<a class="${cssClass}" href="#/preview/${branchId.toString()}/${stereotype}/${catalogueId}">${
                            matcher
                                .group(2)}</a>"""
                        newDescription = newDescription.replace(matcher.group(0), replacementLink)
                    }
                } catch(Exception e) {
                    log.debug(e.message)
                    log.debug(description)
                }

            }
        }
        return newDescription?.trim() ?: null
    }

    String replaceLinksInShortDescription(String input) {
        String output = input
        Matcher matcher = pattern.matcher(input)
        while (matcher.find()) {
            output = output.replace(matcher.group(0), matcher.group(1))
        }
        return output
    }

    CatalogueItem getByPath(String[] path) {
        if (path[0] == "te:${NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME}") {
            Terminology terminology = terminologyService.findByLabel(NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME)
            String termLabel = path[1].replace("tm:", "")
            Term t = terminology.findTermByCode(termLabel)
            if (t) {
                return t
            }
        }
        if (path[0] == "te:${NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}") {
            Terminology terminology = terminologyService.findByLabel(NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME)
            String termLabel = path[1].replace("tm:", "")
            Term t = terminology.findTermByCode(termLabel)
            if (t) {
                return t
            }
        }
        if (path[0] == "te:${NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME}") {
            Terminology terminology = terminologyService.findByLabel(NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME)
            String termLabel = path[1].replace("tm:", "")
            Term t = terminology.findTermByCode(termLabel)
            if (t) {
                return t
            }
        }
        if (path[0].startsWith("dm:${NhsDataDictionary.CORE_MODEL_NAME}")) {
            DataModel dm = dataModelService.findByLabel(NhsDataDictionary.CORE_MODEL_NAME)
            if (path[1] == "dc:${NhsDataDictionary.DATA_CLASSES_CLASS_NAME}") {
                DataClass dc1 = dataClassService.findByParentAndLabel(dm, NhsDataDictionary.DATA_CLASSES_CLASS_NAME)
                if (path[2] == "dc:Retired") {
                    DataClass dc2 = dataClassService.findByParentAndLabel(dc1, "Retired")
                    DataClass dc3 = dataClassService.findByParentAndLabel(dc2, path[3].replace("dc:", ""))
                    return dc3
                } else {
                    DataClass dc2 = dataClassService.findByParentAndLabel(dc1, path[2].replace("dc:", ""))
                    return dc2
                }
            }
            if (path[1] == "dc:${NhsDataDictionary.ATTRIBUTES_CLASS_NAME}") {
                DataClass dc1 = dataClassService.findByParentAndLabel(dm, NhsDataDictionary.ATTRIBUTES_CLASS_NAME)
                if (path[2] == "dc:Retired") {
                    DataClass dc2 = dataClassService.findByParentAndLabel(dc1, "Retired")
                    DataElement de3 = dataElementService.findByParentAndLabel(dc2, path[3].replace("de:", ""))
                    return de3
                } else {
                    DataElement de2 = dataElementService.findByParentAndLabel(dc1, path[2].replace("de:", ""))
                    return de2
                }
            }
            if (path[1] == "dc:${NhsDataDictionary.DATA_ELEMENTS_CLASS_NAME}") {
                DataClass dc1 = dataClassService.findByParentAndLabel(dm, NhsDataDictionary.DATA_ELEMENTS_CLASS_NAME)
                if (path[2] == "dc:Retired") {
                    DataClass dc2 = dataClassService.findByParentAndLabel(dc1, "Retired")
                    DataElement de3 = dataElementService.findByParentAndLabel(dc2, path[3].replace("de:", ""))
                    return de3
                } else {
                    DataElement de2 = dataElementService.findByParentAndLabel(dc1, path[2].replace("de:", ""))
                    return de2
                }
            }
        }
        if (path.length == 1 && path[0].startsWith("dm:")) {
            DataModel dm = dataModelService.findByLabel(path[0].replace("dm:", ""))
            return dm
        }
        log.debug("Cannot find by path!")
        log.debug(path.toString())
        log.debug(path[1])

        return null
    }

    String getStereotypeByPath(String[] path) {
        if (path[0] == "te:${NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME}") {
            return "businessDefinition"
        }
        if (path[0] == "te:${NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}") {
            return "supportingInformation"
        }
        if (path[0] == "te:${NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME}") {
            return "dataSetConstraint"
        }
        if (path[0] == "dm:${NhsDataDictionary.CORE_MODEL_NAME}") {
            if (path[1] == "dc:${NhsDataDictionary.DATA_CLASSES_CLASS_NAME}") {
                return "class"
            }
            if (path[1] == "dc:${NhsDataDictionary.ATTRIBUTES_CLASS_NAME}") {
                return "attribute"
            }
            if (path[1] == "dc:${NhsDataDictionary.DATA_ELEMENTS_CLASS_NAME}") {
                return "element"
            }
        }
        if (path.length == 1 && path[0].startsWith("dm:")) {
            return "dataSet"
        }
        return null
    }


    Map<String, String> attributeMetadata = [
        // Aliases
        "aliasShortName"               : "aliasShortName",
        "aliasAlsoKnownAs"             : "aliasAlsoKnownAs",
        "aliasPlural"                  : "aliasPlural",
        "aliasFormerly"                : "aliasFormerly",
        "aliasFullName"                : "aliasFullName",
        "aliasIndexName"               : "aliasIndexName",
        "aliasSchema"                  : "aliasSchema",
        "name"                         : "name",
        "titleCaseName"                : "TitleCaseName",
        "websitePageHeading"           : "websitePageHeading",

        // SNOMED
        "aliasSnomedCTRefsetId"        : "aliasSnomedCTRefsetId",
        "aliasSnomedCTRefsetName"      : "aliasSnomedCTRefsetName",
        "aliasSnomedCTSubsetName"      : "aliasSnomedCTSubsetName",
        "aliasSnomedCTSubsetOriginalId": "aliasSnomedCTSubsetOriginalId",

        // Publication Lifecycle
        "isPreparatory"                : "isPrepatory",
        "isRetired"                    : "isRetired",
        "retiredDate"                  : "retiredDate",

        // Legacy
        "uin"                          : "uin",
        "baseUri"                      : "base-uri",
        "ultimatePath"                 : "ultimate-path",
        "ddUrl"                        : "DD_URL",
        "search"                       : "search",
        "baseVersion"                  : "baseVersion",
        // "modFinal": "mod__final",
        // "modAbstract": "mod__abstract",

        // Encoding
        "formatLength"                 : "format-length",
        "formatLink"                   : "format-link",


        //"fhirItem": "FHIR_Item",
        //"definition": "definition",
        //"codeSystem": "code-system",
        //"link": "link",
        //"linkTarget": "link-target",
        //"property": "property",
        //"extends": "extends",
        //"type": "type",
        //"readOnly": "readOnly",
        //"clientRole": "clientRole",
        //"permittedNationalCodes": "permitted-national-codes",
        "navigationParent"             : "navigationParent",
        "explanatoryPage"              : "explanatoryPage",
        "class"                        : "class",
        "doNotShowChangeLog"           : "doNotShowChangeLog",
        "node"                         : "node",
        "referencedElement"            : "referencedElement",
        "participant"                  : "participant",
        "supportingFile"               : "supportingFile",
        "isNavigationComponent"        : "isNavigationComponent",
        "isWebsiteIndex"               : "isWebsiteIndex",

        // Not sure about this one...
        // "extraFieldHead": "ExtraFieldhead",

        // do I need to parse these for history?
        //"defaultCode": "DefaultCode",
        //"nationalCodes": "national-codes",
        //"defaultCodes": "default-codes",
        //"valueSet": "value-set",
    ]

    void addMetadataFromComponent(T domainObject, NhsDataDictionaryComponent component, String currentUserEmailAddress) {
        component.otherProperties.each {key, value ->
            addToMetadata(domainObject, key, value, currentUserEmailAddress)
        }
    }

    void addMetadataFromXml(T domainObject, def xml, String currentUserEmailAddress) {

        attributeMetadata.entrySet().each {entry ->
            String xmlValue = xml[entry.value].text()
            if (xmlValue && xmlValue != "") {
                addToMetadata(domainObject, entry.key, xmlValue, currentUserEmailAddress)
            }
        }
    }

    void addToMetadata(MetadataAware domainObject, String key, String value, String currentUserEmailAddress) {

        if (value) {
            domainObject.addToMetadata(new Metadata(namespace: getMetadataNamespace(),
                                                    key: key,
                                                    value: value,
                                                    createdBy: currentUserEmailAddress))
        }
    }

    void nhsDataDictionaryComponentFromItem(T catalogueItem, NhsDataDictionaryComponent component) {
        component.name = catalogueItem.label
        component.definition = catalogueItem.description
        component.catalogueItem = catalogueItem


        if(catalogueItem instanceof DataClass) {
            component.catalogueItemParentId = ((DataClass)catalogueItem).parentDataClass?.id?.toString()
            component.catalogueItemModelId = ((DataClass)catalogueItem).dataModel.id.toString()
        }
        if(catalogueItem instanceof DataElement) {
            component.catalogueItemParentId = ((DataElement)catalogueItem).dataClass?.id?.toString()
            component.catalogueItemModelId = ((DataElement)catalogueItem).dataClass?.dataModel?.id.toString()
        }
        if(catalogueItem instanceof DataModel) {
            component.catalogueItemModelId = catalogueItem.id.toString()
        }
        if(catalogueItem instanceof Term) {
            component.catalogueItemModelId = ((Term)catalogueItem).terminology.id.toString()
        }


        List<Metadata> allRelevantMetadata = Metadata
            .byMultiFacetAwareItemIdAndNamespace(catalogueItem.id, getMetadataNamespace())
            .inList('key', NhsDataDictionary.getAllMetadataKeys())
            .list()

        NhsDataDictionary.getAllMetadataKeys().each {key ->
            component.otherProperties[key] = allRelevantMetadata.find {it.key == key}?.value
        }
    }

    Map<String, D> collectNhsDataDictionaryComponents(Collection<T> catalogueItems, NhsDataDictionary dataDictionary) {
        catalogueItems.collectEntries {ci ->
            [ci.label, getNhsDataDictionaryComponentFromCatalogueItem(ci, dataDictionary)]
        }
    }

    boolean catalogueItemIsRetired(CatalogueItem catalogueItem) {
        List<Metadata> allRelevantMetadata = Metadata
            .byMultiFacetAwareItemIdAndNamespace(catalogueItem.id, getMetadataNamespace())
            .eq('key', "isRetired")
            .list()

        return allRelevantMetadata.any{md -> md.value == "true"}
    }

    List<NhsDDCode> getCodesForTerms(List<Term> terms, NhsDataDictionary nhsDataDictionary) {
        List<NhsDDCode> codes = []
        List<Metadata> allRelevantMetadata = Metadata
            .byMultiFacetAwareItemIdInList(terms.collect {it.id})
            .inList('key', ['publishDate', 'webOrder', 'webPresentation', 'isDefault'])
            .list()
        codes.addAll(terms.collect {term ->
            NhsDDCode nhsDDCode = nhsDataDictionary.codesByCatalogueId[term.id]
            if(!nhsDDCode) {
                nhsDDCode = new NhsDDCode().tap {
                    code = term.code
                    definition = term.definition
                    publishDate = allRelevantMetadata.find {it.multiFacetAwareItemId == term.id && it.key == 'publishDate'}?.value
                    webOrder = allRelevantMetadata.find {it.multiFacetAwareItemId == term.id && it.key == 'webOrder'}?.value
                    webPresentation = allRelevantMetadata.find {it.multiFacetAwareItemId == term.id && it.key == 'webPresentation'}?.value
                    isDefault = Boolean.valueOf(allRelevantMetadata.find {it.multiFacetAwareItemId == term.id && it.key == 'isDefault'}?.value)
                    catalogueItem = term
                }
                nhsDataDictionary.codesByCatalogueId[term.id] = nhsDDCode
            }
            return nhsDDCode
        })
        return codes
    }

}
