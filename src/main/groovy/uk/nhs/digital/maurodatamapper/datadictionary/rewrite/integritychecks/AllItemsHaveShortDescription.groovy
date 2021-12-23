package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType

import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

class AllItemsHaveShortDescription implements IntegrityCheck {

    String name = "All items have a short description"

    String description = "Check that all items have a short description"

    @Override
    List<NhsDataDictionaryComponent> runCheck(NhsDataDictionary dataDictionary) {

        errors = dataDictionary.getAllComponents().findAll {component ->
            String shortDesc = component.otherProperties["shortDescription"]
            (shortDesc == null || shortDesc == "")
        }
        return errors
    }

}
