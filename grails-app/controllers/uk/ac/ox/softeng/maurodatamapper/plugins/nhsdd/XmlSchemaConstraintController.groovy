package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd


import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

class XmlSchemaConstraintController extends DataDictionaryComponentController<Term> {
    static responseFormats = ['json', 'xml']

    @Override
    DataDictionaryComponentService getService() {
        return xmlSchemaConstraintService
    }
}