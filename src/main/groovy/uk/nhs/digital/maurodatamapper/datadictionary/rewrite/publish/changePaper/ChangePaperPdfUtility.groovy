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
package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish.changePaper

import uk.ac.ox.softeng.maurodatamapper.dita.DitaProject
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Section
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Toc
import uk.ac.ox.softeng.maurodatamapper.dita.html.HtmlHelper
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList

import groovy.util.logging.Slf4j
import net.lingala.zip4j.ZipFile
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary

import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.regex.Matcher

@Slf4j
class ChangePaperPdfUtility {

    static File generateChangePaper(NhsDataDictionary thisDataDictionary, NhsDataDictionary previousDataDictionary, Path outputPath) {

        ChangePaper changePaper = new ChangePaper(thisDataDictionary, previousDataDictionary)

        DitaProject ditaProject = new DitaProject().tap {
            title = changePaper.subject
            filename = 'changePaper'
        }
        // ditaProject.addTopic("", createSummaryTopic(dataDictionary))

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

        Map<String, String> pathLookup = [:]
        thisDataDictionary.getAllComponents().each { it ->
            ditaProject.addExternalKey(it.getDitaKey(), it.otherProperties["ddUrl"])
            pathLookup[it.getMauroPath()] = it.getDitaKey()
        }
        List<Topic> changeTopics = generateChangeTopics(changePaper.stereotypedChanges, pathLookup)
        Topic changesTopic = Topic.build(id: "changes") {
            title "Changes"
            body {

            }
            changeTopics.each {changeTopic ->
                topic changeTopic
            }
        }



        ditaProject.addTopic("", createBackgroundTopic(changePaper), Toc.YES)
        ditaProject.addTopic("", summaryOfChangesTopic, Toc.YES)
        ditaProject.addTopic("", changesTopic, Toc.YES)

        String ditaOutputDirectory = outputPath.toString() + File.separator + "dita"
        ditaProject.writeToDirectory(Paths.get(ditaOutputDirectory))

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy")
        String date = simpleDateFormat.format(new Date())

        String filename = changePaper.reference + '-' + date + ".zip"

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


    static String replaceLinksInDescription(String definition, Map<String, String> pathLookup) {
        if(definition) {
            Matcher matcher = NhsDataDictionary.pattern.matcher(definition)
            while(matcher.find()) {
                String matchedUrl = pathLookup[matcher.group(1)]
                if(matchedUrl) {
                    String replacement = "<a href=\"${matchedUrl}\">${matcher.group(2).replaceAll("_"," ")}</a>"
                    definition = definition.replace(matcher.group(0), replacement)
                    // System.err.println("Replacing: " + matcher.group(0) + " with " + replacement)
                }
            }
        }
        return definition
    }


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
                        changedItem.changes.each {change ->
                            stentry {
                                p change.changeType
                            }
                        }
                    }
                }
            }
        }
    }

    static List<Topic> generateChangeTopics(StereotypedChange stereotypedChange, Map<String, String> pathLookup) {
        List<Topic> topics = []
        stereotypedChange.changedItems.each {changedItem ->
            //String newDefinition = replaceLinksInDescription(changedItem.dictionaryComponent.description, pathLookup)

            Topic subTopic = Topic.build(id: changedItem.dictionaryComponent.getDitaKey()) {
                title changedItem.dictionaryComponent.name
                String changeText = changedItem.changes.collect {
                    it.changeType
                }.join(", ")
                shortdesc "Change to ${stereotypedChange.stereotypeName}: ${changeText}"

                changedItem.changes.each {change ->
                    String newChangeDetails = replaceLinksInDescription(change.htmlDetail, pathLookup)
                    body {
                        div HtmlHelper.replaceHtmlWithDita(newChangeDetails)
                    }
                }
            }
            topics.add(subTopic)
        }
        return topics
    }

    static List<Topic> generateChangeTopics(List<StereotypedChange> stereotypedChanges, Map<String, String> pathLookup) {

        List<Topic> topics = []
        if(stereotypedChanges && stereotypedChanges.size() != 0) {
            stereotypedChanges.each {stereotypedChange ->
                topics.addAll(generateChangeTopics(stereotypedChange, pathLookup))
            }
        }
        return topics
    }
}

