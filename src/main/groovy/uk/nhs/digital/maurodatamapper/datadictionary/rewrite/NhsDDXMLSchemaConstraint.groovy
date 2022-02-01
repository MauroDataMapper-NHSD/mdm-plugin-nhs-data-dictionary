package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

class NhsDDXMLSchemaConstraint implements NhsDataDictionaryComponent {

    @Override
    String getStereotype() {
        "XML Schema Constraint"
    }

    @Override
    String getStereotypeForPreview() {
        "xmlSchemaConstraint"
    }


    @Override
    String calculateShortDescription() {
        String shortDescription = name
        //shortDescription = shortDescription.replaceAll("\\w+", " ")
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

    String getMauroPath() {
        return "tm:${NhsDataDictionary.XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME}|te:${name}"
    }

}
