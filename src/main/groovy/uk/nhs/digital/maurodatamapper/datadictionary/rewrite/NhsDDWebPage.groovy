package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions

class NhsDDWebPage implements NhsDataDictionaryComponent {

    List<String> path = []

    @Override
    String getStereotype() {
        "Web Page"
    }

    @Override
    String getStereotypeForPreview() {
        "webPage"
    }

    @Override
    boolean isValidXmlNode(def xmlNode) {
        return !xmlNode."base-uri".text().contains("Supporting_Definitions") &&
               !xmlNode."base-uri".text().contains("Supporting_Information") &&
               !xmlNode."base-uri".text().contains("CDS_Supporting_Information")
    }


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
        if(otherProperties["baseUri"].contains("Messages")) {
            path = getPath()
        }
    }

    String getMauroPath() {
        return ""
    }

    List<String> getPath() {
        List<String> path = []
        try {
            path.addAll(DDHelperFunctions.getPath(otherProperties["baseUri"], "Messages", ".txaClass20"))
        } catch (Exception e) {
            path.addAll(DDHelperFunctions.getPath(otherProperties["baseUri"], "Web_Site_Content", ".txaClass20"))
        }
        path.removeAll {it.equalsIgnoreCase("Data_Sets")}
        path.removeAll {it.equalsIgnoreCase("Content")}

        if (isRetired()) {
            path.add(0, "Retired")
        }
        path = path.collect {DDHelperFunctions.tidyLabel(it)}
        return path
    }


}