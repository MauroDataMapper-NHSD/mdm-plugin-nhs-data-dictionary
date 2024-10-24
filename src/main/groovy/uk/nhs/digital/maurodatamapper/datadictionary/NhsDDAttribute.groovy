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
import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.ChangeAware
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.ChangeFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.CodesRow
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.CodesSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItem
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItemState
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.ItemLink
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.ItemLinkListSection

@Slf4j
class NhsDDAttribute implements NhsDataDictionaryComponent <DataElement>, ChangeAware {

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

    @Deprecated
    @Override
    List<Change> getChanges(NhsDataDictionaryComponent previousComponent) {
        List<Change> changes = []

        Change descriptionChange = createDescriptionChange(previousComponent)
        if (descriptionChange) {
            changes.add(descriptionChange)
        }

        if (isActivePage()) {
            Change nationalCodesChange = createNationalCodesChange(previousComponent as NhsDDAttribute)
            if (nationalCodesChange) {
                changes.add(nationalCodesChange)
            }

            Change aliasesChange = createAliasesChange(previousComponent)
            if (aliasesChange) {
                changes.add(aliasesChange)
            }

            Change linkedElementsChange = createLinkedElementsChange(previousComponent as NhsDDAttribute)
            if (linkedElementsChange) {
                changes.add(linkedElementsChange)
            }
        }

        changes
    }

    Change createNationalCodesChange(NhsDDAttribute previousAttribute) {
        List<NhsDDCode> currentCodes = this.getOrderedNationalCodes()
        List<NhsDDCode> previousCodes = previousAttribute ? previousAttribute.getOrderedNationalCodes() : []

        if (currentCodes.empty && previousCodes.empty) {
            return null
        }

        if (ChangeFunctions.areEqual(currentCodes, previousCodes)) {
            // Identical lists. If this is a new item, this will never be true so will always include a "Codes" change
            return null
        }

        StringWriter htmlWriter = NhsDDCode.createCodesTableChangeHtml(Change.NATIONAL_CODES_TYPE, currentCodes, previousCodes)

        createChange(Change.NATIONAL_CODES_TYPE, previousAttribute, htmlWriter)
    }

    Change createLinkedElementsChange(NhsDDAttribute previousAttribute) {
        List<NhsDDElement> currentElements = instantiatedByElements
            .<NhsDDElement>findAll { element -> !element.isRetired() }
            .sort { element -> element.name }

        List<NhsDDElement> previousElements = previousAttribute
            ? previousAttribute.instantiatedByElements
                .<NhsDDElement>findAll { element -> !element.isRetired() }
                .sort { element -> element.name }
            : []

        if (currentElements.empty && previousElements.empty) {
            return null
        }

        if (ChangeFunctions.areEqual(currentElements, previousElements)) {
            // Identical lists. If this is a new item, this will never be true so will always include a "Data Elements" change
            return null
        }

        StringWriter htmlWriter = ChangeFunctions.createUnorderedListHtml("Data Elements", currentElements, previousElements)

        createChange(Change.CHANGED_ELEMENTS_TYPE, previousAttribute, htmlWriter)
    }

    @Override
    DictionaryItem getPublishStructure() {
        DictionaryItem dictionaryItem = DictionaryItem.create(this)

        addDescriptionSection(dictionaryItem)

        if (itemState == DictionaryItemState.ACTIVE) {
            addNationalCodesSection(dictionaryItem)
            addAliasesSection(dictionaryItem)
            addWhereUsedSection(dictionaryItem)
            addLinkedElementsSection(dictionaryItem)
        }

        addChangeLogSection(dictionaryItem)

        dictionaryItem
    }

    void addNationalCodesSection(DictionaryItem dictionaryItem) {
        if (!this.codes) {
            return
        }

        List<NhsDDCode> orderedCodes = getOrderedNationalCodes()
        List<CodesRow> rows = orderedCodes.collect {code ->
            new CodesRow(code.code, code.description, code.hasWebPresentation())
        }

        dictionaryItem.addSection(CodesSection.createNationalCodes(dictionaryItem, rows))
    }

    void addLinkedElementsSection(DictionaryItem dictionaryItem) {
        if (!instantiatedByElements) {
            return
        }

        List<NhsDDElement> elements = instantiatedByElements
            .<NhsDDElement>findAll { element -> !element.isRetired() }
            .sort { element -> element.name }

        if (elements.empty) {
            return
        }

        List<ItemLink> itemLinks = elements.collect {element -> ItemLink.create(element) }
        dictionaryItem.addSection(ItemLinkListSection.createElementsListSection(dictionaryItem, itemLinks))
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
                Topic linkedElementsTopic = getLinkedElementsTopic()
                if (linkedElementsTopic) {
                    topics.add(linkedElementsTopic)
                }
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
        def elements = instantiatedByElements
            .<NhsDDElement>findAll { element -> !element.isRetired() }
            .sort { element -> element.name }

        if (elements.empty) {
            return null
        }

        Topic.build (id: getDitaKey() + "_dataElements") {
            title "Data Elements"
            body {
                ul {
                    elements.each { element ->
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

    @Override
    String getDiscriminator() {
        return name
    }
}