package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

class BusinessDefinitionController extends DataDictionaryComponentController<Term>{
    static responseFormats = ['json', 'xml']

    @Override
    String getParameterIdKey() {
        "businessDefinitionId"
    }


    @Override
    DataDictionaryComponentService getService() {
        return businessDefinitionService
    }
}

