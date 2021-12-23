package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType

import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

class AllItemsHaveAlias implements IntegrityCheck {

    String name = "All items have an alias"

    String description = "Check that all items have one of the alias fields completed"

    @Override
    List<NhsDataDictionaryComponent> runCheck(NhsDataDictionary dataDictionary) {

        errors = dataDictionary.getAllComponents().findAll {component ->
            !component.isRetired() &&
            component.hasNoAliases()
        }
        return errors
    }

}
