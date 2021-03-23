package uk.nhs.digital.maurodatamapper.datadictionary.importer

import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionaryOptions

abstract class DataDictionaryLoader {

    abstract DataDictionary load(DataDictionaryOptions options)
}
