/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 */
package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.html.HtmlHelper
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList

import groovy.util.logging.Slf4j
import groovy.xml.slurpersupport.GPathResult

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
        if (isPreparatory()) {
            shortDescription = "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                GPathResult xml
                xml = xmlSlurper.parseText("<xml>" + definition + "</xml>")
                String firstParagraph = xml.p[0].text()
                if (xml.p.size() == 0) {
                    firstParagraph = xml.text()
                }
                shortDescription = firstParagraph.substring(0, firstParagraph.indexOf(".") + 1)
            } catch (Exception e) {
                e.printStackTrace()
                log.error("Couldn't parse: " + definition)
                shortDescription = name
            }
        }
        shortDescription = shortDescription.replace("_", " ")
        //shortDescription = shortDescription.replaceAll("\\s+", " ")
        otherProperties["shortDescription"] = shortDescription
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)

        if (xml."code-system".size() > 0) {
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
        if (isRetired()) {
            "dm:${NhsDataDictionary.CORE_MODEL_NAME}|dc:${NhsDataDictionary.ATTRIBUTES_CLASS_NAME}|dc:Retired|de:${name}"
        } else {
            return "dm:${NhsDataDictionary.CORE_MODEL_NAME}|dc:${NhsDataDictionary.ATTRIBUTES_CLASS_NAME}|de:${name}"
        }
    }

    @Override
    void replaceLinksInDefinition(Map<String, NhsDataDictionaryComponent> pathLookup) {
        NhsDataDictionaryComponent.super.replaceLinksInDefinition(pathLookup)
        codes.each { code ->
            code.definition = replaceLinksInString(code.definition, pathLookup)
        }
    }


    @Override
    List<Topic> getWebsiteTopics(Map<String, NhsDataDictionaryComponent>pathLookup) {
        List<Topic> topics = []
        topics.add(descriptionTopic(pathLookup))
        if(!isPreparatory() && this.codes) {
            topics.add(getNationalCodesTopic())
        }
        if(whereUsed) {
            topics.add(whereUsedTopic())
        }
        if(getAliases()) {
            topics.add(aliasesTopic())
        }
        return topics
    }

    Topic getNationalCodesTopic() {
        Topic.build (id: getDitaKey() + "_nationalCodes") {
            title "National Codes"
            body {
                simpletable(relColWidth: new SpaceSeparatedStringList (["1*", "4*"]), outputClass: "table table-striped table-sm") {
                    stHead (outputClass: "thead-light") {
                        stentry "Code"
                        stentry "Description"
                    }
                    codes.each {code ->
                        strow {
                            stentry code.code
                            stentry {
                                div HtmlHelper.replaceHtmlWithDita(code.definition)
                            }
                        }
                    }
                }
            }
        }
    }


}