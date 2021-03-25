package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService

import grails.rest.*
import grails.converters.*

class ClassController extends DataDictionaryComponentController<DataClass>{
	static responseFormats = ['json', 'xml']

    @Override
    DataDictionaryComponentService getService() {
        return classService
    }
}
