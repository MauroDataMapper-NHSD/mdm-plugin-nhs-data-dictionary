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

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.DitaMap
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.XRef
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Scope
import uk.ac.ox.softeng.maurodatamapper.dita.helpers.HtmlHelper
import uk.ac.ox.softeng.maurodatamapper.dita.meta.DitaElement
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.DaisyDiffHelper
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.AliasesRow
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.AliasesSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DescriptionSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItem
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItemState
import uk.nhs.digital.maurodatamapper.datadictionary.utils.DDHelperFunctions

import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Slf4j
trait NhsDataDictionaryComponent <T extends MdmDomain > {

    abstract String getStereotype()
    abstract String getStereotypeForPreview()
    abstract String getPluralStereotypeForWebsite()

    NhsDataDictionary dataDictionary

    T catalogueItem
    UUID catalogueItemId
    UUID branchId
    String catalogueItemModelId
    String catalogueItemParentId

    String name
    String definition = ""

    Map<String, String> otherProperties = [:]

    Map<NhsDataDictionaryComponent, String> whereUsed = [:]

    List<NhsDDChangeLog> changeLog = []
    String changeLogHeaderText = ""
    String changeLogFooterText = ""

    abstract String calculateShortDescription()

    boolean isRetired() {
        "true" == otherProperties["isRetired"]
    }

    boolean isPreparatory() {
        "true" == otherProperties["isPreparatory"]
    }

    boolean isActivePage() {
        !isRetired() && !isPreparatory()
    }

    String getUin() {
        otherProperties["uin"]
    }

    String getShortDescription() {
        if(otherProperties["shortDescription"] && isActivePage()){
            return otherProperties["shortDescription"]
        } else {
            return calculateShortDescription()
        }
    }

    String getTitleCaseName() {
        otherProperties["titleCaseName"]
    }

