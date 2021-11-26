package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel

import grails.rest.*
import grails.converters.*

class DataSetController extends DataDictionaryComponentController<DataModel> {

    static responseFormats = ['json', 'xml']

    @Override
    String getParameterIdKey() {
        "dataSetId"
    }



    @Override
    DataDictionaryComponentService getService() {
        return dataSetService
    }

}