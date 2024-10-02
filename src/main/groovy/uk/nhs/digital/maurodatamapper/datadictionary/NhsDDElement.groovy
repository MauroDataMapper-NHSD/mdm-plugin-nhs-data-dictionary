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

    @Override
    String calculateShortDescription() {
        if(isPreparatory()) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                String firstSentence = getFirstSentence()
                if (!description && instantiatesAttributes.size() == 1) {
                    return instantiatesAttributes[0].getShortDescription()
                } else if (firstSentence && firstSentence.toLowerCase().contains("is the same as") && instantiatesAttributes.size() == 1) {
                    return instantiatesAttributes[0].getShortDescription()
                } else if(firstSentence) {
                    return firstSentence
                } else {
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

    @Override
    Topic descriptionTopic() {
        Topic.build (id: getDitaKey() + "_description") {
            title "Description"
            body {
                if (isActivePage()) {
                    if (otherProperties["attributeText"]) {
                        div HtmlHelper.replaceHtmlWithDita(otherProperties["attributeText"])
                    } else {
                        if (instantiatesAttributes.size() == 1) {
                            p {
                                xRef this.calculateXRef()
                                text " is the same as attribute "
                                xRef instantiatesAttributes[0].calculateXRef()
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
    void buildComponentDetailsChangeList(List<Change> changes, NhsDataDictionaryComponent previousComponent, boolean includeDataSets) {
        Change formatLengthChange = createFormatLengthChange(previousComponent as NhsDDElement)
        if (formatLengthChange) {
            changes.add(formatLengthChange)
        }

        Change standardDescriptionChange = createStandardDescriptionChange(previousComponent, includeDataSets)
        if (standardDescriptionChange) {
            changes.add(standardDescriptionChange)
        }

        // National codes should only be added if the component is new or updated
        Change nationalCodesChange = createNationalCodesChange(previousComponent as NhsDDElement)
        if (nationalCodesChange) {
            changes.add(nationalCodesChange)
        }

        // Default codes should only be added if the component is new or updated
        Change defaultCodesChange = createDefaultCodesChange(previousComponent as NhsDDElement)
        if (defaultCodesChange) {
            changes.add(defaultCodesChange)
        }

        // Aliases should only be added if the component is new or updated
        Change aliasesChange = createAliasesChange(previousComponent)
        if (aliasesChange) {
            changes.add(aliasesChange)
        }
    }

    Change createNationalCodesChange(NhsDDElement previousElement) {
        List<NhsDDCode> currentCodes = this.getOrderedNationalCodes()
        List<NhsDDCode> previousCodes = previousElement ? previousElement.getOrderedNationalCodes() : []

        if (currentCodes.isEmpty() && previousCodes.isEmpty()) {
            return null
        }

        String htmlDetail = NhsDDCode.createCodesTableChangeHtml(currentCodes, previousCodes)
        DitaElement ditaDetail = NhsDDCode.createCodesTableChangeDita(currentCodes, previousCodes, Change.NATIONAL_CODES_TYPE)

        new Change(
            changeType: Change.NATIONAL_CODES_TYPE,
            stereotype: stereotype,
            oldItem: previousElement,
            newItem: this,
            htmlDetail: htmlDetail,
            ditaDetail: ditaDetail,
            preferDitaDetail: true
        )
    }

    Change createDefaultCodesChange(NhsDDElement previousElement) {
        List<NhsDDCode> currentCodes = this.getOrderedDefaultCodes()
        List<NhsDDCode> previousCodes = previousElement ? previousElement.getOrderedDefaultCodes() : []

        if (currentCodes.isEmpty() && previousCodes.isEmpty()) {
            return null
        }

        String htmlDetail = NhsDDCode.createCodesTableChangeHtml(currentCodes, previousCodes)
        DitaElement ditaDetail = NhsDDCode.createCodesTableChangeDita(currentCodes, previousCodes, Change.DEFAULT_CODES_TYPE)

        new Change(
            changeType: Change.DEFAULT_CODES_TYPE,
            stereotype: stereotype,
            oldItem: previousElement,
            newItem: this,
            htmlDetail: htmlDetail,
            ditaDetail: ditaDetail,
            preferDitaDetail: true
        )
    }

    Change createFormatLengthChange(NhsDDElement previousElement) {
        String currentFormatLength = this.formatLength
        XRef currentFormatLinkXref = this.formatLinkXref

        String previousFormatLength = previousElement?.formatLength
        XRef previousFormatLinkXref = previousElement?.formatLinkXref

        String htmlDetail = createFormatLengthChangeHtml(currentFormatLength, currentFormatLinkXref, previousFormatLength, previousFormatLinkXref)
        DitaElement ditaDetail = createFormatLengthChangeDita(currentFormatLength, currentFormatLinkXref, previousFormatLength, previousFormatLinkXref)

        new Change(
            changeType: Change.FORMAT_LENGTH_TYPE,
            stereotype: stereotype,
            oldItem: previousElement,
            newItem: this,
            htmlDetail: htmlDetail,
            ditaDetail: ditaDetail,
            preferDitaDetail: true
        )
    }

    static String createFormatLengthChangeHtml(String currentFormatLength, XRef currentFormatLinkXref, String previousFormatLength, XRef previousFormatLinkXref) {
        StringWriter stringWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(stringWriter)

        markupBuilder.div(class: "format-length-detail") {
            dl {
                dt "Format / Length"
                dd {
                    if (currentFormatLinkXref) {
                        if (!previousFormatLinkXref) {
                            markupBuilder.p(class: "new") {
                                // TODO: not sure what to output for HTML here, but just for preview
                                mkp.yield("See ${currentFormatLinkXref.toXmlString()}")
                            }
                        }
                        else {
                            markupBuilder.p {
                                // TODO: not sure what to output for HTML here, but just for preview
                                mkp.yield("See ${currentFormatLinkXref.toXmlString()}")
                            }
                        }
                    }
                    else {
                        if (!previousFormatLength || (previousFormatLength && previousFormatLength != currentFormatLength)) {
                            markupBuilder.p(class: "new") {
                                mkp.yield(currentFormatLength)
                            }
                            if (previousFormatLength) {
                                markupBuilder.p(class: "deleted") {
                                    mkp.yield(previousFormatLength)
                                }
                            }
                        } else if (!currentFormatLength || (currentFormatLength && currentFormatLength != previousFormatLength)) {
                            markupBuilder.p(class: "deleted") {
                                mkp.yield(currentFormatLength)
                            }
                            if (currentFormatLength) {
                                markupBuilder.p(class: "new") {
                                    mkp.yield(currentFormatLength)
                                }
                            }
                        } else {
                            markupBuilder.p {
                                mkp.yield(currentFormatLength)
                            }
                        }
                    }
                }
            }
        }

        return stringWriter.toString()
    }

    static DitaElement createFormatLengthChangeDita(String currentFormatLength, XRef currentFormatLinkXref, String previousFormatLength, XRef previousFormatLinkXref) {
        Div.build {
            dl {
                dlentry {
                    dt "Format / Length"
                    dd {
                        if (currentFormatLinkXref) {
                            if (!previousFormatLinkXref) {
                                p(outputClass: "new") {
                                    txt "See "
                                    xRef currentFormatLinkXref
                                }
                            }
                            else {
                                p {
                                    txt "See "
                                    xRef currentFormatLinkXref
                                }
                            }
                        }
                        else {
                            if (!previousFormatLength || (previousFormatLength && previousFormatLength != currentFormatLength)) {
                                p(outputClass: "new") {
                                    txt currentFormatLength
                                }
                                if (previousFormatLength) {
                                    p(outputClass: "deleted") {
                                        txt previousFormatLength
                                    }
                                }
                            } else if (!currentFormatLength || (currentFormatLength && currentFormatLength != previousFormatLength)) {
                                p(outputClass: "deleted") {
                                    txt currentFormatLength
                                }
                                if (currentFormatLength) {
                                    p(outputClass: "new") {
                                        txt currentFormatLength
                                    }
                                }
                            } else {
                                p currentFormatLength
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []

        if (isActivePage() && (otherProperties["formatLength"] || otherProperties["formatLink"])) {
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


}