    void setShortDescription() {
        String shortDescription = calculateShortDescription()
        if(!shortDescription) {
            log.error("Null short description! ${name}")
            shortDescription = name
        }
        //shortDescription = shortDescription.replaceAll("\\\\r", " ")
        shortDescription = shortDescription.replaceAll("\\s+", " ")
        shortDescription = shortDescription.replaceAll("\\\\n", " ")
        shortDescription = shortDescription.replaceAll("\\\\r", " ")
        otherProperties["shortDescription"] = shortDescription
    }

    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        if(xml.title.size() > 0 && xml.title.text()) {
            this.name = xml.title[0].text().replace("_", " ")
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
        definition = (DDHelperFunctions.parseHtml(xml.definition[0])).replace("\u00a0", " ")

        NhsDataDictionary.METADATA_FIELD_MAPPING.entrySet().each {entry ->
            Node xmlValue = xml[entry.value][0]
            if((!xmlValue || xmlValue.text() == "") && xml."class"[entry.value]) {
                xmlValue = xml."class"[entry.value][0]
            }
            if(xmlValue && xmlValue.text() != "") {
                otherProperties[entry.key] = xmlValue.text()
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
        NhsDataDictionary.aliasFields.each {aliasKey, aliasValue ->

            String alias = otherProperties[aliasKey]
            if(alias) {
                aliases[aliasValue] = alias
            }
        }
        return aliases
    }

    Map<String, String> getUrlReplacements() {
        String ddUrl = this.otherProperties["ddUrl"]

        return [
            (ddUrl) : this.mauroPath
        ]
    }

    String getNameWithoutNonAlphaNumerics() {
        name.replaceAll("[^A-Za-z0-9- ]", "").replace(" ", "_")
    }

    abstract String getMauroPath()

    String getDitaKey() {
        String key = getStereotype().replace(" ", "_") + "_" + getNameWithoutNonAlphaNumerics()
        if(isRetired()) {
            key += "_retired"
        }
        key.toLowerCase()
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

    String getDataDictionaryUrl() {
        return """${NhsDataDictionary.WEBSITE_URL}/${getPluralStereotypeForWebsite()}/${getNameWithoutNonAlphaNumerics().toLowerCase()}.html"""
    }

    Change createChange(
        String type,
        NhsDataDictionaryComponent previousItem,
        StringWriter htmlWriter,
        DitaElement ditaElement = null,
        boolean preferDita = false) {
        String html = htmlWriter.toString()
        new Change(
            changeType: type,
            stereotype: stereotype,
            oldItem: previousItem,
            newItem: this,
            htmlDetail: htmlWriter.toString(),
            ditaDetail: ditaElement ?: HtmlHelper.replaceHtmlWithDita(html),
            preferDitaDetail: ditaElement && preferDita
        )
    }

    List<Change> getChanges(NhsDataDictionaryComponent previousComponent) {
        List<Change> changes = []

        Change descriptionChange = createDescriptionChange(previousComponent)
        if (descriptionChange) {
            changes.add(descriptionChange)
        }

        if (isActivePage()) {
            Change aliasesChange = createAliasesChange(previousComponent)
            if (aliasesChange) {
                changes.add(aliasesChange)
            }
        }

        changes
    }

    Change createDescriptionChange(NhsDataDictionaryComponent previousComponent) {
        boolean isNewItem = previousComponent == null

        StringWriter htmlWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(htmlWriter)

        if (isNewItem) {
            markupBuilder.div {
                div (class: "new") {
                    mkp.yieldUnescaped(this.description)
                }
            }

            Div ditaElement = Div.build(outputClass: "new") {
                div HtmlHelper.replaceHtmlWithDita(this.description)
            }

            return createChange(Change.NEW_TYPE, null, htmlWriter, ditaElement, true)
        }

        // To get accurate description comparison, we have to load all strings as HTML Dom trees and compare them. A simple
        // string comparison won't do anymore - because you could have superfluous whitespace between HTML tags that a string compare will
        // just consider "different" when semantically it isn't. _However_, this HTML comparison is *incredibly* slow across comparing
        // two dictionary branches now! If you want to test it faster, uncomment the line below and remember to put it back before you commit
        //if (description == previousComponent.description) { // DEBUG
        if (DaisyDiffHelper.calculateDifferences(description, previousComponent.description).length == 0) {
            // These descriptions are the same - no change required
            return null
        }

        // For updated descriptions, there could be really complex HTML which really messes up the diff output,
        // particular when HTML tables merge cells together, the diff tags added back don't align correctly.
        // So just ignore this for now!
        boolean currentContainsHtmlTable = DaisyDiffHelper.containsHtmlTable(this.description)
        boolean previousContainsHtmlTable = DaisyDiffHelper.containsHtmlTable(previousComponent.description)
        boolean canDiffContent = !currentContainsHtmlTable && !previousContainsHtmlTable

        String diffHtml = ""
        if (!canDiffContent) {
            log.warn("HTML in ${this.stereotype} '${this.name}' contains table, cannot produce diff")
            diffHtml = this.description
        }
        else {
            diffHtml = DaisyDiffHelper.diff(previousComponent.description, this.description)
        }

        markupBuilder.div {
            if (!canDiffContent) {
                markupBuilder.p(class: "info-message") {
                    mkp.yield("Contains content that cannot be directly compared. This is the current content.")
                }
            }
            markupBuilder.div {
                mkp.yieldUnescaped(diffHtml)
            }
        }

        String changeType = Change.UPDATED_DESCRIPTION_TYPE
        if (isRetired() && !previousComponent.isRetired()) {
            changeType = Change.RETIRED_TYPE
        }

        createChange(changeType, previousComponent, htmlWriter)
    }

    Change createAliasesChange(NhsDataDictionaryComponent previousComponent) {
        Map<String, String> currentAliases = getAliases()
        Map<String, String> previousAliases = previousComponent ? previousComponent.getAliases() : [:]

        if (currentAliases.isEmpty() && previousAliases.isEmpty()) {
            return null
        }

        if (currentAliases.equals(previousAliases)) {
            // Identical lists. If this is a new item, this will never be true so will always include an "Aliases" change
            return null
        }

        StringWriter htmlWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(htmlWriter)

        markupBuilder.div {
            p "This ${getStereotype()} is also known by these names:"
            table(class: "alias-table") {
                thead {
                    markupBuilder.th(width: "34%") {
                        mkp.yield("Context")
                    }
                    markupBuilder.th(width: "66%") {
                        mkp.yield("Alias")
                    }
                }
                tbody {
                    currentAliases.each {context, alias ->
                        if (!previousAliases.containsKey(context) || (previousAliases.containsKey(context) && previousAliases[context] != alias)) {
                            markupBuilder.tr {
                                markupBuilder.td(class: "new") {
                                    mkp.yield(context)
                                }
                                markupBuilder.td(class: "new") {
                                    mkp.yield(alias)
                                }
                            }
                        } else {
                            markupBuilder.tr {
                                td context
                                td alias
                            }
                        }
                    }
                    previousAliases.each {context, alias ->
                        if (!currentAliases.containsKey(context) || (currentAliases.containsKey(context) && currentAliases[context] != alias)) {
                            markupBuilder.tr {
                                markupBuilder.td(class: "deleted") {
                                    mkp.yield(context)
                                }
                                markupBuilder.td(class: "deleted") {
                                    mkp.yield(alias)
                                }
                            }
                        }
                    }
                }
            }
        }

        createChange(Change.ALIASES_TYPE, previousComponent, htmlWriter)
    }

    DitaMap generateMap() {
        DitaMap.build(
                id: getDitaKey()
        ) {
            title getNameWithRetired()
        }
    }

    LocalDate getToDate() {
        if(otherProperties["validTo"]) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return LocalDate.parse(otherProperties["validTo"] as CharSequence, formatter);
        }
        return null
    }

    LocalDate getFromDate() {
        if(otherProperties["validFrom"]) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return LocalDate.parse(otherProperties["validFrom"] as CharSequence, formatter);
        }
        return null
    }

    DictionaryItem getPublishStructure() {
        DictionaryItemState state = isRetired()
            ? DictionaryItemState.RETIRED
            : isPreparatory()
            ? DictionaryItemState.PREPARATORY
            : DictionaryItemState.ACTIVE

        def dictionaryItem = new DictionaryItem(
            catalogueItemId,
            branchId,
            stereotype,
            name,
            state,
            outputClass,
            shortDescription)

        dictionaryItem.addSection(new DescriptionSection(dictionaryItem, definition))

        if (state == DictionaryItemState.ACTIVE) {
            List<AliasesRow> aliasesRows = getAliases().collect {context, alias -> new AliasesRow(context, alias)}
            dictionaryItem.addSection(new AliasesSection(dictionaryItem, aliasesRows))

            // TODO: Where Used table
        }

        // TODO: Change Log table

        dictionaryItem
    }

    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []
        topics.add(descriptionTopic())
        if (isActivePage()) {
            if (getAliases()) {
                topics.add(aliasesTopic())
            }
            if (whereUsed) {
                topics.add(whereUsedTopic())
            }
        }
        topics.add(changeLogTopic())
        return topics
    }

