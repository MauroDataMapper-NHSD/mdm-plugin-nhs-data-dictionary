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

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.DitaMap
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.XRef
import uk.ac.ox.softeng.maurodatamapper.dita.html.HtmlHelper
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList

import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.nhs.digital.maurodatamapper.datadictionary.old.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish.DaisyDiffHelper
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish.changePaper.Change

import java.util.regex.Matcher

@Slf4j
trait NhsDataDictionaryComponent <T extends MdmDomain> {

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

    HashSet<NhsDataDictionaryComponent> whereUsed = []

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

    String getDataDictionaryUrl() {
        return """${NhsDataDictionary.WEBSITE_URL}/${getPluralStereotypeForWebsite()}/${getNameWithoutNonAlphaNumerics().toLowerCase()}.html"""
    }

    List<Change> getChanges(NhsDataDictionaryComponent previousComponent, boolean includeDataSets = false) {

        StringWriter stringWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(stringWriter)


        List<Change> changeList = []
        if (!previousComponent) {
            markupBuilder.div {
                div (class: "new") {
                    mkp.yieldUnescaped(this.description)
                }
            }

            changeList += new Change(
                changeType: "New",
                stereotype: stereotype,
                oldItem: null,
                newItem: this,
                htmlDetail: stringWriter.toString(),
                ditaDetail: Div.build(outputClass: "new") {
                    div HtmlHelper.replaceHtmlWithDita(this.description)
                }
            )
        } else if(isRetired() && !previousComponent.isRetired()) {

            markupBuilder.div {
                /*div (class: "deleted") {
                    mkp.yieldUnescaped(previousComponent.description)
                }
                div (class: "new") {
                    mkp.yieldUnescaped(this.description)
                }*/

                div {
                    mkp.yieldUnescaped(DaisyDiffHelper.diff(previousComponent.description, this.description))
                }


            }

            changeList += new Change(
                changeType: "Retired",
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
                            div HtmlHelper.replaceHtmlWithDita(this.outputAsHtml(markupBuilder))
                        }
                    }
                }
            )
        } else if (description != previousComponent.description) {
            markupBuilder.div {
                div {
                    mkp.yieldUnescaped(DaisyDiffHelper.diff(previousComponent.description, this.description))
                    if(this instanceof NhsDDDataSet && includeDataSets) {
                        mkp.yieldUnescaped(((NhsDDDataSet)this).structureAsHtml)
                    }
                }
            }
            changeList += new Change(
                changeType: "Updated description",
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
        return changeList
    }

    DitaMap generateMap() {
        DitaMap.build(
                id: getDitaKey()
        ) {
            title getNameWithRetired()
        }
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
        Topic.build(
            id: getDitaKey()
        ) {
            title getNameWithRetired()
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
                    div HtmlHelper.replaceHtmlWithDita(definition)
                }
            }
        }
    }

    Topic whereUsedTopic() {
        Topic.build (id: getDitaKey() + "_whereUsed") {
            title "Where Used"
            body {
                simpletable(relColWidth: new SpaceSeparatedStringList (["3*","7*"]), outputClass: "table table-sm") {
                    stHead (outputClass: "thead-light") {
                        stentry "Type"
                        stentry "Link"
                    }
                    whereUsed.sort {it.name}.each {component ->
                        strow {
                            stentry component.stereotype
                            stentry {
                                xRef component.calculateXRef()
                            }
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
                simpletable(relColWidth: new SpaceSeparatedStringList (["1*","2*"]), outputClass: "table table-sm") {
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
                    // System.err.println("Replacing: " + matcher.group(0) + " with " + replacement)
                    if(this != component) {
                        component.whereUsed.add(this)
                    }
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
        List<String> path = []
        try {
            path.addAll(DDHelperFunctions.getPath(otherProperties["baseUri"], "Messages", ".txaClass20"))
        } catch (Exception e) {
            path.addAll(DDHelperFunctions.getPath(otherProperties["baseUri"], "Web_Site_Content", ".txaClass20"))
        }
        path.removeAll {it.equalsIgnoreCase("Data_Sets")}
        path.removeAll {it.equalsIgnoreCase("Content")}

        if(name.startsWith("CDS")) {
            path.add(0, "Commissioning Data Sets")
        }
        if (isRetired()) {
            path.add(0, "Retired")
        }
        path = path.collect {DDHelperFunctions.tidyLabel(it)}
        return path
    }



}