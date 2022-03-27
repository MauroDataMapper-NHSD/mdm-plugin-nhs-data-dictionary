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

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionaryComponent

trait NhsDataDictionaryComponent {

    abstract String getStereotype()
    abstract String getStereotypeForPreview()

    NhsDataDictionary dataDictionary

    CatalogueItem catalogueItem
    String catalogueItemModelId
    String catalogueItemParentId

    String name
    String definition = ""

    Map<String, String> otherProperties = [:]


    abstract String calculateShortDescription()


    boolean isRetired() {
        "true" == otherProperties["isRetired"]
    }

    boolean isPreparatory() {
        "true" == otherProperties["isPreparatory"]
    }

    String getUin() {
        otherProperties["uin"]
    }

    String getShortDescription() {
        otherProperties["shortDescription"]
    }

    String getTitleCaseName() {
        otherProperties["titleCaseName"]
    }


    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        if(xml.name.text()) {
            this.name = xml.name.text().replace("_", " ")
        } else { // This should only apply for dataSetConstraints
            this.name = xml."class".name.text().replace("_", " ")
        }

        /*  We're doing capitalised items now
        if(xml.TitleCaseName.text()) {
            this.name = xml.TitleCaseName[0].text()
        } else { // This should only apply for dataSetConstraints
            this.name = xml."class".websitePageHeading.text()
        }*/

/*        String cleanedDefinition = xml.definition.text().
            replace("&amp;", "&").
            replaceAll( "&([^;]+(?!(?:\\w|;)))", "&amp;\$1" ).
            replace("<", "&lt;").
            replace(">", "&gt;").
            replace("\u00a0", " ")
        definition = DDHelperFunctions.parseHtml(cleanedDefinition)
        definition = definition.replaceAll("\\s+", " ")
*/
        definition = (DDHelperFunctions.parseHtml(xml.definition)).replace("\u00a0", " ")

        NhsDataDictionary.METADATA_FIELD_MAPPING.entrySet().each {entry ->
            String xmlValue = xml[entry.value][0].text()
            if(!xmlValue || xmlValue == "") {
                xmlValue = xml."class"[entry.value].text()
            }
            if(xmlValue && xmlValue != "") {
                otherProperties[entry.key] = xmlValue
            }
        }
    }

    boolean isValidXmlNode(def xmlNode) {
        return true
    }

    abstract String getXmlNodeName()

    boolean hasNoAliases() {
        return getAliases().size() == 0
    }

    Map<String, String> getAliases() {
        Map<String, String> aliases = [:]
        Set<String> aliasFields = NhsDataDictionary.getAllMetadataKeys().findAll {it.startsWith("alias")}

        aliasFields.each {aliasField ->
            String aliasValue = otherProperties[aliasField]
            if(aliasValue) {
                aliases[aliasField] = aliasValue
            }
        }
        return aliases
    }



    Map<String, String> getUrlReplacements() {
        String ddUrl = this.otherProperties["ddUrl"]
        return [
            (ddUrl) : this.getMauroPath()
        ]
    }

    String getNameWithoutNonAlphaNumerics() {
        name.replaceAll("[^A-Za-z0-9 ]", "").replace(" ", "_")
    }

    abstract String getMauroPath()
    String getDitaKey() {
        getStereotype().replace(" ", "") + "_" + getNameWithoutNonAlphaNumerics()
    }

    String getDescription() {
        if(dataDictionary && isRetired()) {
            return dataDictionary.retiredItemText
        } else if(dataDictionary && isPreparatory()) {
            return dataDictionary.preparatoryItemText
        } else {
            return definition
        }
    }


    String getNameWithRetired() {
        if(isRetired()) {
            return this.name + " (Retired)"
        } else {
            return this.name
        }
    }



}