package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService

import grails.rest.*
import grails.converters.*

class ClassController {
	static responseFormats = ['json', 'xml']
	NhsDataDictionaryService nhsDataDictionaryService
    DataClassService dataClassService

    def index() {

        Set<DataClass> classes = nhsDataDictionaryService.getDataClasses(false)

        def result = classes.sort{it.label}.collect {dataClass ->
            [
                catalogueId: dataClass.id.toString(),
                name: dataClass.label,
                stereotype: "class"

            ]
        }

        respond result
    }

    def show() {
        DataClass dataClass = dataClassService.get(params.id)

        String description = dataClass.description

        def result = [
                catalogueId: dataClass.id.toString(),
                name: dataClass.label,
                stereotype: "class",
                description: description

        ]
        respond result
    }
}
