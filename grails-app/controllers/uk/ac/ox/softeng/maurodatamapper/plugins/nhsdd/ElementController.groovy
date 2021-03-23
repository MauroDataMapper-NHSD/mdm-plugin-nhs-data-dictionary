package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd


import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService

class ElementController {
	static responseFormats = ['json', 'xml']
	NhsDataDictionaryService nhsDataDictionaryService
    DataElementService dataElementService

    def index() {

        Set<DataElement> elements = nhsDataDictionaryService.getDataFieldNotes(false)

        def result = elements.sort{it.label}.collect {dataElement ->
            [
                catalogueId: dataElement.id.toString(),
                name: dataElement.label,
                stereotype: "element"

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
                stereotype: "element",
                description: description
        ]
        respond result
    }
}
