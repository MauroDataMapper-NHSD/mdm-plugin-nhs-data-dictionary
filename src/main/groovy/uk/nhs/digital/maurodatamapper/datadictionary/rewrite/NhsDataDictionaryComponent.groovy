package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions

trait NhsDataDictionaryComponent {

    abstract String getStereotype()

    CatalogueItem catalogueItem

    String name
    String definition

    Map<String, String> otherProperties = [:]


    abstract String calculateShortDescription()


    boolean isRetired() {
        "true" == otherProperties["isRetired"]
    }

    boolean isPreparatory() {
        "true" == otherProperties["isPreparatory"]
    }

    String getUin() {
        otherProperties["uin"]
    }


    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        if(xml.TitleCaseName.text()) {
            this.name = xml.TitleCaseName[0].text()
        } else { // This should only apply for XmlSchemaConstraints
            this.name = xml."class".websitePageHeading.text()
        }

        definition = (DDHelperFunctions.parseHtml(xml.definition)).replace("\u00a0", " ")

        NhsDataDictionary.METADATA_FIELD_MAPPING.entrySet().each {entry ->
            String xmlValue = xml[entry.value].text()
            if(!xmlValue || xmlValue == "") {
                xmlValue = xml."class"[entry.value].text()
            }
            if(xmlValue && xmlValue != "") {
                otherProperties[entry.key] = xmlValue
            }
        }
    }

    boolean isValidXmlNode(def xmlNode) {
        return true
    }

    abstract String getXmlNodeName()

}