package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

class NhsDDWebPage implements NhsDataDictionaryComponent {

    @Override
    String calculateShortDescription() {
        return null
    }

    @Override
    String getXmlNodeName() {
        "DDWebPage"
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)
        dataDictionary.webPagesByUin[getUin()] = this

    }
}