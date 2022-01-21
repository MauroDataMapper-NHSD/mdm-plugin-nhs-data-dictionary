package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType

import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

class AllClassesHaveRelationships implements IntegrityCheck {

    String name = "Class Relationships Defined"

    String description = "Check that all live classes have relationships to other classes defined"

    @Override
    List<NhsDataDictionaryComponent> runCheck(NhsDataDictionary dataDictionary) {

        errors = dataDictionary.classes.values().findAll{ddClass ->
            !ddClass.isRetired() && !classHasRelationships(ddClass) }
        return errors
    }

    boolean classHasRelationships(NhsDDClass ddClass) {
        if(ddClass.classRelationships.size() > 0) {
            return true
        } else {
            boolean found = false
            ddClass.extendsClasses.each {extendedClass
                if(classHasRelationships(extendedClass)) {
                    found = true
                }
            }
            return found
        }
    }


}
