package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.utils

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import groovy.transform.Sortable
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

/**
 * @since 06/01/2022
 */
@Sortable(includes = 'label')
class StereotypedCatalogueItem {
    CatalogueItem catalogueItem
    String label
    String stereotype
    Boolean isRetired

    StereotypedCatalogueItem(CatalogueItem catalogueItem, String stereotype) {
        this.catalogueItem = catalogueItem
        this.stereotype = stereotype
        this.isRetired = catalogueItem.metadata.find {
            it.namespace == NhsDataDictionary.METADATA_NAMESPACE &&
            it.key == "isRetired" &&
            it.value == "true"
        }
        this.label = catalogueItem.label
    }

    StereotypedCatalogueItem(NhsDataDictionaryComponent component) {
        this.catalogueItem = component.catalogueItem
        this.stereotype = component.stereotypeForPreview
        this.isRetired = component.isRetired()
        this.label = component.getNameWithRetired()
    }

    String getId() {
        catalogueItem.id.toString()
    }

}
