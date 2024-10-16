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

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.meta.DitaElement

import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.XRef

import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.maurodatamapper.dita.helpers.HtmlHelper

import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.ChangeAware
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.ChangeFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.CodesRow
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.CodesSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItem
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItemState
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.FormatLengthSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.ItemLink
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.ItemLinkListSection

@Slf4j
class NhsDDElement implements NhsDataDictionaryComponent <DataElement>, ChangeAware {

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
        "data_elements"
    }



    List<NhsDDCode> codes = []

    XRef formatLinkXref

    String codeSetVersion

    List<NhsDDAttribute> instantiatesAttributes = []

    String getFormatLength() {
        if (!otherProperties.containsKey("formatLength")) {
            return null
        }

        otherProperties["formatLength"]
    }

    boolean hasFormatLength() {
        otherProperties["formatLength"] || otherProperties["formatLink"]
    }

    String previewAttributeText

    boolean isEmptyDescription() {
        !description || description == "<div/>"
    }

    @Override
    String calculateShortDescription() {
        if(isPreparatory()) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                List<NhsDDAttribute> activeAttributes = instantiatesAttributes.findAll { !it.isRetired() }
                String firstSentence = getFirstSentence()
                boolean missingDescription = isEmptyDescription()
                if (missingDescription && otherProperties["attributeText"]) {
                    return getSentence(otherProperties["attributeText"], 0)
                }
                else if (missingDescription && activeAttributes.size() == 1) {
                    return activeAttributes[0].getShortDescription()
                }
                else if (firstSentence && firstSentence.toLowerCase().contains("is the same as") && activeAttributes.size() == 1) {
                    return activeAttributes[0].getShortDescription()
                }
                else if (firstSentence) {
                    return firstSentence
                }
                else {
                    System.err.println("Couldn't set short description: $stereotype $name")
                    System.err.println("$description")
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

        instantiatesAttributes.addAll(xml."link".collect {link ->
            link.participant.find{p -> p["@role"] == "Supplier"}["@referencedUin"]
        }.collect {
            dataDictionary.attributesByUin[it]
        })
        instantiatesAttributes.each {nhsDDAttribute ->
            nhsDDAttribute.instantiatedByElements.add(this)
        }


        if (xml."value-set".size() > 0 && !isRetired()) {
            String codeSetVersion = xml."value-set".Bundle.entry.expansion.parameter.Bundle.entry.resource.CodeSystem.version."@value".text()
            NhsDDAttribute linkedAttribute = instantiatesAttributes.find {it.codes.size() > 0 && !it.isRetired()}
            if(!linkedAttribute) {
                log.error("No suitable linked attribute found for element ${name}")
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
        return getPathFromName(name, isRetired())
    }

    static String getPathFromName(String name, Boolean retired = false) {
        if(retired) {
            "dm:${NhsDataDictionary.ELEMENTS_MODEL_NAME}|dc:Retired|de:${name}"
        } else {
            // Match ingest structure of organising DD Elements alphabetically
            String className = name.substring(0, 1).toUpperCase()
            return "dm:${NhsDataDictionary.ELEMENTS_MODEL_NAME}|dc:${className}|de:${name}"
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

/*    @Override
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
*/

    String getAttributeTextAsHtml() {
        if (!isActivePage()) {
            return null
        }

        if (otherProperties["attributeText"]) {
            return otherProperties["attributeText"]
        }

        List<NhsDDAttribute> activeAttributes = instantiatesAttributes.findAll {!it.isRetired() }
        if (activeAttributes.size() == 1) {
            NhsDDAttribute attribute = activeAttributes[0]
            return "<a href=\"${this.getMauroPath()}\">${this.name}</a> is the same as attribute <a href=\"${attribute.getMauroPath()}\">${attribute.name}</a>."
        }

        return null
    }

    @Override
    Topic descriptionTopic() {
        Topic.build (id: getDitaKey() + "_description") {
            title "Description"
            body {
                if (isActivePage()) {
                    if (otherProperties["attributeText"]) {
                        div HtmlHelper.replaceHtmlWithDita(otherProperties["attributeText"])
                    }
                    else {
                        List<NhsDDAttribute> activeAttributes = instantiatesAttributes.findAll {!it.isRetired() }
                        if (activeAttributes.size() == 1) {
                            p {
                                xRef this.calculateXRef()
                                text " is the same as attribute "
                                xRef activeAttributes[0].calculateXRef()
                                text "."
                            }
                        }
                    }
                }

                if (definition) {
                    div HtmlHelper.replaceHtmlWithDita(definition.replace('<table', '<table class=\"table-striped\"'))
                }
            }
        }
    }

    @Override
    List<Change> getChanges(NhsDataDictionaryComponent previousComponent) {
        List<Change> changes = []

        if (isActivePage()) {
            Change formatLengthChange = createFormatLengthChange(previousComponent as NhsDDElement)
            if (formatLengthChange) {
                changes.add(formatLengthChange)
            }
        }

        Change descriptionChange = createDescriptionChange(previousComponent)
        if (descriptionChange) {
            changes.add(descriptionChange)
        }

        if (isActivePage()) {
            Change nationalCodesChange = createNationalCodesChange(previousComponent as NhsDDElement)
            if (nationalCodesChange) {
                changes.add(nationalCodesChange)
            }

            Change defaultCodesChange = createDefaultCodesChange(previousComponent as NhsDDElement)
            if (defaultCodesChange) {
                changes.add(defaultCodesChange)
            }

            Change aliasesChange = createAliasesChange(previousComponent)
            if (aliasesChange) {
                changes.add(aliasesChange)
            }

            Change linkedAttributesChange = createLinkedAttributesChange(previousComponent as NhsDDElement)
            if (linkedAttributesChange) {
                changes.add(linkedAttributesChange)
            }
        }

        changes
    }

    Change createNationalCodesChange(NhsDDElement previousElement) {
        List<NhsDDCode> currentCodes = this.getOrderedNationalCodes()
        List<NhsDDCode> previousCodes = previousElement ? previousElement.getOrderedNationalCodes() : []

        if (currentCodes.empty && previousCodes.empty) {
            return null
        }

        if (ChangeFunctions.areEqual(currentCodes, previousCodes)) {
            // Identical lists. If this is a new item, this will never be true so will always include a "Codes" change
            return null
        }

        StringWriter htmlWriter = NhsDDCode.createCodesTableChangeHtml(Change.NATIONAL_CODES_TYPE, currentCodes, previousCodes)

        createChange(Change.NATIONAL_CODES_TYPE, previousElement, htmlWriter)
    }

    Change createDefaultCodesChange(NhsDDElement previousElement) {
        List<NhsDDCode> currentCodes = this.getOrderedDefaultCodes()
        List<NhsDDCode> previousCodes = previousElement ? previousElement.getOrderedDefaultCodes() : []

        if (currentCodes.empty && previousCodes.empty) {
            return null
        }

        if (ChangeFunctions.areEqual(currentCodes, previousCodes)) {
            // Identical lists. If this is a new item, this will never be true so will always include a "Codes" change
            return null
        }

        StringWriter htmlWriter = NhsDDCode.createCodesTableChangeHtml(Change.DEFAULT_CODES_TYPE, currentCodes, previousCodes)

        createChange(Change.DEFAULT_CODES_TYPE, previousElement, htmlWriter)
    }

    Change createLinkedAttributesChange(NhsDDElement previousElement) {
        List<NhsDDAttribute> currentAttributes = instantiatesAttributes
            .findAll { attribute -> !attribute.isRetired() }
            .sort { attribute -> attribute.name }

        List<NhsDDAttribute> previousAttributes = previousElement
            ? previousElement.instantiatesAttributes
                .findAll { attribute -> !attribute.isRetired() }
                .sort { attribute -> attribute.name }
            : []

        if (currentAttributes.empty && previousAttributes.empty) {
            return null
        }

        if (ChangeFunctions.areEqual(currentAttributes, previousAttributes)) {
            // Identical lists. If this is a new item, this will never be true so will always include an "Attributes" change
            return null
        }

        StringWriter htmlWriter = ChangeFunctions.createUnorderedListHtml("Attributes", currentAttributes, previousAttributes)

        createChange(Change.CHANGED_ATTRIBUTES_TYPE, previousElement, htmlWriter)
    }

    Change createFormatLengthChange(NhsDDElement previousElement) {
        def currentFormatLength = new NhsDDFormatLength(this)
        def previousFormatLength = previousElement ? new NhsDDFormatLength(previousElement) : NhsDDFormatLength.EMPTY

        if (currentFormatLength.empty() && previousFormatLength.empty()) {
            return null
        }

        boolean isNewItem = previousElement == null

        if (!isNewItem && currentFormatLength.equals(previousFormatLength)) {
            // Identical, nothing to show. If it's a new item though, always show this
            return null
        }

        StringWriter htmlWriter = currentFormatLength.getChangeHtml(previousFormatLength)
        DitaElement ditaElement = currentFormatLength.getChangeDita(previousFormatLength)

        createChange(Change.FORMAT_LENGTH_TYPE, previousElement, htmlWriter, ditaElement, true)
    }

    @Override
    DictionaryItem getPublishStructure() {
        DictionaryItem dictionaryItem = DictionaryItem.create(this)

        if (itemState == DictionaryItemState.ACTIVE) {
            addFormatLengthSection(dictionaryItem)
        }

        addDescriptionSection(dictionaryItem)

        if (itemState == DictionaryItemState.ACTIVE) {
            if (hasNationalCodes()) {
                List<CodesRow> nationalCodes = createCodesSectionRows(orderedNationalCodes)
                dictionaryItem.addSection(CodesSection.createNationalCodes(dictionaryItem, nationalCodes))
            }
            if (hasDefaultCodes()) {
                List<CodesRow> defaultCodes = createCodesSectionRows(orderedDefaultCodes)
                dictionaryItem.addSection(CodesSection.createDefaultCodes(dictionaryItem, defaultCodes))
            }
            addAliasesSection(dictionaryItem)
            addWhereUsedSection(dictionaryItem)
            addLinkedAttributesSection(dictionaryItem)
        }

        addChangeLogSection(dictionaryItem)

        dictionaryItem
    }

    static List<CodesRow> createCodesSectionRows(List<NhsDDCode> codesList) {
        codesList.collect {code ->
            new CodesRow(code.code, code.description, code.hasWebPresentation())
        }
    }

    void addFormatLengthSection(DictionaryItem dictionaryItem) {
        dictionaryItem.addSection(new FormatLengthSection(dictionaryItem, new NhsDDFormatLength(this)))
    }

    void addLinkedAttributesSection(DictionaryItem dictionaryItem) {
        if (!instantiatesAttributes) {
            return
        }

        List<NhsDDAttribute> attributes = instantiatesAttributes
            .findAll { attribute -> !attribute.isRetired() }
            .sort { attribute -> attribute.name }

        if (attributes.empty) {
            return
        }

        List<ItemLink> itemLinks = attributes.collect {attribute -> ItemLink.create(attribute) }
        dictionaryItem.addSection(ItemLinkListSection.createAttributesListSection(dictionaryItem, itemLinks))
    }

    @Override
    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []

        if (isActivePage() && hasFormatLength()) {
            topics.add(getFormatLengthTopic())
        }

        topics.add(descriptionTopic())

        if (isActivePage()) {
            if (hasNationalCodes()) {
                topics.add(getNationalCodesTopic())
            }
            if (hasDefaultCodes()) {
                topics.add(getDefaultCodesTopic())
            }
            if (getAliases()) {
                topics.add(aliasesTopic())
            }
            if (whereUsed) {
                topics.add(whereUsedTopic())
            }
            if (instantiatesAttributes) {
                Topic linkedAttributesTopic = getLinkedAttributesTopic()
                if (linkedAttributesTopic) {
                    topics.add(linkedAttributesTopic)
                }
            }
        }
        topics.add(changeLogTopic())
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

    List<NhsDDCode> getOrderedNationalCodes() {
        List<NhsDDCode> orderedCodes = codes
            .findAll { !it.isDefault }
            .sort {it.webOrder }

        return orderedCodes
    }

    List<NhsDDCode> getOrderedDefaultCodes() {
        List<NhsDDCode> orderedCodes = codes
            .findAll { it.isDefault }
            .sort {it.webOrder }

        return orderedCodes
    }

    Topic getNationalCodesTopic() {
        List<NhsDDCode> orderedCodes = getOrderedNationalCodes()
        String topicTitle = "National Codes"
        if(instantiatesAttributes) {
            if(orderedCodes.size() < instantiatesAttributes[0].codes.findAll { !it.isDefault }.size()) {
                topicTitle = "Permitted National Codes"
            }
        }

        NhsDDCode.getCodesTopic(getDitaKey() + "_nationalCodes", topicTitle, orderedCodes)
    }

    Topic getDefaultCodesTopic() {
        List<NhsDDCode> orderedCodes = getOrderedDefaultCodes()
        NhsDDCode.getCodesTopic(getDitaKey() + "_defaultCodes", "Default Codes", orderedCodes)
    }


    Topic getLinkedAttributesTopic() {
        List<NhsDDAttribute> attributes = instantiatesAttributes
            .findAll { attribute -> !attribute.isRetired() }
            .sort { attribute -> attribute.name }

        if (attributes.empty) {
            return null
        }

        String attributesTitle = attributes.size() > 1 ? "Attributes" : "Attribute"
        Topic.build (id: getDitaKey() + "_attributes") {
            title attributesTitle
            body {
                ul {
                    attributes.each { attribute ->
                        li {
                            xRef (attribute.calculateXRef())
                        }
                    }
                }
            }
        }
    }

    void updateWhereUsed() {
        dataDictionary.dataSets.values().each {dataSet ->
            if(dataSet.allElements.reuseElement.contains(this)) {
                whereUsed[dataSet] = "references in description $name".toString()
            }
        }
    }

    @Override
    String getDiscriminator() {
        name
    }
}
