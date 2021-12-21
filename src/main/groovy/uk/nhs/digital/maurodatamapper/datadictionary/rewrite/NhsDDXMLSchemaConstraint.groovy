package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

class NhsDDXMLSchemaConstraint implements NhsDataDictionaryComponent {

    @Override
    String calculateShortDescription() {
        String shortDescription = name
        otherProperties["shortDescription"] = shortDescription
        return shortDescription
    }

    @Override
    String getXmlNodeName() {
        "DDXmlSchemaConstraint"
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)
    }
}
