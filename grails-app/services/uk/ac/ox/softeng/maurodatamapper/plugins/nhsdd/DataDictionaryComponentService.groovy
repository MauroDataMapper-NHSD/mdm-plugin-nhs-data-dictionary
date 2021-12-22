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
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionaryComponent
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
@Transactional
abstract class DataDictionaryComponentService<T extends CatalogueItem> {

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

    abstract Map indexMap(T object)

    List<Map> index() {
        getAll().sort {it.label}.collect {indexMap(it)}
    }

    abstract def show(String branch, String id)

    abstract Set<T> getAll()

    abstract T getItem(UUID id)

    Map<String, String> getAliases(T catalogueItem) {
        Map<String, String> aliases = [:]
        DataDictionaryComponent.aliasFields.each {aliasField ->

            Metadata foundMd = catalogueItem.metadata.find{it.namespace == getMetadataNamespace() && it.key == aliasField.key}
            if(foundMd) {
                aliases[aliasField.value] = foundMd.value
            }
        }
        return aliases
    }

    abstract String getMetadataNamespace()


    def getWhereUsed(DataDictionary dataDictionary, String id) {
        DataDictionaryComponent component = dataDictionary.getAllComponents().find {
            it.catalogueId.toString().equalsIgnoreCase(id)
        }
        List<Map> whereUsed = []
        String itemLink = component.getInternalLink()
        log.debug("itemLink: " + itemLink)
        log.debug("description: " + component.definition.toString())
        dataDictionary.getAllComponents().collect {eachComponent ->
            if (eachComponent.definition.toString().contains(itemLink)) {
                whereUsed.add([
                    type       : eachComponent.typeText,
                    name       : eachComponent.name,
                    stereotype : eachComponent.outputClassForLink,
                    componentId: eachComponent.catalogueId.toString(),
                    text       : "references in description ${component.name}"
                ])
            }
        }

        return whereUsed.sort {it.name}
    }

    static Pattern pattern = Pattern.compile("\\[([^\\]]*)\\]\\(([^\\)]*)\\)")

    String convertLinksInDescription(String branch, String description) {

        String newDescription = description
        Matcher matcher = pattern.matcher(description)
        while(matcher.find()) {
            String[] path = matcher.group(2).split("\\|")
            CatalogueItem foundCatalogueItem = getByPath(path)
            if(foundCatalogueItem) {
                String stereotype = getStereotypeByPath(path)
                String catalogueId = foundCatalogueItem.id.toString()
                String cssClass = stereotype
                String replacementLink = """<a class="${cssClass}" href="#/preview/${branch}/${stereotype}/${catalogueId}">${matcher.group(1)}</a>"""
                newDescription = newDescription.replace(matcher.group(0), replacementLink)
            }
        }
        return newDescription?.trim() ?: null
    }

    String replaceLinksInShortDescription(String input) {
        String output = input
        Matcher matcher = pattern.matcher(input)
        while(matcher.find()) {
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
        if (path[0] == "te:${NhsDataDictionary.XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME}") {
            Terminology terminology = terminologyService.findByLabel(NhsDataDictionary.XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME)
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
            if (path[1] == "dc:${NhsDataDictionary.DATA_FIELD_NOTES_CLASS_NAME}") {
                DataClass dc1 = dataClassService.findByParentAndLabel(dm, NhsDataDictionary.DATA_FIELD_NOTES_CLASS_NAME)
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
        log.debug(path[1])

        return null
    }

    String getStereotypeByPath(String[] path) {
        if(path[0] == "te:${NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME}") {
            return "businessDefinition"
        }
        if(path[0] == "te:${NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}") {
            return "supportingInformation"
        }
        if(path[0] == "te:${NhsDataDictionary.XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME}") {
            return "xmlSchemaConstraint"
        }
        if(path[0] == "dm:${NhsDataDictionary.CORE_MODEL_NAME}") {
            if(path[1] == "dc:${NhsDataDictionary.DATA_CLASSES_CLASS_NAME}") {
                return "class"
            }
            if(path[1] == "dc:${NhsDataDictionary.ATTRIBUTES_CLASS_NAME}") {
                return "attribute"
            }
            if(path[1] == "dc:${NhsDataDictionary.DATA_FIELD_NOTES_CLASS_NAME}") {
                return "element"
            }
        }
        if(path.length == 1 && path[0].startsWith("dm:")) {
            return "dataSet"
        }
        return null
    }


    Map<String, String> attributeMetadata = [
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
        //"formatLink": "format-link",
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

    void addMetadataFromComponent(T domainObject, NhsDataDictionaryComponent component, String currentUserEmailAddress) {
        component.otherProperties.each {key, value ->
            addToMetadata(domainObject, key, value, currentUserEmailAddress)
        }
    }

    void addMetadataFromXml(T domainObject, def xml, String currentUserEmailAddress) {

        attributeMetadata.entrySet().each {entry ->
            String xmlValue = xml[entry.value].text()
            if(xmlValue && xmlValue != "") {
                addToMetadata(domainObject, entry.key, xmlValue, currentUserEmailAddress)
            }
        }
    }

    void addToMetadata(MetadataAware domainObject, String key, String value, String currentUserEmailAddress) {

        if(value) {
            domainObject.addToMetadata(new Metadata(namespace: getMetadataNamespace(),
                                                    key: key,
                                                    value: value,
                                                    createdBy: currentUserEmailAddress))
        }
    }

    void nhsDataDictionaryComponentFromItem(CatalogueItem catalogueItem, NhsDataDictionaryComponent component) {
        component.name = catalogueItem.label
        component.definition = catalogueItem.description
        component.catalogueItem = catalogueItem
        NhsDataDictionary.METADATA_FIELD_MAPPING.keySet().each {key ->
            Metadata md = catalogueItem.metadata.find {
                it.namespace == getMetadataNamespace() &&
                it.key == key

            }
            if (md) {
                component.otherProperties[key] = md.value
            } else {
                component.otherProperties[key] = null
            }
        }
    }

}
