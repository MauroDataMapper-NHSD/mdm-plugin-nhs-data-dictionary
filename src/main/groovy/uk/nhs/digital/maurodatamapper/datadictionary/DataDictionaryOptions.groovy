package uk.nhs.digital.maurodatamapper.datadictionary

trait DataDictionaryOptions {

    abstract boolean getProcessDataSets()

    abstract boolean getProcessElements()

    abstract boolean getProcessAttributes()

    abstract boolean getProcessClasses()

    abstract boolean getProcessNhsBusinessDefinitions()

    abstract boolean getProcessSupportingDefinitions()

    abstract boolean getProcessXmlSchemaConstraints()

    abstract boolean getProcessWebPages()


}
