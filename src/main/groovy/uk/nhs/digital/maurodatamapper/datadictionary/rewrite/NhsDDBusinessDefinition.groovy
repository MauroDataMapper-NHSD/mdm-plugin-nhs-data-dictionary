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

import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import groovy.util.logging.Slf4j
import groovy.xml.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

@Slf4j
class NhsDDBusinessDefinition implements NhsDataDictionaryComponent {

    @Override
    String getStereotype() {
        "NHS Business Definition"
    }

    @Override
    String getStereotypeForPreview() {
        "businessDefinition"
    }


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
                shortDescription = name
            }
        }
        //shortDescription = shortDescription.replaceAll("\\s+", " ")
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

    String getMauroPath() {
        return "te:${NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME}|tm:${name}"
    }
}
