package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

@Slf4j
class NhsDDDataSet implements NhsDataDictionaryComponent {

    @Override
    String getStereotype() {
        "Data Set"
    }

    List<String> path
    String overview
    GPathResult definitionAsXml

    boolean isCDS

    List<NhsDDDataSetClass> dataSetClasses

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)
        NhsDDWebPage explanatoryWebPage = dataDictionary.webPagesByUin[xml.explanatoryPage.text()]
        if(explanatoryWebPage) {
            overview = explanatoryWebPage.definition
        } else {
            log.error("Cannot find explanatory page for dataset: {}", name)
        }
        definitionAsXml = xml.definition
    }

    @Override
    String calculateShortDescription() {
        String shortDescription = name
        if(isPreparatory()) {
            shortDescription = "This item is being used for development purposes and has not yet been approved."
        } else {

            List<String> aliases = [name]
            aliases.addAll(otherProperties.findAll{key, value -> key.startsWith("alias")}.collect {it.value})

            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + overview + "</xml>")
                String allParagraphs = ""
                xml.p.each { paragraph ->
                    allParagraphs += paragraph.text()
                }
                String nextSentence = ""
                while (!aliases.find { it -> nextSentence.contains(it) } && allParagraphs.contains(".")) {
                    nextSentence = allParagraphs.substring(0, allParagraphs.indexOf(".") + 1)
                    if (aliases.find { it -> nextSentence.contains(it) }) {
                        if(nextSentence.startsWith("Introduction")) {
                            nextSentence = nextSentence.replaceFirst("Introduction", "")
                        }
                        shortDescription = nextSentence
                        break
                    }
                    allParagraphs = allParagraphs.substring(allParagraphs.indexOf(".") + 1)
                }
            } catch (Exception e) {
                log.error("Couldn't parse: " + overview)
                shortDescription = name
            }
        }

        otherProperties["shortDescription"] = shortDescription
        return shortDescription
    }

    @Override
    String getXmlNodeName() {
        "DDDataSet"
    }

}
