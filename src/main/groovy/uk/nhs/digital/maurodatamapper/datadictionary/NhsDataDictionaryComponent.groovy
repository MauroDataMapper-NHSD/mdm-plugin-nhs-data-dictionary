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
import uk.ac.ox.softeng.maurodatamapper.dita.helpers.HtmlHelper
import uk.ac.ox.softeng.maurodatamapper.dita.meta.DitaElement
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList

import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.nhs.digital.maurodatamapper.datadictionary.publish.DaisyDiffHelper
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change
import uk.nhs.digital.maurodatamapper.datadictionary.utils.DDHelperFunctions

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Matcher

@Slf4j
trait NhsDataDictionaryComponent <T extends MdmDomain > {

    abstract String getStereotype()
    abstract String getStereotypeForPreview()
    abstract String getPluralStereotypeForWebsite()

    NhsDataDictionary dataDictionary

    T catalogueItem
    String catalogueItemModelId
    String catalogueItemParentId

    String name
    String definition = ""

    Map<String, String> otherProperties = [:]

    Map<NhsDataDictionaryComponent, String> whereUsed = [:]

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
        if(otherProperties["shortDescription"]){
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
        if(xml.name.size() > 0 && xml.name.text()) {
            this.name = xml.name[0].text().replace("_", " ")
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
        name.replaceAll("[^A-Za-z0-9 ]", "").replace(" ", "_")
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

    List<Change> getChanges(NhsDataDictionaryComponent previousComponent, boolean includeDataSets = false) {
        List<Change> changeList = []
        buildChangeList(changeList, previousComponent, includeDataSets)

        return changeList
    }

    void buildChangeList(List<Change> changes, NhsDataDictionaryComponent previousComponent, boolean includeDataSets) {
        if (!previousComponent) {
            buildComponentDetailsChangeList(changes, null, false)
        }
        else if (isRetired() && !previousComponent.isRetired()) {
            Change retiredChange = createRetiredComponentChange(previousComponent)
            changes.add(retiredChange)
        }
        else if (description != previousComponent.description) {
            buildComponentDetailsChangeList(changes, previousComponent, includeDataSets)
        }
    }

    void buildComponentDetailsChangeList(List<Change> changes, NhsDataDictionaryComponent previousComponent, boolean includeDataSets) {
        Change standardDescriptionChange = createStandardDescriptionChange(previousComponent, includeDataSets)
        if (standardDescriptionChange) {
            changes.add(standardDescriptionChange)
        }

        // Aliases should only be added if the component is new or updated
        Change aliasesChange = createAliasesChange(previousComponent)
        if (aliasesChange) {
            changes.add(aliasesChange)
        }
    }

    Change createStandardDescriptionChange(NhsDataDictionaryComponent previousComponent, boolean includeDataSets) {
        if (!previousComponent) {
            return createNewComponentChange()
        }

        return createUpdatedDescriptionChange(previousComponent, includeDataSets)
    }

    Change createNewComponentChange() {
        StringWriter stringWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(stringWriter)

        markupBuilder.div {
            div (class: "new") {
                mkp.yieldUnescaped(this.description)
            }
        }

        new Change(
            changeType: Change.NEW_TYPE,
            stereotype: stereotype,
            oldItem: null,
            newItem: this,
            htmlDetail: stringWriter.toString(),
            ditaDetail: Div.build(outputClass: "new") {
                div HtmlHelper.replaceHtmlWithDita(this.description)
            }
        )
    }

    Change createRetiredComponentChange(NhsDataDictionaryComponent previousComponent) {
        StringWriter stringWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(stringWriter)

        markupBuilder.div {
            /*div (class: "deleted") {
                mkp.yieldUnescaped(previousComponent.description)
            }
            div (class: "new") {
                mkp.yieldUnescaped(this.description)
            }*/

            mkp.yieldUnescaped(DaisyDiffHelper.diff(previousComponent.description, this.description))
        }

        new Change(
            changeType: Change.RETIRED_TYPE,
            stereotype: stereotype,
            oldItem: previousComponent,
            newItem: this,
            htmlDetail: stringWriter.toString(),
            ditaDetail: Div.build {
                div (outputClass: "deleted") {
                    div HtmlHelper.replaceHtmlWithDita(previousComponent.description)
                    if(previousComponent instanceof NhsDDDataSet) {
                        div HtmlHelper.replaceHtmlWithDita(previousComponent.outputAsHtml(markupBuilder))
                    }
                }
                div (outputClass: "new") {
                    div HtmlHelper.replaceHtmlWithDita(this.description)
                    if(this instanceof NhsDDDataSet) {
                        div HtmlHelper.replaceHtmlWithDita(((NhsDDDataSet)this).outputAsHtml(markupBuilder))
                    }
                }
            }
        )
    }

    Change createUpdatedDescriptionChange(NhsDataDictionaryComponent previousComponent, boolean includeDataSets) {
        StringWriter stringWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(stringWriter)

        markupBuilder.div {
            div {
                mkp.yieldUnescaped(DaisyDiffHelper.diff(previousComponent.description, this.description))
                if(this instanceof NhsDDDataSet && includeDataSets) {
                    mkp.yieldUnescaped(((NhsDDDataSet)this).structureAsHtml)
                }
            }
        }

        new Change(
            changeType: Change.UPDATED_DESCRIPTION_TYPE,
            stereotype: stereotype,
            oldItem: previousComponent,
            newItem: this,
            htmlDetail: stringWriter.toString(),
            ditaDetail: Div.build {
                div (outputClass: "deleted") {
                    div HtmlHelper.replaceHtmlWithDita(previousComponent.description)
                }
                div (outputClass: "new") {
                    div HtmlHelper.replaceHtmlWithDita(this.description)
                }
            }
        )
    }

    Change createAliasesChange(NhsDataDictionaryComponent previousComponent) {
        Map<String, String> currentAliases = getAliases()
        Map<String, String> previousAliases = previousComponent ? previousComponent.getAliases() : [:]

        if (currentAliases.isEmpty() && previousAliases.isEmpty()) {
            return null
        }

        String htmlDetail = createAliasTableChangeHtml(currentAliases, previousAliases)
        DitaElement ditaDetail = createAliasTableChangeDita(currentAliases, previousAliases)

        new Change(
            changeType: Change.ALIASES_TYPE,
            stereotype: stereotype,
            oldItem: previousComponent,
            newItem: this,
            htmlDetail: htmlDetail,
            ditaDetail: ditaDetail,
            preferDitaDetail: true
        )
    }

    String createAliasTableChangeHtml(Map<String, String> currentAliases, Map<String, String> previousAliases) {
        StringWriter stringWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(stringWriter)

        markupBuilder.div {
            p "This ${getStereotype()} is also known by these names:"
            table(class: "alias-table") {
                thead {
                    th "Context"
                    th "Alias"
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

        return stringWriter.toString()
    }

    DitaElement createAliasTableChangeDita(Map<String, String> currentAliases, Map<String, String> previousAliases) {
        Div.build {
            p "This ${getStereotype()} is also known by these names:"
            simpletable(relColWidth: new SpaceSeparatedStringList (["1*","2*"])) {
                stHead {
                    stentry "Context"
                    stentry "Alias"
                }
                currentAliases.each { context, alias ->
                    if (!previousAliases.containsKey(context) || (previousAliases.containsKey(context) && previousAliases[context] != alias)) {
                        strow {
                            stentry(outputClass: "new") {
                                ph context
                            }
                            stentry(outputClass: "new") {
                                ph alias
                            }
                        }
                    }
                    else {
                        strow {
                            stentry {
                                ph context
                            }
                            stentry {
                                ph alias
                            }
                        }
                    }
                }
                previousAliases.each { context, alias ->
                    if (!currentAliases.containsKey(context) || (currentAliases.containsKey(context) && currentAliases[context] != alias)) {
                        strow {
                            stentry(outputClass: "deleted") {
                                ph context
                            }
                            stentry(outputClass: "deleted") {
                                ph context
                            }
                        }
                    }
                }
            }
        }
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

    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []
        topics.add(descriptionTopic())
        if(getAliases()) {
            topics.add(aliasesTopic())
        }
        if(whereUsed) {
            topics.add(whereUsedTopic())
        }
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
                    whereUsed.sort {it.key.name}.each {component, text ->
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


    XRef calculateXRef() {
        XRef.build(
            outputClass: getOutputClass(),
            keyRef: getDitaKey(),
            format: "html"
        ) {
            txt getNameWithRetired()
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
        if(source) {
            Matcher matcher = NhsDataDictionary.pattern.matcher(source)
            while(matcher.find()) {
                NhsDataDictionaryComponent component = pathLookup[matcher.group(1)]

                if(component) {
                    String text = matcher.group(2).replaceAll("_"," ")
                    String replacement = "<a class='${component.getOutputClass()}' href=\"${component.getDitaKey()}\">${text}</a>"
                    source = source.replace(matcher.group(0), replacement)
                    if(this != component) {
                        component.whereUsed[this] = "references in description ${this.name}".toString()

                    }
                } else {
                    log.info("Cannot match component: ${matcher.group(1)}")
                }
            }
        }
        return source
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