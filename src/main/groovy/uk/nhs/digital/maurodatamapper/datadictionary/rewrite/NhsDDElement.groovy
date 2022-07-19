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
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.XRef
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Format

import groovy.util.logging.Slf4j

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
                    if (concept.property.find {property ->
                        property.code.@value.toString() == "Data Element" &&
                        property.valueString.@value.toString() == capitalizedCodeSetName
                    }) {
                        NhsDDCode code = linkedAttribute.codes.find {it -> it.code == concept.code.@value.toString()}
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

    }

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
