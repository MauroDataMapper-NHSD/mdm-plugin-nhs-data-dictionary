package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd


import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

class SupportingInformationController extends DataDictionaryComponentController<Term> {
    static responseFormats = ['json', 'xml']

    @Override
    String getParameterIdKey() {
        "supportingInformationId"
    }

    @Override
    DataDictionaryComponentService getService() {
        return supportingInformationService
    }
}