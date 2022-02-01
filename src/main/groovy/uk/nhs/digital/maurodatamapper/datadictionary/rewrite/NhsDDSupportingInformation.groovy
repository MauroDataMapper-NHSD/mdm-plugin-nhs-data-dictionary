package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

@Slf4j
class NhsDDSupportingInformation implements NhsDataDictionaryComponent {

    @Override
    String getStereotype() {
        "Supporting Information"
    }

    @Override
    String getStereotypeForPreview() {
        "supportingInformation"
    }


    @Override
    String calculateShortDescription() {
        String shortDescription
        if(isPreparatory()) {
            shortDescription = "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + DDHelperFunctions.parseHtml(definition) + "</xml>")
                String firstParagraph = xml.p[0].text()
                if (xml.p.size() == 0) {
                    firstParagraph = xml.text()
                }
                shortDescription = firstParagraph.substring(0, firstParagraph.indexOf(". ") + 1)
            } catch (Exception e) {
                log.error("Couldn't parse: " + definition)
                shortDescription = name
            }
        }
        //shortDescription = shortDescription.replaceAll("\\s+", " ")
        otherProperties["shortDescription"] = shortDescription
        return shortDescription
    }

    @Override
    boolean isValidXmlNode(def xmlNode) {
        return xmlNode."base-uri".text().contains("Supporting_Definitions") ||
                xmlNode."base-uri".text().contains("Supporting_Information") ||
                xmlNode."base-uri".text().contains("CDS_Supporting_Information")
    }

    @Override
    String getXmlNodeName() {
        "DDWebPage"
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)
    }

    String getMauroPath() {
        return "tm:${NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}|te:${name}"
    }



}
