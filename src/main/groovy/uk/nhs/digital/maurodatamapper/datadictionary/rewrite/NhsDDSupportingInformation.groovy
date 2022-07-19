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
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

@Slf4j
class NhsDDSupportingInformation implements NhsDataDictionaryComponent <Term> {

    @Override
    String getStereotype() {
        "Supporting Information"
    }

    @Override
    String getStereotypeForPreview() {
        "supportingInformation"
    }


    @Override
    String calculateShortDescription() {
        if(!definition || definition == "") {
            return name
        }
        if(isPreparatory()) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                return getFirstSentence()
            } catch (Exception e) {
                log.error("Couldn't parse the definition for $name because ${e.message}")
                return name
            }
        }
    }

    @Override
    boolean isValidXmlNode(def xmlNode) {
        return xmlNode."base-uri".text().contains("Supporting_Definitions") ||
                xmlNode."base-uri".text().contains("Supporting_Information") ||
                xmlNode."base-uri".text().contains("CDS_Supporting_Information")
    }

    @Override
    String getXmlNodeName() {
        "DDWebPage"
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)
    }

    String getMauroPath() {
        return "te:${NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}|tm:${name}"
    }



}
