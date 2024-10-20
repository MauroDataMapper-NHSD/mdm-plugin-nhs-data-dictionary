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
package uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper

import uk.ac.ox.softeng.maurodatamapper.dita.DitaProject
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Section
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.TopicRef
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Toc
import uk.ac.ox.softeng.maurodatamapper.dita.helpers.HtmlHelper
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList

import groovy.util.logging.Slf4j
import net.lingala.zip4j.ZipFile
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent

import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.regex.Matcher

@Slf4j
class ChangePaperPdfUtility {

    static File generateChangePaper(NhsDataDictionary thisDataDictionary, NhsDataDictionary previousDataDictionary, Path outputPath, boolean includeDataSets = false) {

        DitaProject ditaProject = new DitaProject("NHS Data Model and Dictionary","nhs_data_dictionary")
        ditaProject.useTopicsFolder = false

        Map<String, NhsDataDictionaryComponent> pathLookup = [:]
        thisDataDictionary.getAllComponents().each { it ->
            ditaProject.addExternalKey(it.getDitaKey(), it.getDataDictionaryUrl())
            pathLookup[it.getMauroPath()] = it
        }


        if (previousDataDictionary) {
            previousDataDictionary.getAllComponents().each {it ->
                if (!pathLookup[it.getMauroPath()]) {
                    ditaProject.addExternalKey(it.getDitaKey(), it.getDataDictionaryUrl())
                    pathLookup[it.getMauroPath()] = it
                }
            }
        }
        thisDataDictionary.getAllComponents().each {it ->
            it.replaceLinksInDefinition(pathLookup)
        }
        if (previousDataDictionary) {
            previousDataDictionary.getAllComponents().each {it ->
                it.replaceLinksInDefinition(pathLookup)
            }
        }
        ChangePaper changePaper = new ChangePaper(thisDataDictionary, previousDataDictionary, includeDataSets)

        // The background text could be HTML, so make sure any links inside this are fixed up
        changePaper.background = NhsDataDictionary.replaceLinksInStringAndUpdateWhereUsed(
            changePaper.background,
            pathLookup,
            null)

        Topic summaryOfChangesTopic = Topic.build(id: "summary") {
            title "Summary of Changes"
            body {
                changePaper.stereotypedChanges.each {stereotypedChange ->
                    if(stereotypedChange.changedItems) {
                        section generateSummaryContentSection(stereotypedChange.changedItems, stereotypedChange.stereotypeName)
                    }
                }
            }
        }

        //System.err.println(pathLookup)
        List<Topic> changeTopics = generateChangeTopics(changePaper.stereotypedChanges)
        Topic changesTopic = Topic.build(id: "changes") {
            title "Changes"
            body {

            }
            changeTopics.each {changeTopic ->
                topic changeTopic
            }
        }



        ditaProject.registerTopic("", createBackgroundTopic(changePaper))
        ditaProject.registerTopic("", summaryOfChangesTopic)
        ditaProject.registerTopic("", changesTopic)

        ["background", "summary", "changes"].each { topicId ->
            ditaProject.mainMap.topicRef(TopicRef.build {
                keyRef topicId
                toc Toc.YES
            })
        }

        String ditaOutputDirectory = outputPath.toString() + File.separator + "dita"
        ditaProject.writeToDirectory(Paths.get(ditaOutputDirectory))

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy")
        String date = simpleDateFormat.format(new Date())
        String changePaperType = includeDataSets ? "datasets" : "basic"

        String filename = "change-paper-${changePaper.reference}-${changePaperType}-${date}.zip"

        ZipFile zipFile = new ZipFile(outputPath.toString() + File.separator + filename)
        zipFile.addFolder(new File(ditaOutputDirectory))

        return zipFile.getFile()

    }




    static Topic createBackgroundTopic(ChangePaper changePaper) {

        Topic backgroundTopic = Topic.build(id: "background") {
            title "Background"
            body {
                dl {
                    changePaper.propertiesAsMap().each { key, value ->
                        dlentry {
                            dt key
                            dd {
                                div HtmlHelper.replaceHtmlWithDita(value)
                            }
                        }
                    }
                }
                p {
                    txt "Note: New text is shown with a "
                    b(outputClass: "new") { txt "blue background" }
                    txt ". Deleted text is "
                    b(outputClass: "deleted") {txt "crossed out" }
                    txt ". Retired text is shown "
                    b(outputClass: "retired") { txt "in grey" }
                    txt "."
                }
            }
        }
        return backgroundTopic
    }

//    static String replaceLinksInHtml(String html, Map<String, String> pathLookup) {
//        if(html) {
//            Matcher matcher = NhsDataDictionary.pattern.matcher(html)
//            while(matcher.find()) {
//                String matchedUrl = pathLookup[matcher.group(1)]
//                if(matchedUrl) {
//                    String replacement = "<a href=\"${matchedUrl}\">${matcher.group(2).replaceAll("_"," ")}</a>"
//                    html = html.replace(matcher.group(0), replacement)
//                } else {
//                    log.error("Cannot match: " + matcher.group(0))
//                }
//            }
//        }
//        return html
//    }

    static Section generateSummaryContentSection(List<ChangedItem> changedItems, String stereotype) {
        return Section.build {
            title "${stereotype} Definitions"
            simpletable(relColWidth: new SpaceSeparatedStringList(["70*","30*"])) {
                changedItems.each {changedItem ->
                    strow {
                        stentry {
                            xRef(keyRef: changedItem.dictionaryComponent.getDitaKey()) {
                                txt changedItem.dictionaryComponent.name
                            }
                        }
                        stentry {
                            p changedItem.getSummaryOfChanges()
                        }
                    }
                }
            }
        }
    }

    static List<Topic> generateChangeTopics(StereotypedChange stereotypedChange) {
        List<Topic> topics = []

        stereotypedChange.changedItems.each {changedItem ->
            //String newDefinition = replaceLinksInDescription(changedItem.dictionaryComponent.description, pathLookup)
            //System.err.println(changedItem.dictionaryComponent.name)

            Topic subTopic = Topic.build(id: changedItem.dictionaryComponent.getDitaKey()) {
                title changedItem.dictionaryComponent.name
                shortdesc "Change to ${stereotypedChange.stereotypeName}: ${changedItem.summaryOfChanges}"

                body {
                    changedItem.changes.each {change ->
                        if (change.preferDitaDetail && change.ditaDetail) {
                            div change.ditaDetail
                        }
                        else {
                            div HtmlHelper.replaceHtmlWithDita(change.htmlDetail)
                        }
                    }
                }
            }
            topics.add(subTopic)
        }
        return topics
    }

    static List<Topic> generateChangeTopics(List<StereotypedChange> stereotypedChanges) {

        List<Topic> topics = []
        if(stereotypedChanges && stereotypedChanges.size() != 0) {
            stereotypedChanges.each {stereotypedChange ->
                topics.addAll(generateChangeTopics(stereotypedChange))
            }
        }
        return topics
    }
}

