package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary


abstract class DataDictionaryComponentController<T extends CatalogueItem> {
	static responseFormats = ['json', 'xml']

    DataSetService dataSetService
    ElementService elementService
    ClassService classService
    AttributeService attributeService
    BusinessDefinitionService businessDefinitionService
    SupportingInformationService supportingInformationService
    XmlSchemaConstraintService xmlSchemaConstraintService

    NhsDataDictionaryService nhsDataDictionaryService

    abstract DataDictionaryComponentService getService()

    abstract String getParameterIdKey()


    def index() {
        respond getService().index(UUID.fromString(params.versionedFolderId), params.boolean('includeRetired')?:false)
    }

    def show() {
        respond getService().show(params.branchName, params.id)
    }

    def whereUsed() {
        DataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(params.branchName)
        respond getService().getWhereUsed(dataDictionary, params[getParameterIdKey()])
    }

}
