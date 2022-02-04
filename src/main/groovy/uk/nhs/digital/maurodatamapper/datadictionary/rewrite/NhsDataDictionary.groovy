package uk.nhs.digital.maurodatamapper.datadictionary.rewrite


import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
@Transactional
class NhsDataDictionary {


    static final String METADATA_NAMESPACE = "uk.nhs.datadictionary"

    static final String FOLDER_NAME = "NHS Data Dictionary"
    static final String CORE_MODEL_NAME = "NHS Data Dictionary - Core"
    static final String ATTRIBUTES_CLASS_NAME = "Attributes"
    static final String DATA_FIELD_NOTES_CLASS_NAME = "Data Field Notes"
    static final String DATA_CLASSES_CLASS_NAME = "Classes"
    static final String DATA_SETS_FOLDER_NAME = "Data Sets"
    static final String BUSINESS_DEFINITIONS_TERMINOLOGY_NAME = "NHS Business Definitions"
    static final String SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME = "Supporting Information"
    static final String XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME = "XML Schema Constraints"
    static final String WEB_PAGES_TERMINOLOGY_NAME = "Web Pages"

    static Pattern pattern = Pattern.compile("<a[\\s]*(?:uin=\"[^\"]*\")?[\\s]*href=\"([^\"]*)\"[\\s]*(?:uin=\"[^\"]*\")?>([^<]*)</a>")


    Map<String, NhsDDAttribute> attributes = [:]
    Map<String, NhsDDElement> elements = [:]
    Map<String, NhsDDClass> classes = [:]
    Map<String, NhsDDDataSet> dataSets = [:]
    Map<String, NhsDDBusinessDefinition> businessDefinitions = [:]
    Map<String, NhsDDSupportingInformation> supportingInformation = [:]
    Map<String, NhsDDXMLSchemaConstraint> xmlSchemaConstraints = [:]
    Map<String, NhsDDWebPage> webPages = [:]

    Map<String, NhsDDAttribute> attributesByUin = [:]
    Map<String, NhsDDClass> classesByUin = [:]
    Map<String, NhsDDWebPage> webPagesByUin = [:]

    // TODO:  Need to refactor this out sometime
    Map<String, DataElement> elementsByUrl = [:]

    Map<UUID, NhsDDCode> codesByCatalogueId = [:]

    String folderName = FOLDER_NAME

    Map<String, Map<String, NhsDataDictionaryComponent>> componentClasses = [
        "NhsDDAttribute": attributes,
        "NhsDDElement": elements,
        "NhsDDClass": classes,
        "NhsDDWebPage": webPages,
        "NhsDDDataSet": dataSets,
        "NhsDDBusinessDefinition": businessDefinitions,
        "NhsDDSupportingInformation": supportingInformation,
        "NhsDDXMLSchemaConstraint": xmlSchemaConstraints,
    ]

    List<NhsDataDictionaryComponent> getAllComponents() {
        attributes.values() +
            elements.values() +
            classes.values() +
            dataSets.values() +
            businessDefinitions.values() +
            supportingInformation.values() +
            xmlSchemaConstraints.values() +
            webPages.values()
    }

    static NhsDataDictionary buildFromXml(def xml, String releaseDate, String folderVersionNo = null) {
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
                xml[dummyComponent.getXmlNodeName()].sort {it.name.text()}.each { node ->
                    NhsDataDictionaryComponent component = (NhsDataDictionaryComponent) Class.forName(componentNameWithPackage).getConstructor()
                                                                                                          .newInstance()
                    if(component.isValidXmlNode(node)) {
                        component.fromXml(node, it)
                        component.calculateShortDescription()
                        NhsDataDictionaryComponent existingItem = map[component.name]
                        if(!existingItem || existingItem.isRetired()) {
                            map[component.name] = component
                        }
                    }
                }
            }
            processClassLinks()
            processLinksFromXml()
            long endTime = System.currentTimeMillis()
            log.info("Data Dictionary build from XML complete in ${Utils.getTimeString(endTime - startTime)}")
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
            String newDefinition = component.definition
            if(component.definition) {
                Matcher matcher = pattern.matcher(newDefinition)
                while(matcher.find()) {
                    String matchedUrl = replacements[matcher.group(1)]
                    if(matchedUrl) {
                        String replacement = "<a href=\"${matchedUrl}\">${matcher.group(2).replaceAll("_"," ")}</a>"
                        newDefinition = newDefinition.replace(matcher.group(0), replacement)
                        log.trace("Replacing: " + matcher.group(0) + " with " + replacement)
                    } else {
                        List<String> existingUnmatched = unmatchedUrls[component.name]
                        if(existingUnmatched) {
                            existingUnmatched.add(matcher.group(0))
                        } else {
                            unmatchedUrls[component.name] = [matcher.group(0)]
                        }
                    }

                }
            }
            component.definition = newDefinition
        }
        log.warn("Unmatched Urls: {}", unmatchedUrls.size())
    }


    static Set<String> getAllMetadataKeys() {
        Set<String> allKeys = []
        allKeys.addAll(METADATA_FIELD_MAPPING.keySet())

        // These fields are not part of the original ingest from xml but calculated subsequently.
        // Another approach would be to enumerate all the profile definitions from this plugin and list all their keys
        allKeys.addAll(["shortDescription","overview","linkedAttributes","noAliasesRequired","approvingOrganisation"])
        return allKeys
    }

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
