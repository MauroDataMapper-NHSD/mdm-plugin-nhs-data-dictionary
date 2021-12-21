package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

@Slf4j
class NhsDDBusinessDefinition implements NhsDataDictionaryComponent {

    @Override
    String calculateShortDescription() {
        String shortDescription
        if (isPreparatory()) {
            shortDescription = "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + DDHelperFunctions.parseHtml(definition.toString()) + "</xml>")
                String firstParagraph = xml.p[0].text()
                if (xml.p.size() == 0) {
                    firstParagraph = xml.text()
                }
                String firstSentence = firstParagraph.substring(0, firstParagraph.indexOf(".") + 1)
                if (firstSentence.toLowerCase().contains("is a")) {
                    String secondParagraph = xml.p[1].text()
                    String secondSentence = secondParagraph.substring(0, secondParagraph.indexOf(".") + 1)
                    shortDescription = secondSentence
                } else {
                    shortDescription = firstSentence
                }
            } catch (Exception e) {
                log.error("Couldn't parse: " + definition)
                shortDescription = code
            }
        }

        otherProperties["shortDescription"] = shortDescription
        return shortDescription
    }

    @Override
    String getXmlNodeName() {
        "DDBusinessDefinition"
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)
    }
}
