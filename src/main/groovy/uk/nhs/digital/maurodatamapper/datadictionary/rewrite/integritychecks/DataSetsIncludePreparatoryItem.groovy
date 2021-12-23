package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType

import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

class DataSetsIncludePreparatoryItem implements IntegrityCheck {

    String name = "Datasets that include preparatory items"

    String description = "Check that a dataset doesn't include any preparatory data elements in its definition"

    @Override
    List<NhsDataDictionaryComponent> runCheck(NhsDataDictionary dataDictionary) {

        List<NhsDataDictionaryComponent> prepItems = dataDictionary.elements.values().findAll {it.isPreparatory()}

        errors = dataDictionary.dataSets.values().findAll {component ->
            ((DataModel)component.catalogueItem).allDataElements.find {dataElement ->
                prepItems.find {it.name == dataElement.label}
            }
        }
        return errors
    }

}
