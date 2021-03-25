package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService

import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary

class SupportingInformationController extends DataDictionaryComponentController<Term> {
    static responseFormats = ['json', 'xml']

    @Override
    DataDictionaryComponentService getService() {
        return supportingInformationService
    }
}