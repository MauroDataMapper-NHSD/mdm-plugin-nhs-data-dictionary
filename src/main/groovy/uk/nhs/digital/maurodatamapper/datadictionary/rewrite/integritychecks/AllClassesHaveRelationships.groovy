package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType

import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

class AllClassesHaveRelationships implements IntegrityCheck {

    String name = "Class Relationships Defined"

    String description = "Check that all live classes have relationships to other classes defined"

    @Override
    List<NhsDataDictionaryComponent> runCheck(NhsDataDictionary dataDictionary) {

        errors = dataDictionary.classes.values().findAll{ddClass ->
            !ddClass.isRetired() && ddClass.classRelationships.size() == 0 }
        return errors
    }

}
