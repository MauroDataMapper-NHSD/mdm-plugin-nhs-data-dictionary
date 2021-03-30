package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd


import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

class ElementController extends DataDictionaryComponentController<DataElement>{
	static responseFormats = ['json', 'xml']

    @Override
    String getParameterIdKey() {
        "elementId"
    }



    @Override
    DataDictionaryComponentService getService() {
        return elementService
    }
}
