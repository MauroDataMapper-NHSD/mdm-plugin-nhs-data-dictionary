/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.meta.DitaElement

import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change

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

    /**
     * The {@link NhsDDClass} that this attribute belongs under. Must be set during ingest
     * once all the attributes are mapped to the {@link NhsDataDictionary} and the classes
     * are loaded.
     */
    NhsDDClass parentClass

    List<NhsDDCode> codes = []

    String codesVersion

    Set<NhsDDElement> instantiatedByElements = [] as Set

    boolean getIsKey() {
        if (!otherProperties.containsKey("isKey")) {
            return false
        }

        Boolean.parseBoolean(otherProperties["isKey"])
    }

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
                code.webOrder = Integer.parseInt(concept.property.find {it.code[0].@value == "Web Order"}?.valueInteger[0]?.@value)
                code.webPresentation = unquoteString(concept.property.find {it.code[0].@value == "Web Presentation"}?.valueString?[0]?.@value)
                code.isDefault = (code.webOrder == null || code.webOrder == 0)


                codes.add(code)
            }
            // now clear the web order field if we don't need it
            if(codes.findAll { it.webOrder }.sort {it.code }.code == codes.findAll { it.webOrder }.sort {it.webOrder}.code) {
                codes.each {
                    it.webOrder == null
                }
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
            "dm:${NhsDataDictionary.CLASSES_MODEL_NAME}|dc:Retired|de:${name}"
        } else {
            String parentClassName = parentClass ? "dc:${parentClass.name}|" : ""
            return "dm:${NhsDataDictionary.CLASSES_MODEL_NAME}|${parentClassName}de:${name}"
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
    void buildComponentDetailsChangeList(List<Change> changes, NhsDataDictionaryComponent previousComponent, boolean includeDataSets) {
        Change standardDescriptionChange = createStandardDescriptionChange(previousComponent, includeDataSets)
        if (standardDescriptionChange) {
            changes.add(standardDescriptionChange)
        }

        // National codes should only be added if the component is new or updated
        Change nationalCodesChange = createNationalCodesChange(previousComponent as NhsDDAttribute)
        if (nationalCodesChange) {
            changes.add(nationalCodesChange)
        }

        // Aliases should only be added if the component is new or updated
        Change aliasesChange = createAliasesChange(previousComponent)
        if (aliasesChange) {
            changes.add(aliasesChange)
        }
    }

    Change createNationalCodesChange(NhsDDAttribute previousAttribute) {
        List<NhsDDCode> currentCodes = this.getOrderedNationalCodes()
        List<NhsDDCode> previousCodes = previousAttribute ? previousAttribute.getOrderedNationalCodes() : []

        if (currentCodes.isEmpty() && previousCodes.isEmpty()) {
            return null
        }

        String htmlDetail = NhsDDCode.createCodesTableChangeHtml(currentCodes, previousCodes)
        DitaElement ditaDetail = NhsDDCode.createCodesTableChangeDita(currentCodes, previousCodes, Change.NATIONAL_CODES_TYPE)

        new Change(
            changeType: Change.NATIONAL_CODES_TYPE,
            stereotype: stereotype,
            oldItem: previousAttribute,
            newItem: this,
            htmlDetail: htmlDetail,
            ditaDetail: ditaDetail,
            preferDitaDetail: true
        )
    }

    @Override
    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []
        topics.add(descriptionTopic())
        if (isActivePage()) {
            if (this.codes) {
                topics.add(getNationalCodesTopic())
            }
            if (getAliases()) {
                topics.add(aliasesTopic())
            }
            if (whereUsed) {
                topics.add(whereUsedTopic())
            }
            if (instantiatedByElements) {
                topics.add(getLinkedElementsTopic())
            }
        }
        topics.add(changeLogTopic())
        return topics
    }

    Topic getNationalCodesTopic() {
        List<NhsDDCode> orderedCodes = getOrderedNationalCodes()
        NhsDDCode.getCodesTopic(getDitaKey() + "_nationalCodes", "National Codes", orderedCodes)
    }

    List<NhsDDCode> getOrderedNationalCodes() {
        List<NhsDDCode> orderedCodes = codes
            .findAll { !it.isDefault }
            .sort {it.webOrder }

        orderedCodes
    }

    Topic getLinkedElementsTopic() {
        Topic.build (id: getDitaKey() + "_dataElements") {
            title "Data Elements"
            body {
                ul {
                    instantiatedByElements
                        .<NhsDDElement>findAll { element -> !element.isRetired() }
                        .sort { element -> element.name }
                        .each { element ->
                            li {
                                xRef (element.calculateXRef())
                            }
                        }
                }
            }
        }
    }

    @Override
    void updateWhereUsed() {
        instantiatedByElements.each { NhsDDElement element ->
            whereUsed[element] = "is the data element of $name".toString()
        }
        dataDictionary.classes.values().each {clazz ->
            if(clazz.allAttributes().contains(this)) {
                whereUsed[clazz] = "has an attribute $name of type $name".toString()
            }
        }
    }

}