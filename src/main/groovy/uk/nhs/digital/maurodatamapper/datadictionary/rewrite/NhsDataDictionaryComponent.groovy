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

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Section
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.XRef
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Format
import uk.ac.ox.softeng.maurodatamapper.dita.html.HtmlHelper
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList

import groovy.xml.MarkupBuilder
import groovy.xml.XmlSlurper
import uk.nhs.digital.maurodatamapper.datadictionary.old.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish.changePaper.Change

import java.util.regex.Matcher

trait NhsDataDictionaryComponent {

    // For parsing short descriptions
    static XmlSlurper xmlSlurper = new XmlSlurper()

    abstract String getStereotype()
    abstract String getStereotypeForPreview()

    NhsDataDictionary dataDictionary

    CatalogueItem catalogueItem
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
        return """${NhsDataDictionary.WEBSITE_URL}/${getStereotypeForPreview()}/${getNameWithoutNonAlphaNumerics()}.html"""
    }

    List<Change> getChanges(NhsDataDictionaryComponent previousComponent) {

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
                div (class: "old") {
                    mkp.yieldUnescaped(previousComponent.description)
                }
                div (class: "new") {
                    mkp.yieldUnescaped(this.description)
                }
            }

            changeList += new Change(
                changeType: "Retired",
                stereotype: stereotype,
                oldItem: previousComponent,
                newItem: this,
                htmlDetail: stringWriter.toString(),
                ditaDetail: Div.build {
                    div (outputClass: "old") {
                        div HtmlHelper.replaceHtmlWithDita(previousComponent.description)
                    }
                    div (outputClass: "new") {
                        div HtmlHelper.replaceHtmlWithDita(this.description)
                    }
                }
            )
        } else if (definition != previousComponent.definition) {
            markupBuilder.div {
                div (class: "old") {
                    mkp.yieldUnescaped(previousComponent.description)
                }
                div (class: "new") {
                    mkp.yieldUnescaped(this.description)
                }
            }
            changeList += new Change(
                changeType: "Updated description",
                stereotype: stereotype,
                oldItem: previousComponent,
                newItem: this,
                htmlDetail: stringWriter.toString(),
                ditaDetail: Div.build {
                    div (outputClass: "old") {
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

    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []
        topics.add(descriptionTopic())
        if(whereUsed) {
            topics.add(whereUsedTopic())
        }
        if(getAliases()) {
            topics.add(aliasesTopic())
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
                div HtmlHelper.replaceHtmlWithDita(definition)
            }
        }
    }

    Topic whereUsedTopic() {
        Topic.build (id: getDitaKey() + "_whereUsed") {
            title "Where Used"
            body {
                simpletable(relColWidth: new SpaceSeparatedStringList (["3*","7*"]), outputClass: "table table-striped table-sm") {
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
                simpletable(relColWidth: new SpaceSeparatedStringList (["1*","2*"]), outputClass: "table table-striped table-sm") {
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
            format: Format.HTML
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


}