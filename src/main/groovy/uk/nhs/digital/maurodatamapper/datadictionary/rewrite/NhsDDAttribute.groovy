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

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic

import groovy.util.logging.Slf4j

@Slf4j
class NhsDDAttribute implements NhsDataDictionaryComponent <DataElement> {

    @Override
    String getStereotype() {
        "Attribute"
    }

    @Override
    String getStereotypeForPreview() {
        "attribute"
    }

    @Override
    String getPluralStereotypeForWebsite() {
        "attributes"
    }

    List<NhsDDCode> codes = []

    String codesVersion

    Set<NhsDDElement> instantiatedByElements = [] as Set

    @Override
    String calculateShortDescription() {
        if (isPreparatory()) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                return getFirstSentence()
            } catch (Exception e) {
                e.printStackTrace()
                log.error("Couldn't parse: " + definition)
                return name
            }
        }
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)

        if (xml."code-system".size() > 0) {
            codesVersion = xml."code-system"[0].Bundle.entry.resource.CodeSystem.version."@value".text()
            xml."code-system"[0].Bundle.entry.resource.CodeSystem.concept.each {Node concept ->
                NhsDDCode code = new NhsDDCode(this)

                code.code = concept.code[0].@value.toString()
                code.definition = concept.display[0].@value.toString()

                code.publishDate = concept.property.find {it.code[0].@value == "Publish Date"}?.valueDateTime[0].@value

                code.isRetired = false
                if(concept.property.find {it.code[0].@value == "Status"}) {
                    code.isRetired = concept.property.find {it.code[0].@value == "Status"}?.valueString[0]?.@value == "retired"
                } else if(concept.property.find {it.code[0].@value == "status"}) {
                    code.isRetired = concept.property.find {it.code[0].@value == "status"}?.valueCode[0]?.@value == "retired"
                }
                if(code.isRetired) {
                    code.retiredDate = concept.property.find {it.code[0].@value == "Retired Date"}?.valueDateTime[0]?.@value
                }
                code.webOrder = concept.property.find {it.code[0].@value == "Web Order"}?.valueInteger[0]?.@value
                code.webPresentation = unquoteString(concept.property.find {it.code[0].@value == "Web Presentation"}?.valueString?[0]?.@value)
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

    static String unquoteString(String input) {
        if(input) {
            return input.
                replaceAll("&quot;", "\"").
                replaceAll("&gt;", ">").
                replaceAll("&lt;", "<").
                replaceAll("&apos;", "'")
        } else {
            return null
        }
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
            code.webPresentation = replaceLinksInString(code.webPresentation, pathLookup)
        }
    }


    @Override
    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []
        topics.add(descriptionTopic())
        if(!isPreparatory() && this.codes) {
            topics.add(getNationalCodesTopic())
        }
        if(getAliases()) {
            topics.add(aliasesTopic())
        }
        if(whereUsed) {
            topics.add(whereUsedTopic())
        }
        if(instantiatedByElements) {
            topics.add(getDataElementsTopic())
        }
        return topics
    }

    Topic getNationalCodesTopic() {
        List<NhsDDCode> orderedCodes = codes.sort {it.code}
        if(codes.find{it.webOrder }) {
            orderedCodes = codes.sort {it.webOrder}
        }
        NhsDDCode.getCodesTopic(getDitaKey() + "_nationalCodes", "National Codes", orderedCodes)
    }

    Topic getDataElementsTopic() {
        Topic.build (id: getDitaKey() + "_dataElements") {
            title "Data Elements"
            body {
                ul {
                    instantiatedByElements.each { NhsDDElement element ->
                        li {
                            xRef (element.calculateXRef())
                        }
                    }
                }
            }
        }
    }

}