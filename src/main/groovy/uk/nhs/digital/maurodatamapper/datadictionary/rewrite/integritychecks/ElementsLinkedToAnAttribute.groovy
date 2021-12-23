package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType

import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

class ElementsLinkedToAnAttribute implements IntegrityCheck {

    String name = "Elements linked to an attribute"

    String description = "Check that all live elements are linked to an attribute"

    @Override
    List<NhsDataDictionaryComponent> runCheck(NhsDataDictionary dataDictionary) {

        errors = dataDictionary.elements.values().findAll {ddElement ->
            // log.debug(ddAttribute.classLinks.size())
            !ddElement.isRetired() &&
            ddElement.getInstantiatesAttributes() == []
        }
        return errors
    }

}
