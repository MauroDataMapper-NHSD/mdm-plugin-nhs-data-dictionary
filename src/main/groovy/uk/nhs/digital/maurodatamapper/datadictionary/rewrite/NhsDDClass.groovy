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
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

@Slf4j
class NhsDDClass implements NhsDataDictionaryComponent {

    @Override
    String getStereotype() {
        "Class"
    }

    @Override
    String getStereotypeForPreview() {
        "class"
    }


    List<NhsDDCode> codes = []

    List<NhsDDClass> extendsClasses = []

    List<NhsDDAttribute> keyAttributes = []
    List<NhsDDAttribute> otherAttributes = []

    List<NhsDDClassRelationship> classRelationships = []

    List<NhsDDClassLink> classLinks = []

    @Override
    String calculateShortDescription() {
        String shortDescription
        if(isPreparatory()) {
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
                if (firstSentence.toLowerCase().contains("a subtype of")) {
                    String secondParagraph = xml.p[1].text()
                    String secondSentence = secondParagraph.substring(0, secondParagraph.indexOf(".") + 1)
                    shortDescription = secondSentence
                } else {
                    shortDescription = firstSentence
                }
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
    String getXmlNodeName() {
        "DDClass"
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)

        xml.property.each { property ->
            String attributeUin = property.referencedElement.text()
            NhsDDAttribute attribute = dataDictionary.attributesByUin[attributeUin]
            if(attribute) {
                if("true" == property.isUnique.text()) {
                    keyAttributes.add(attribute)
                } else {
                    otherAttributes.add(attribute)
                }
            } else {
                log.debug("Cannot find attributeElement with Uin: " + attributeUin)
            }
        }
        classLinks = xml.link.collect{it -> new NhsDDClassLink(it)}
        dataDictionary.classesByUin[getUin()] = this
    }

    List<NhsDDAttribute> allAttributes() {
        keyAttributes + otherAttributes
    }

    String getMauroPath() {
        if(isRetired()) {
            "dm:${NhsDataDictionary.CORE_MODEL_NAME}|dc:${NhsDataDictionary.DATA_CLASSES_CLASS_NAME}|dc:Retired|dc:${name}"
        } else {
            return "dm:${NhsDataDictionary.CORE_MODEL_NAME}|dc:${NhsDataDictionary.DATA_CLASSES_CLASS_NAME}|dc:${name}"
        }
    }
}
