package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDCode
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary


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
        respond getService().show(UUID.fromString(params.versionedFolderId), params.id)
    }

    def whereUsed() {
        respond getService().getWhereUsed(UUID.fromString(params.versionedFolderId), params[getParameterIdKey()])
    }

}
