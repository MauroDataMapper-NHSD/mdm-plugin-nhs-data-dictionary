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
package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishOptions

import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
@Transactional
class NhsDataDictionary {


    static final String WEBSITE_URL = "https://www.datadictionary.nhs.uk"

    static final String METADATA_NAMESPACE = "uk.nhs.datadictionary"
    static final String DEFAULT_PROFILE_NAMESPACE = "default.profile"

    static final String FOLDER_NAME = "NHS Data Dictionary"
    static final String CORE_MODEL_NAME = "NHS Data Dictionary - Core"
    static final String ATTRIBUTES_CLASS_NAME = "Attributes"
    static final String DATA_ELEMENTS_CLASS_NAME = "Data Elements"
    static final String DATA_CLASSES_CLASS_NAME = "Classes"
    static final String DATA_SETS_FOLDER_NAME = "Data Sets"
    static final String BUSINESS_DEFINITIONS_TERMINOLOGY_NAME = "NHS Business Definitions"
    static final String SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME = "Supporting Information"
    static final String DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME = "Data Set Constraints"
    static final String WEB_PAGES_TERMINOLOGY_NAME = "Web Pages"

    static Pattern pattern = Pattern.compile("<a[\\s]*(?:uin=\"[^\"]*\")?[\\s]*href=\"([^\"]*)\"[\\s]*(?:uin=\"[^\"]*\")?>([^<]*)</a>")

    String retiredItemText = ""
    String preparatoryItemText = ""

    Map<String, String> workItemDetails = [:]


    Map<String, NhsDDAttribute> attributes = [:]
    Map<String, NhsDDElement> elements = [:]
    Map<String, NhsDDClass> classes = [:]
    Map<String, NhsDDDataSet> dataSets = [:]
    Map<List<String>, NhsDDDataSetFolder> dataSetFolders = [:]
    Map<String, NhsDDBusinessDefinition> businessDefinitions = [:]
    Map<String, NhsDDSupportingInformation> supportingInformation = [:]
    Map<String, NhsDDDataSetConstraint> dataSetConstraints = [:]
    Map<String, NhsDDWebPage> webPages = [:]

    Map<String, NhsDDAttribute> attributesByUin = [:]
    Map<String, NhsDDClass> classesByUin = [:]
    Map<String, NhsDDWebPage> webPagesByUin = [:]

    // TODO:  Need to refactor this out sometime
    Map<String, DataElement> elementsByUrl = [:]

    Map<UUID, NhsDDCode> codesByCatalogueId = [:]
    Map<UUID, NhsDDClass> classesByCatalogueId = [:]
    Map<UUID, NhsDDElement> elementsByCatalogueId = [:]
    Map<UUID, NhsDDAttribute> attributesByCatalogueId = [:]

    String folderName = FOLDER_NAME

    String branchName = "main"

    VersionedFolder containingVersionedFolder

    Map<String, Map<String, NhsDataDictionaryComponent>> componentClasses = [
        "NhsDDAttribute": attributes,
        "NhsDDElement": elements,
        "NhsDDClass": classes,
        "NhsDDWebPage": webPages,
        "NhsDDBusinessDefinition": businessDefinitions,
        "NhsDDSupportingInformation": supportingInformation,
        "NhsDDDataSet": dataSets,
        "NhsDDDataSetConstraint": dataSetConstraints,
    ]

    List<NhsDataDictionaryComponent> getAllComponents() {
        (attributes.values() +
            elements.values() +
            classes.values() +
            dataSets.values() +
            businessDefinitions.values() +
            supportingInformation.values() +
            dataSetConstraints.values() +
            webPages.values() +
            dataSetFolders.values()) as List
    }

    Map<String, List<NhsDataDictionaryComponent>> allComponentsByIndex(boolean includeRetired = false) {
        componentsByIndex(getAllComponents(), includeRetired)
    }

