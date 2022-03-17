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


import groovy.util.logging.Slf4j
import groovy.xml.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

@Slf4j
class NhsDDElement implements NhsDataDictionaryComponent {

    @Override
    String getStereotype() {
        "Data Element"
    }

    @Override
    String getStereotypeForPreview() {
        "element"
    }



    List<NhsDDCode> codes = []

    String codeSetVersion

    List<NhsDDAttribute> instantiatesAttributes = []

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
                String firstSentence = firstParagraph.substring(0, firstParagraph.indexOf(".") + 1)
                if (firstSentence.toLowerCase().contains("is the same as")) {
                    if (instantiatesAttributes.size() == 1) {
                        firstSentence = instantiatesAttributes[0].otherProperties["shortDescription"]
                    } else {
                        firstSentence = name
                    }
                }
                shortDescription = firstSentence
            } catch (Exception e) {
                e.printStackTrace()
                log.error("Couldn't parse: ${definition}")
                shortDescription = name
            }
        }
        //shortDescription = shortDescription.replaceAll("\\s+", " ")
        otherProperties["shortDescription"] = shortDescription
        return shortDescription
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
            "dm:${NhsDataDictionary.CORE_MODEL_NAME}|dc:${NhsDataDictionary.DATA_FIELD_NOTES_CLASS_NAME}|dc:Retired|de:${name}"
        } else {
            return "dm:${NhsDataDictionary.CORE_MODEL_NAME}|dc:${NhsDataDictionary.DATA_FIELD_NOTES_CLASS_NAME}|de:${name}"
        }
    }

}
