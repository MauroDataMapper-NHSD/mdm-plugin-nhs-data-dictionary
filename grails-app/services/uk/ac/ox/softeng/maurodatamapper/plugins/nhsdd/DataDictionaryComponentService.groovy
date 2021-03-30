package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService

import grails.gorm.transactions.Transactional
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionaryComponent

import java.util.regex.Matcher
import java.util.regex.Pattern

@Transactional
abstract class DataDictionaryComponentService<T extends CatalogueItem> {

    DataModelService dataModelService
    DataClassService dataClassService
    DataElementService dataElementService
    TerminologyService terminologyService
    TermService termService
    FolderService folderService

    NhsDataDictionaryService nhsDataDictionaryService

    abstract Map indexMap(T object)

    List<Map> index() {
        getAll().sort{it.label}.collect { indexMap(it)}
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

    String getShortDescription(T catalogueItem) {
        return "Short description has not been implemented for this type yet"
    }

    boolean isPreparatory(T item) {
        return item.metadata.find { md ->
            md.namespace == getMetadataNamespace() &&
                    md.key == "isPreparatory" &&
                    md.value == "true"
        }
    }

    def getWhereUsed(DataDictionary dataDictionary, String id) {
        DataDictionaryComponent component = dataDictionary.getAllComponents().find{
            it.catalogueId.toString().equalsIgnoreCase(id)
        }
        List<Map> whereUsed = []
        String itemLink = component.getInternalLink()
        System.err.println("itemLink: " + itemLink)
        System.err.println("description: " + component.definition.toString())
        dataDictionary.getAllComponents().collect{eachComponent ->
            if(eachComponent.definition.toString().contains(itemLink)) {
                whereUsed.add([
                        type: eachComponent.typeText,
                        name: eachComponent.name,
                        stereotype: eachComponent.outputClassForLink,
                        componentId: eachComponent.catalogueId.toString(),
                        text: "references in description ${component.name}"
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
        return newDescription
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
        if(path[0] == "te:${DataDictionary.BUSDEF_TERMINOLOGY_NAME}") {
            Terminology terminology = terminologyService.findByLabel(DataDictionary.BUSDEF_TERMINOLOGY_NAME)
            String termLabel = path[1].replace("tm:", "")
            Term t = terminology.findTermByCode(termLabel)
            if(t) {
                return t
            }
        }
        if(path[0] == "te:${DataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}") {
            Terminology terminology = terminologyService.findByLabel(DataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME)
            String termLabel = path[1].replace("tm:", "")
            Term t = terminology.findTermByCode(termLabel)
            if(t) {
                return t
            }
        }
        if(path[0] == "te:${DataDictionary.XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME}") {
            Terminology terminology = terminologyService.findByLabel(DataDictionary.XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME)
            String termLabel = path[1].replace("tm:", "")
            Term t = terminology.findTermByCode(termLabel)
            if(t) {
                return t
            }
        }
        if(path[0].startsWith("dm:${DataDictionary.DATA_DICTIONARY_CORE_MODEL_NAME}")) {
            DataModel dm = dataModelService.findByLabel(DataDictionary.DATA_DICTIONARY_CORE_MODEL_NAME)
            if(path[1] == "dc:${DataDictionary.DATA_DICTIONARY_DATA_CLASSES_CLASS_NAME}") {
                DataClass dc1 = dataClassService.findByParentAndLabel(dm, DataDictionary.DATA_DICTIONARY_DATA_CLASSES_CLASS_NAME)
                if(path[2] == "dc:Retired") {
                    DataClass dc2 = dataClassService.findByParentAndLabel(dc1, "Retired")
                    DataClass dc3 = dataClassService.findByParentAndLabel(dc2, path[3].replace("dc:", ""))
                    return dc3
                } else {
                    DataClass dc2 = dataClassService.findByParentAndLabel(dc1, path[2].replace("dc:", ""))
                    return dc2
                }
            }
            if(path[1] == "dc:${DataDictionary.DATA_DICTIONARY_ATTRIBUTES_CLASS_NAME}") {
                DataClass dc1 = dataClassService.findByParentAndLabel(dm, DataDictionary.DATA_DICTIONARY_ATTRIBUTES_CLASS_NAME)
                if(path[2] == "dc:Retired") {
                    DataClass dc2 = dataClassService.findByParentAndLabel(dc1, "Retired")
                    DataElement de3 = dataElementService.findByParentAndLabel(dc2, path[3].replace("de:", ""))
                    return de3
                } else {
                    DataElement de2 = dataElementService.findByParentAndLabel(dc1, path[2].replace("de:", ""))
                    return de2
                }
            }
            if(path[1] == "dc:${DataDictionary.DATA_DICTIONARY_DATA_FIELD_NOTES_CLASS_NAME}") {
                DataClass dc1 = dataClassService.findByParentAndLabel(dm, DataDictionary.DATA_DICTIONARY_DATA_FIELD_NOTES_CLASS_NAME)
                if(path[2] == "dc:Retired") {
                    DataClass dc2 = dataClassService.findByParentAndLabel(dc1, "Retired")
                    DataElement de3 = dataElementService.findByParentAndLabel(dc2, path[3].replace("de:", ""))
                    return de3
                } else {
                    DataElement de2 = dataElementService.findByParentAndLabel(dc1, path[2].replace("de:", ""))
                    return de2
                }
            }
        }
        if(path.length == 1 && path[0].startsWith("dm:")) {
            DataModel dm = dataModelService.findByLabel(path[0].replace("dm:", ""))
            return dm
        }
        System.err.println("Cannot find by path!")
        System.err.println(path[1])

        return null
    }

    String getStereotypeByPath(String[] path) {
        if(path[0] == "te:${DataDictionary.BUSDEF_TERMINOLOGY_NAME}") {
            return "businessDefinition"
        }
        if(path[0] == "te:${DataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}") {
            return "supportingInformation"
        }
        if(path[0] == "te:${DataDictionary.XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME}") {
            return "xmlSchemaConstraint"
        }
        if(path[0] == "dm:${DataDictionary.DATA_DICTIONARY_CORE_MODEL_NAME}") {
            if(path[1] == "dc:${DataDictionary.DATA_DICTIONARY_DATA_CLASSES_CLASS_NAME}") {
                return "class"
            }
            if(path[1] == "dc:${DataDictionary.DATA_DICTIONARY_ATTRIBUTES_CLASS_NAME}") {
                return "attribute"
            }
            if(path[1] == "dc:${DataDictionary.DATA_DICTIONARY_DATA_FIELD_NOTES_CLASS_NAME}") {
                return "element"
            }
        }
        if(path.length == 1 && path[0].startsWith("dm:")) {
            return "dataSet"
        }
        return null
    }


}
