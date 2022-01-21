package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

@Slf4j
class NhsDDAttribute implements NhsDataDictionaryComponent {

    @Override
    String getStereotype() {
        "Attribute"
    }

    @Override
    String getStereotypeForPreview() {
        "attribute"
    }

    List<NhsDDCode> codes = []

    String codesVersion

    Set<NhsDDElement> instantiatedByElements = [] as Set

    @Override
    String calculateShortDescription() {
        String shortDescription
        if(isPreparatory()) {
            shortDescription = "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + definition + "</xml>")
                String firstParagraph = xml.p[0].text()
                if (xml.p.size() == 0) {
                    firstParagraph = xml.text()
                }
                shortDescription = firstParagraph.substring(0, firstParagraph.indexOf(".") + 1)
            } catch (Exception e) {
                log.error("Couldn't parse: " + definition)
                shortDescription = name
            }
        }
        shortDescription.replaceAll("\\w+", " ")
        otherProperties["shortDescription"] = shortDescription
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)

        if(xml."code-system".size() > 0) {
            codesVersion = xml."code-system"[0].Bundle.entry.resource.CodeSystem.version."@value".text()
            xml."code-system"[0].Bundle.entry.resource.CodeSystem.concept.each {concept ->
                NhsDDCode code = new NhsDDCode(this)

                code.code = concept.code.@value.toString()
                code.definition = concept.display.@value.toString()

                code.publishDate = concept.property.find {it.code."@value" == "Publish Date"}?.valueDateTime?."@value"?.text()
                code.isRetired = concept.property.find {it.code."@value" == "Status"}?.valueString?."@value"?.text() == "retired"
                code.retiredDate = concept.property.find {it.code."@value" == "Retired Date"}?.valueDateTime?."@value"?.text()
                code.webOrder = concept.property.find {it.code."@value" == "Web Order"}?.valueInteger?."@value"?.text()
                code.webPresentation = concept.property.find {it.code."@value" == "Web Presentation"}?.valueString?."@value"?.text()
                code.isDefault = (code.webOrder == null || code.webOrder == "0")
                codes.add(code)
            }
        }
        dataDictionary.attributesByUin[getUin()] = this
    }

    @Override
    String getXmlNodeName() {
        "DDAttribute"
    }

    String getFormatLengthHtml() {
        if (otherProperties["formatLink"]) {
            return """<a href='${otherProperties["formatLink"]}'>${otherProperties["formatLength"]}</a>"""
        } else if (otherProperties["formatLength"]) {
            return otherProperties["formatLength"]
        } else return ""
    }

    String getMauroPath() {
        if(isRetired()) {
            "dm:${NhsDataDictionary.CORE_MODEL_NAME}|dc:${NhsDataDictionary.ATTRIBUTES_CLASS_NAME}|dc:Retired|de:${name}"
        } else {
            return "dm:${NhsDataDictionary.CORE_MODEL_NAME}|dc:${NhsDataDictionary.ATTRIBUTES_CLASS_NAME}|de:${name}"
        }
    }
}
