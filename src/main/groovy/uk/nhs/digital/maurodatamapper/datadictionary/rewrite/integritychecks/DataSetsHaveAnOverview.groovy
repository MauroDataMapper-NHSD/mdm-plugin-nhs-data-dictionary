package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType

import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

class DataSetsHaveAnOverview implements IntegrityCheck {

    String name = "Data Sets have an overview"

    String description = "Check that all live datasets have an overview field"

    @Override
    List<NhsDataDictionaryComponent> runCheck(NhsDataDictionary dataDictionary) {

        errors = dataDictionary.dataSets.values().findAll {ddDataSet ->
            // log.debug(ddAttribute.classLinks.size())
            !ddDataSet.isRetired() &&
            (ddDataSet.definition == null || ddDataSet.definition == "")
        }
        return errors
    }

}
