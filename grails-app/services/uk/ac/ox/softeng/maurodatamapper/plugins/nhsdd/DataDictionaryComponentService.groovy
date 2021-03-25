package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService

import grails.gorm.transactions.Transactional
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionaryComponent

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

    abstract def show(String id)

    abstract Set<T> getAll()

    abstract T getItem(UUID id)

    Map<String, String> getAliases(T catalogueItem) {
        Map<String, String> aliases = [:]
        catalogueItem.metadata.each {
            System.err.println(it)
        }
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

}