    Map<String, List<NhsDataDictionaryComponent>> componentsByIndex(Collection<NhsDataDictionaryComponent> components,
                                                                    boolean includeRetired = false) {

        Map<String, List<NhsDataDictionaryComponent>> indexMap = [:]

        List<String> alphabet = ['0-9']
        alphabet.addAll('a'..'z')

        alphabet.each {alphIndex ->
            List<NhsDataDictionaryComponent> componentsByIndex = components.findAll {component ->
                (includeRetired || !component.isRetired()) &&
                ((alphIndex == '0-9' && Character.isDigit(component.name.charAt(0))) ||
                 component.name.toLowerCase().startsWith(alphIndex))
            }.sort {it.name}

            if(componentsByIndex.size() > 0) {
                indexMap[alphIndex.toUpperCase()] = componentsByIndex
            }
        }

        indexMap
    }

    Map<String, NhsDataDictionaryComponent> internalLinks = [:]

    void buildInternalLinks() {
        allComponents.each {eachComponent ->
            internalLinks[eachComponent.getMauroPath()] = eachComponent
        }
    }


    static NhsDataDictionary buildFromXml(def xml, String releaseDate, String folderVersionNo = null, PublishOptions publishOptions) {
        return new NhsDataDictionary().tap {
            log.info('Building new NHS Data Dictionary from XML')
            long startTime = System.currentTimeMillis()
            if (releaseDate) {
                folderName = "${FOLDER_NAME} (${releaseDate})"
            }

            componentClasses.each {componentClassName, map ->
                String componentNameWithPackage = "${NhsDataDictionary.packageName}.${componentClassName}"
                NhsDataDictionaryComponent dummyComponent = (NhsDataDictionaryComponent) Class.forName(componentNameWithPackage).getConstructor()
                    .newInstance()
                log.info("Building ${componentClassName}...")
                long componentStartTime = System.currentTimeMillis()
                if(publishOptions.isPublishableComponent(dummyComponent)) {
                    xml[dummyComponent.getXmlNodeName()] /*.sort {it.name.text()} */.each {node ->
                        NhsDataDictionaryComponent component = (NhsDataDictionaryComponent) Class.forName(componentNameWithPackage).getConstructor()
                            .newInstance()
                        if (component.isValidXmlNode(node)) {
                            component.fromXml(node, it)
                            NhsDataDictionaryComponent existingItem = map[component.name]
                            if (!existingItem || existingItem.isRetired()) {
                                map[component.name] = component
                            }
                        }
                    }
                }
                log.info("${componentClassName} built in ${Utils.getTimeString(System.currentTimeMillis() - componentStartTime)}")
            }
            long componentStartTime = System.currentTimeMillis()
            if(publishOptions.publishDataSetFolders) {
                processDataSetFolders()
            }
            log.info("Data Set Folders built in ${Utils.getTimeString(System.currentTimeMillis() - componentStartTime)}")
            componentStartTime = System.currentTimeMillis()
            if(publishOptions.publishClasses) {
                processClassLinks()
            }
            log.info("Class Links built in ${Utils.getTimeString(System.currentTimeMillis() - componentStartTime)}")
            componentStartTime = System.currentTimeMillis()
            processLinksFromXml()
            log.info("Links processed in ${Utils.getTimeString(System.currentTimeMillis() - componentStartTime)}")

            componentStartTime = System.currentTimeMillis()
            allComponents.each { component ->
                component.setShortDescription()
            }
            log.info("Short descriptions calculated in ${Utils.getTimeString(System.currentTimeMillis() - componentStartTime)}")

            long endTime = System.currentTimeMillis()
            log.info("Data Dictionary build from XML complete in ${Utils.getTimeString(endTime - startTime)}")
        }
    }

    Map<String, String> introductionPageMap = [
            //"PLICS": "PLICS Data Set Overview",
            "PLICS Data Set": "Patient Level Information Costing System Integrated Data Set Introduction",
            "EPMA": "Electronic Prescribing and Medicines Administration Data Sets Introduction",
            "COSDS": "Cancer Outcomes and Services Data Set Introduction"
            //"Commissioning Data Sets": "Commissioning Data Sets (CDS) Introduction"
    ]

