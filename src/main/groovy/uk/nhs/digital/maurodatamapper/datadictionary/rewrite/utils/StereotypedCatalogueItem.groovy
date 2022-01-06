package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.utils

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import groovy.transform.Sortable

/**
 * @since 06/01/2022
 */
@Sortable(includes = 'label')
class StereotypedCatalogueItem {
    CatalogueItem catalogueItem
    String stereotype

    StereotypedCatalogueItem(CatalogueItem catalogueItem, String stereotype) {
        this.catalogueItem = catalogueItem
        this.stereotype = stereotype
    }

    String getId() {
        catalogueItem.id.toString()
    }

    String getLabel() {
        catalogueItem.label
    }
}
