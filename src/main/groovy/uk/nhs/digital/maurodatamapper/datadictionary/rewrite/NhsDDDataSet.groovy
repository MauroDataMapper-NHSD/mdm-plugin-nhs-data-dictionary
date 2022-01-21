package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import org.apache.commons.lang3.StringUtils
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

@Slf4j
class NhsDDDataSet implements NhsDataDictionaryComponent {

    @Override
    String getStereotype() {
        "Data Set"
    }

    @Override
    String getStereotypeForPreview() {
        "dataSet"
    }


    List<String> path
    GPathResult definitionAsXml

    boolean isCDS

    List<NhsDDDataSetClass> dataSetClasses

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)
        NhsDDWebPage explanatoryWebPage = dataDictionary.webPagesByUin[xml.explanatoryPage.text()]
        if(explanatoryWebPage) {
            definition = explanatoryWebPage.definition
        } else {
            if(isRetired()) {
                log.info("Cannot find explanatory page for dataset: {}", name)
            } else {
                log.warn("Cannot find explanatory page for dataset: {}", name)
            }
        }
        path = getPath()
        definitionAsXml = xml.definition
        otherProperties["approvingOrganisation"] = "Data Alliance Partnership Board (DAPB)"
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
                xml = Html.xmlSlurper.parseText("<xml>" + definition + "</xml>")
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
                log.error("Couldn't parse: " + definition)
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


    String getMauroPath() {
        String mauroPath = StringUtils.join(path.collect{"fo:${it}"}, "|")

        mauroPath += "dm:${name}"

    }


}
