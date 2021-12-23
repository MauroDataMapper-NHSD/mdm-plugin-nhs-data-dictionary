package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks

import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware

import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

trait IntegrityCheck {

    String name
    String description

    abstract List<NhsDataDictionaryComponent> runCheck (NhsDataDictionary dataDictionary)

}