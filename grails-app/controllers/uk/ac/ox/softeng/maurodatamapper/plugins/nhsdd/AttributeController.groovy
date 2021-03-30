package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService

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
