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
package uk.nhs.digital.maurodatamapper.datadictionary

import groovy.xml.XmlUtil
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.XRef

import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.maurodatamapper.dita.html.HtmlHelper

@Slf4j
class NhsDDElement implements NhsDataDictionaryComponent <DataElement> {

    @Override
    String getStereotype() {
        "Data Element"
    }

    @Override
    String getStereotypeForPreview() {
        "element"
    }


    @Override
    String getPluralStereotypeForWebsite() {
        "elements"
    }



    List<NhsDDCode> codes = []

    XRef formatLinkXref

    String codeSetVersion

    List<NhsDDAttribute> instantiatesAttributes = []

    @Override
    String calculateShortDescription() {
        if(isPreparatory()) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                String firstSentence = getFirstSentence()
                if (firstSentence && firstSentence.toLowerCase().contains("is the same as") && instantiatesAttributes.size() == 1) {
                    return instantiatesAttributes[0].otherProperties["shortDescription"]
                } else if(firstSentence) {
                    return firstSentence
                }
            } catch (Exception e) {
                e.printStackTrace()
                log.error("Couldn't parse: ${definition}")
                return name
            }
        }
    }

    @Override
    String getXmlNodeName() {
        "DDDataElement"
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)
        String capitalizedCodeSetName = xml.name[0].text()
        if (xml."value-set".size() > 0 && !isRetired()) {
            String codeSetVersion = xml."value-set".Bundle.entry.expansion.parameter.Bundle.entry.resource.CodeSystem.version."@value".text()
            String attributeUin = xml.link.participant.find {it -> it.@role == 'Supplier'}.@referencedUin
            NhsDDAttribute linkedAttribute = dataDictionary.attributesByUin[attributeUin]
            if(!linkedAttribute) {
                log.error("No linked attribute with UIN ${attributeUin} found for element ${name}")
            } else {
                xml."value-set".Bundle.entry.expansion.parameter.Bundle.entry.resource.CodeSystem.concept.each {concept ->
                    if (concept.property.find {Node property ->
                        property.code[0].attribute('value') == "Data Element" &&
                        property.valueString[0].attribute('value') == capitalizedCodeSetName
                    }) {
                        NhsDDCode code = linkedAttribute.codes.find {it -> it.code == concept.code[0].attribute('value')}
                        codes.add(code)
                    }
                }
            }

        }
        instantiatesAttributes.addAll(xml."link".collect {link ->
            link.participant.find{p -> p["@role"] == "Supplier"}["@referencedUin"]
        }.collect {
            dataDictionary.attributesByUin[it]
        })
        instantiatesAttributes.each {nhsDDAttribute ->
            nhsDDAttribute.instantiatedByElements.add(this)
        }


        if(!isRetired()) {
            if (definition.find(regex)) {
                definition = definition.replaceFirst(regex, "").trim()
            } else {
                Node definitionXml = HtmlHelper.tidyAndConvertToNode("<p>" + definition + "<p>")
                Node firstParagraph = definitionXml.children().find{it instanceof Node && it.name() == 'p'}
                firstParagraph.parent().remove(firstParagraph)

                otherProperties["attributeText"] = XmlUtil.serialize(firstParagraph).replaceFirst("<\\?xml version=\"1.0\".*\\?>", "")
                definition = XmlUtil.serialize(definitionXml).replaceFirst("<\\?xml version=\"1.0\".*\\?>", "")

            }
        }

    }

    //final String regex = "<a[^>]*>[^<]*<\\/a> is the same as attribute\\W<a[^>]*>[^<]*<\\/a>\\s*\\."
    final String regex = "<a[^>]*>[^<]*</a>\\W+is\\s+the\\s+same\\s+as\\s+attribute\\W+<a[^>]*>[^<]*</a>\\s*\\."

    String getMauroPath() {
        if(isRetired()) {
            "dm:${NhsDataDictionary.CORE_MODEL_NAME}|dc:${NhsDataDictionary.DATA_ELEMENTS_CLASS_NAME}|dc:Retired|de:${name}"
        } else {
            return "dm:${NhsDataDictionary.CORE_MODEL_NAME}|dc:${NhsDataDictionary.DATA_ELEMENTS_CLASS_NAME}|de:${name}"
        }
    }

    boolean hasNationalCodes() {
        this.codes.find { !it.isDefault}
    }

    boolean hasDefaultCodes() {
        this.codes.find { it.isDefault }
    }

    @Override
    void replaceLinksInDefinition(Map<String, NhsDataDictionaryComponent> pathLookup) {
        NhsDataDictionaryComponent.super.replaceLinksInDefinition(pathLookup)
        codes.each { code ->
            code.webPresentation = replaceLinksInString(code.webPresentation, pathLookup)
        }
        if(otherProperties["formatLink"] && pathLookup[otherProperties["formatLink"]]) {
            formatLinkXref = pathLookup[otherProperties["formatLink"]].calculateXRef()
        }
        if(otherProperties["attributeText"] && otherProperties["attributeText"] != "") {
            otherProperties["attributeText"] = replaceLinksInString(otherProperties["attributeText"], pathLookup)
        }
    }

    @Override
    String getDescription() {
        if(dataDictionary && isRetired()) {
            return dataDictionary.retiredItemText
        } else if(dataDictionary && isPreparatory()) {
            return dataDictionary.preparatoryItemText
        } else {
            if(otherProperties["attributeText"] && otherProperties["attributeText"] != "") {
                return otherProperties["attributeText"] + definition
            } else {
                if(instantiatesAttributes.size() == 1) {

                    return "<p>" +
                            "<a href=\"${getMauroPath()}\">" +
                            getNameWithRetired() +
                            "</a> is the same as attribute " +
                            "<a href=\"${instantiatesAttributes[0].getMauroPath()}\"'>" +
                            instantiatesAttributes[0].getNameWithRetired() +
                            "</a>.</p>" +
                            definition
                } else {
                    return definition
                }
            }
        }
    }


    @Override
    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []
        topics.add(descriptionTopic())

        if(otherProperties["formatLength"] || otherProperties["formatLink"]) {
            topics.add(getFormatLengthTopic())
        }
        if(!isPreparatory() && hasNationalCodes()) {
            topics.add(getNationalCodesTopic())
        }
        if(!isPreparatory() && hasDefaultCodes()) {
            topics.add(getDefaultCodesTopic())
        }
        if(getAliases()) {
            topics.add(aliasesTopic())
        }
        if(whereUsed) {
            topics.add(whereUsedTopic())
        }
        if(instantiatesAttributes) {
            topics.add(getAttributesTopic())
        }
        return topics
    }

    Topic getFormatLengthTopic() {
        Topic.build (id: getDitaKey() + "_formatLength") {
            title "Format / Length"
            body {
                if(formatLinkXref) {
                    p {
                        txt "See "
                        xRef formatLinkXref
                    }
                } else {
                    p otherProperties["formatLength"]
                }
            }
        }
    }

    Topic getNationalCodesTopic() {
        List<NhsDDCode> orderedCodes = codes.findAll { !it.isDefault }.sort {it.code}
        if(codes.find{it.webOrder }) {
            orderedCodes = orderedCodes.sort {it.webOrder}
        }
        String topicTitle = "National Codes"
        if(instantiatesAttributes) {
            if(orderedCodes.size() < instantiatesAttributes[0].codes.findAll { !it.isDefault }.size()) {
                topicTitle = "Permitted National Codes"
            }
        }

        NhsDDCode.getCodesTopic(getDitaKey() + "_nationalCodes", topicTitle, orderedCodes)
    }

    Topic getDefaultCodesTopic() {
        List<NhsDDCode> orderedCodes = codes.findAll { it.isDefault }.sort {it.code}
        if(orderedCodes.find{it.webOrder }) {
            orderedCodes = orderedCodes.sort {it.webOrder}
        }
        NhsDDCode.getCodesTopic(getDitaKey() + "_defaultCodes", "Default Codes", orderedCodes)
    }


    Topic getAttributesTopic() {
        Topic.build (id: getDitaKey() + "_attributes") {
            title "Attribute"
            body {
                ul {
                    instantiatesAttributes.each { NhsDDAttribute attribute ->
                        li {
                            xRef (attribute.calculateXRef())
                        }
                    }
                }
            }
        }
    }

}