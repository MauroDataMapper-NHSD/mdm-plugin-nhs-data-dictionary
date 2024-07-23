package uk.nhs.digital.maurodatamapper.datadictionary.utils

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass

import groovy.transform.Sortable
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClassRelationship

@Sortable(includes = 'relationship')
class StereotypedClassRelationship {
    String key
    String relationship
    DataClass targetClass
    String stereotype

    StereotypedClassRelationship(NhsDDClassRelationship classRelationship) {
        key = classRelationship.isKey ? "Key" : ""
        relationship = classRelationship.relationshipDescription
        targetClass = classRelationship.targetClass.catalogueItem
        stereotype = classRelationship.targetClass.stereotypeForPreview
    }
}
