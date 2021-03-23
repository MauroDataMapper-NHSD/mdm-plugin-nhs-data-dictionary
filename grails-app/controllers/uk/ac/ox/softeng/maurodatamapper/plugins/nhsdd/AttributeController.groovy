package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService

class AttributeController {
	static responseFormats = ['json', 'xml']
	NhsDataDictionaryService nhsDataDictionaryService
    DataElementService dataElementService

    def index() {

        Set<DataElement> attributes = nhsDataDictionaryService.getAttributes(false)

        def result = attributes.sort{it.label}.collect {dataElement ->
            [
                catalogueId: dataElement.id.toString(),
                name: dataElement.label,
                stereotype: "attribute"

            ]
        }

        respond result
    }

    def show() {
        DataElement dataElement = dataElementService.get(params.id)

        String description = dataElement.description

        def result = [
                catalogueId: dataElement.id.toString(),
                name: dataElement.label,
                stereotype: "attribute",
                description: description
        ]
        respond result
    }
}