    void processDataSetFolders() {
        Set<List<String>> paths = []
        dataSets.values().each {dataSet ->
            List<String> newWebPath = []
            newWebPath.addAll(dataSet.getWebPath())
            newWebPath.removeLast()
            paths.add(newWebPath)
        }
        paths.add(["Commissioning Data Sets"])
        paths.add(["Retired"])
        paths.each {path ->

            NhsDDDataSetFolder dataSetFolder = new NhsDDDataSetFolder()
            dataSetFolder.name = path.last()
            dataSetFolder.folderPath.addAll(path)
            if(path.contains("Retired")) {
                dataSetFolder.otherProperties["isRetired"] = "true"
            } else {
                dataSetFolder.otherProperties["isRetired"] = "false"
            }

            String introductionPageName = introductionPageMap[dataSetFolder.name]
            if(!introductionPageName) {
                introductionPageName = dataSetFolder.name + " Introduction"
            }
            NhsDataDictionaryComponent introductoryWebPage = webPages[introductionPageName]?:supportingInformation[introductionPageName]
            if(!introductoryWebPage && !dataSetFolder.isRetired()) {
                log.error("Cannot find introductory web page: ${dataSetFolder.name}")
            } else {
                if(introductoryWebPage && !dataSetFolder.isRetired()) {
                    log.info("Introduction page found: " + introductoryWebPage.name)
                    dataSetFolder.otherProperties = introductoryWebPage.otherProperties
                    dataSetFolder.definition = introductoryWebPage.definition
                }
            }
            dataSetFolders[path] = dataSetFolder
        }

    }

    void processClassLinks() {
        classes.each {name, clazz ->
            clazz.classLinks.each {classLink ->
                classLink.supplierClass = classesByUin[classLink.supplierUin]
                if(!classLink.supplierClass) {
                    log.error("Cannot match supplier class relationship: {} :: SupplierUin: {}\n{}", name, classLink.supplierUin, classLink.toString())
                }
                classLink.clientClass = classesByUin[classLink.clientUin]
                if(!classLink.clientClass) {
                    log.error("Cannot match client class relationship: {} ClientUin: {}\n{}", name, classLink.clientUin, classLink.toString())
                }
            }
        }
    }

    void processLinksFromXml() {
        Map<String, String> replacements = [:]
        getAllComponents().each {component ->
            replacements.putAll(component.getUrlReplacements())
        }
        Map<String, List<String>> unmatchedUrls = [:]
        getAllComponents().each {component ->
            component.definition = replaceUrls(component.definition, replacements, unmatchedUrls, component.name)
            if(component instanceof NhsDDElement) {

                ((NhsDDElement)component).codes.each {code ->
                    if(code.webPresentation) {
                        code.webPresentation = replaceUrls(code.webPresentation, replacements, unmatchedUrls, component.name)
                    }
                }
                String formatLink = ((NhsDDElement)component).otherProperties["formatLink"]
                if(formatLink && replacements[formatLink]) {
                    ((NhsDDElement)component).otherProperties["formatLink"] = replacements[formatLink]
                }
                if(component.otherProperties["attributeText"] && component.otherProperties["attributeText"] != "") {
                    component.otherProperties["attributeText"] = replaceUrls(component.otherProperties["attributeText"], replacements, unmatchedUrls, component.name)
                }

            }
            if(component instanceof NhsDDAttribute) {
                ((NhsDDAttribute)component).codes.each {code ->
                    code.webPresentation = replaceUrls(code.webPresentation, replacements, unmatchedUrls, component.name)
                }
            }
        }
        log.warn("Unmatched Urls: {}", unmatchedUrls.size())
    }

    static String replaceUrls(String input, Map<String, String> replacements, Map<String, List<String>> unmatchedUrls, String componentName) {
        if(input) {
            Matcher matcher = pattern.matcher(input)
            while(matcher.find()) {
                String matchedUrl = replacements[matcher.group(1)]
                if(matchedUrl) {
                    String replacement = "<a href=\"${matchedUrl}\">${matcher.group(2).replaceAll("_"," ")}</a>"
                    input = input.replace(matcher.group(0), replacement)
                    log.trace("Replacing: " + matcher.group(0) + " with " + replacement)
                } else {
                    List<String> existingUnmatched = unmatchedUrls[componentName]
                    if(existingUnmatched) {
                        existingUnmatched.add(matcher.group(0))
                    } else {
                        unmatchedUrls[componentName] = [matcher.group(0)]
                    }
                }

            }
        }
        return input
    }


