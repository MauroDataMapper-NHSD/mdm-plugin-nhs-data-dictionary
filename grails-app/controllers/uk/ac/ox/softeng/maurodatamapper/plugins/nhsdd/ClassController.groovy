package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass

class ClassController extends DataDictionaryComponentController<DataClass>{
	static responseFormats = ['json', 'xml']

    @Override
    String getParameterIdKey() {
        "classId"
    }


    @Override
    DataDictionaryComponentService getService() {
        return classService
    }
}