    Topic generateTopic() {
        String titleOutputClass = getOutputClass()
        Topic.build(
            id: getDitaKey()
        ) {
            title (outputClass: titleOutputClass)  {
                text getNameWithRetired()
            }
            shortdesc getShortDescription()
            getWebsiteTopics().each {
                topic it
            }
        }
    }

    Topic descriptionTopic() {
        Topic.build (id: getDitaKey() + "_description") {
            title "Description"
            body {
                if(definition) {
                    div HtmlHelper.replaceHtmlWithDita(definition.replace('<table', '<table class=\"table-striped\"'))
                }
            }
        }
    }

    Topic whereUsedTopic() {
        Topic.build (id: getDitaKey() + "_whereUsed") {
            title "Where Used"
            body {
                simpletable(relColWidth: new SpaceSeparatedStringList (["1*","3*","2*"]), outputClass: "table table-sm table-striped") {
                    stHead (outputClass: "thead-light") {
                        stentry "Type"
                        stentry "Link"
                        stentry "How used"
                    }
                    whereUsed
                        .findAll { !it.key.isRetired() }
                        .sort { it.key.name }
                        .each {component, text ->
                            strow {
                                stentry component.stereotype
                                stentry {
                                    xRef component.calculateXRef()
                                }
                                stentry text
                            }
                        }
                }
            }
        }
    }

    Topic aliasesTopic() {
        Topic.build (id: getDitaKey() + "_aliases") {
            title "Also Known As"
            body {
                p "This ${getStereotype()} is also known by these names:"
                simpletable(relColWidth: new SpaceSeparatedStringList (["1*","2*"]), outputClass: "table table-sm table-striped") {
                    stHead (outputClass: "thead-light") {
                        stentry "Context"
                        stentry "Alias"
                    }
                    getAliases().each {context, alias ->
                        strow {
                            stentry context
                            stentry alias
                        }
                    }
                }
            }
        }
    }

    Topic changeLogTopic() {
        Topic.build(id: getDitaKey() + "_changeLog") {
            title "Change Log"
            body {
                if (changeLog && !changeLog.empty && changeLogHeaderText) {
                    div HtmlHelper.replaceHtmlWithDita(changeLogHeaderText)
                }
                if (changeLog && !changeLog.empty) {
                    simpletable(relColWidth: new SpaceSeparatedStringList (["2*","5*","3*"]), outputClass: "table table-sm table-striped") {
                        stHead (outputClass: "thead-light") {
                            stentry "Change Request"
                            stentry "Change Request Description"
                            stentry "Implementation Date"
                        }
                        changeLog.each { entry ->
                            strow {
                                stentry {
                                    if (entry.referenceUrl) {
                                        xRef getExternalXRef(entry.referenceUrl, entry.reference)
                                    }
                                    else {
                                        txt entry.reference
                                    }
                                }
                                stentry entry.description
                                stentry entry.implementationDate
                            }
                        }
                    }
                }
                if (changeLogFooterText) {
                    div HtmlHelper.replaceHtmlWithDita(changeLogFooterText)
                }
            }
        }
    }