    static Set<String> getAllMetadataKeys() {
        Set<String> allKeys = []
        allKeys.addAll(METADATA_FIELD_MAPPING.keySet())

        // These fields are not part of the original ingest from xml but calculated subsequently.
        // Another approach would be to enumerate all the profile definitions from this plugin and list all their keys
        allKeys.addAll(["shortDescription","overview","linkedAttributes","noAliasesRequired","approvingOrganisation", "attributeText"])
        allKeys.removeAll(KEYS_FOR_INGEST_ONLY)
        return allKeys
    }

    static Map<String, String> aliasFields = [
        "aliasAlsoKnownAs": "Also known as",
        "aliasFormerly": "Formerly",
        "aliasFullName": "Full name",
        "aliasIndexName": "Index name",
        "aliasODSXMLSchema": "ODS XML Schema",
        "aliasOid": "Object Identifier",
        "aliasPlural": "Plural",
        "aliasPossessive": "Possessive",
        "aliasReportHeading": "Report heading",
        "aliasSchema": "Schema",
        "aliasShortName": "Short name",
        "aliasSnomedCTRefsetId": "SNOMED CT Refset Id",
        "aliasSnomedCTRefsetName": "SNOMED CT Refset Name"
    ]

    static List<String> KEYS_FOR_INGEST_ONLY = [
            "uin",
            "baseUri",
            "ultimatePath",
            "ddUrl",
            "search",
            "baseVersion"
    ]

    static final Map<String, String> METADATA_FIELD_MAPPING = [
        // Aliases
        "aliasShortName": "aliasShortName",
        "aliasAlsoKnownAs": "aliasAlsoKnownAs",
        "aliasPlural": "aliasPlural",
        "aliasFormerly": "aliasFormerly",
        "aliasFullName": "aliasFullName",
        "aliasIndexName": "aliasIndexName",
        "aliasSchema": "aliasSchema",
        "name": "name",
        "titleCaseName": "TitleCaseName",
        "websitePageHeading": "websitePageHeading",

        // SNOMED
        "aliasSnomedCTRefsetId": "aliasSnomedCTRefsetId",
        "aliasSnomedCTRefsetName": "aliasSnomedCTRefsetName",
        "aliasSnomedCTSubsetName": "aliasSnomedCTSubsetName",
        "aliasSnomedCTSubsetOriginalId": "aliasSnomedCTSubsetOriginalId",

        // Publication Lifecycle
        "isPreparatory": "isPrepatory",
        "isRetired": "isRetired",
        "retiredDate": "retiredDate",

        // Legacy
        "uin": "uin",
        "baseUri": "base-uri",
        "ultimatePath": "ultimate-path",
        "ddUrl": "DD_URL",
        "search": "search",
        "baseVersion": "baseVersion",
        // "modFinal": "mod__final",
        // "modAbstract": "mod__abstract",

        // Encoding
        "formatLength": "format-length",
        "formatLink": "format-link",


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
        "navigationParent": "navigationParent",
        "explanatoryPage": "explanatoryPage",
        "class": "class",
        "doNotShowChangeLog": "doNotShowChangeLog",
        "node": "node",
        "referencedElement": "referencedElement",
        "participant": "participant",
        "supportingFile": "supportingFile",
        "isNavigationComponent": "isNavigationComponent",
        "isWebsiteIndex": "isWebsiteIndex",

        // Not sure about this one...
        // "extraFieldHead": "ExtraFieldhead",

        // do I need to parse these for history?
        //"defaultCode": "DefaultCode",
        //"nationalCodes": "national-codes",
        //"defaultCodes": "default-codes",
        //"valueSet": "value-set",
    ]

}
