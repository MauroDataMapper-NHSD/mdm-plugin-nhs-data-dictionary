package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem


abstract class DataDictionaryComponentController<T extends CatalogueItem> {
	static responseFormats = ['json', 'xml']

    DataSetService dataSetService
    ElementService elementService
    ClassService classService
    AttributeService attributeService
    BusinessDefinitionService businessDefinitionService
    SupportingInformationService supportingInformationService
    XmlSchemaConstraintService xmlSchemaConstraintService


    abstract DataDictionaryComponentService getService()

    def index() {
        respond getService().index()
    }

    def show() {
        respond getService().show(params.id)
    }
}