    XRef calculateXRef() {
        XRef.build(
            outputClass: getOutputClass(),
            keyRef: getDitaKey(),
            format: "html"
        ) {
            txt getNameWithRetired()
        }
    }

    XRef getExternalXRef(String url, String text) {
        XRef.build(
            scope: Scope.EXTERNAL,
            format: "html",
            href: url
        ) {
            txt text
        }
    }

    String getOutputClass() {
        String outputClass = getStereotypeForPreview()
        if(isRetired()) {
            outputClass += " retired"
        }

        return outputClass
    }

    void replaceLinksInDefinition(Map<String, NhsDataDictionaryComponent> pathLookup) {
        if(definition) {
            definition = replaceLinksInString(description, pathLookup)
        }
    }

    String replaceLinksInString(String source, Map<String, NhsDataDictionaryComponent> pathLookup) {
        NhsDataDictionary.replaceLinksInStringAndUpdateWhereUsed(source, pathLookup, this)
    }

    static List<String> calculateSentences(String html) {
        Node xml = HtmlHelper.tidyAndConvertToNode(html)
        if(xml.children().find { childNode ->
            childNode instanceof String || childNode.name().toString().toLowerCase() == 'a' // An indicator that there are no paragraphs
        }) {
            return xml.text().split("\\.")
        }

        List<String> response = getNodeSentences(xml)

        response.removeAll {it.trim() == ""}
        return response
    }

    static List<String> getNodeSentences(String str) {
        return str.split("\\.")
    }

    static List<String> getNodeSentences(Node xml) {
        List<String> response = []
        xml.children().each { childNode ->
            if (childNode instanceof String) {
                response.add((String) childNode)
            } else {
                switch (childNode.name().toString().toLowerCase()) {
                    case 'img':
                    case 'br':
                        break
                    case 'ul':
                    case 'table':
                    case 'div':
                        childNode.children().each { child ->
                            response.addAll(getNodeSentences(child))
                        }
                        break
                    case 'p':
                    case 'span':
                    case 'strong':
                    default:
                        response.addAll(childNode.text().split("\\."))
                        break


                }
            }
        }
        return response
    }


    String getFirstSentence(String html = this.getDescription()) {
        getSentence(html, 0)
    }

    String getSentence(String html = this.definition, int i) {
        if(!html) {
            return null
        }
        String sentence = calculateSentences(html)[i]
        if (!sentence) {
            return null
        }
        return tidyShortDescription(sentence) + "."
    }

    String tidyShortDescription(String sentence) {
        if(!sentence) {
            return null
        }
        String response = sentence.replace("_", " ")
        response = response.replaceAll("\\s+", " ")
        return response
    }

    List<String> getWebPath() {
        if(otherProperties["baseUri"]) {
            // This is really for when we're ingesting
            List<String> path = []
            try {
                path.addAll(DDHelperFunctions.getPath(otherProperties["baseUri"], "Messages", ".txaClass20"))
            } catch (Exception e) {
                path.addAll(DDHelperFunctions.getPath(otherProperties["baseUri"], "Web_Site_Content", ".txaClass20"))
            }
            path.removeAll {it.equalsIgnoreCase("Data_Sets")}
            path.removeAll {it.equalsIgnoreCase("Content")}

            if(name.startsWith("CDS") && !(isRetired())) {
                path.add(0, "Commissioning Data Sets")
            }
            if (isRetired()) {
                path.add(0, "Retired")
            }
            path = path.collect {DDHelperFunctions.tidyLabel(it)}
            return path
        } else {
            // otherwise, get the path from the folder hierarchy -
            // TODO this with inheritance
            if(this instanceof NhsDDDataSet) {
                return ((NhsDDDataSet)this).path
            }
        }
    }

    /*
    Helper functions to resolve type checking issues in grails views
     */

    String getCatalogueItemIdAsString() {
        return catalogueItem.id.toString()
    }

    String getCatalogueItemDomainTypeAsString() {
        return catalogueItem.domainType.toString()
    }

    void updateWhereUsed() {

    }


}