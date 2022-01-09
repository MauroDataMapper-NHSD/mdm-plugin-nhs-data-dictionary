package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd


import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

class AttributeController extends DataDictionaryComponentController<DataElement> {
	static responseFormats = ['json', 'xml']

    @Override
    String getParameterIdKey() {
        "attributeId"
    }


    @Override
    DataDictionaryComponentService getService() {
        return attributeService
    }
}
