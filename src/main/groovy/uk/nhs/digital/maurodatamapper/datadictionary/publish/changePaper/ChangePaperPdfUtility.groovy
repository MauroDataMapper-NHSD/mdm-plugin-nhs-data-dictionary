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
import uk.nhs.digital.maurodatamapper.datadictionary.publish.ItemLinkScanner
import uk.nhs.digital.maurodatamapper.datadictionary.publish.NhsDataDictionaryComponentPathResolver
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishTarget

import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat

@Slf4j
class ChangePaperPdfUtility {

    static File generateChangePaper(
        NhsDataDictionary thisDataDictionary,
        NhsDataDictionary previousDataDictionary,
        Path outputPath,
        boolean includeDataSets = false) {

        DitaProject ditaProject = new DitaProject("NHS Data Model and Dictionary","nhs_data_dictionary")
        ditaProject.useTopicsFolder = false

        NhsDataDictionaryComponentPathResolver pathResolver = new NhsDataDictionaryComponentPathResolver()
        pathResolver.add(thisDataDictionary)

        thisDataDictionary.getAllComponents().each { it ->
            ditaProject.addExternalKey(it.getDitaKey(), it.getDataDictionaryUrl())
        }

        if (previousDataDictionary) {
            previousDataDictionary.getAllComponents().each {it ->
                if (!pathResolver.contains(it.getMauroPath())) {
                    ditaProject.addExternalKey(it.getDitaKey(), it.getDataDictionaryUrl())
                }
            }
        }

        pathResolver.addWhenNotPresent(previousDataDictionary)

        PublishContext publishContext = new PublishContext(PublishTarget.CHANGE_PAPER)
        publishContext.setItemLinkScanner(ItemLinkScanner.createForDitaOutput(pathResolver))

        ChangePaper changePaper = new ChangePaper(
            thisDataDictionary,
            previousDataDictionary,
            includeDataSets)

        // The background text could be HTML, so make sure any links inside this are fixed up
        changePaper.background = publishContext.replaceLinksInString(changePaper.background)

        Topic backgroundTopic = createBackgroundTopic(changePaper)
        Topic summaryOfChangesTopic = createSummaryOfChangesTopic(changePaper)
        Topic changesTopic = createChangesTopic(changePaper, publishContext)

        ditaProject.registerTopic("", backgroundTopic)
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

    static Topic createSummaryOfChangesTopic(ChangePaper changePaper) {
        Topic.build(id: "summary") {
            title "Summary of Changes"
            body {
                changePaper.stereotypedChanges.each {stereotypedChange ->
                    if (stereotypedChange.changedItems) {
                        section generateSummaryContentSection(stereotypedChange.changedItems, stereotypedChange.stereotypeName)
                    }
                }
            }
        }
    }

    static Topic createChangesTopic(ChangePaper changePaper, PublishContext publishContext) {
        List<Topic> changeTopics = generateChangeTopics(changePaper.stereotypedChanges, publishContext)

        Topic.build(id: "changes") {
            title "Changes"
            body {

            }
            changeTopics.each {changeTopic ->
                topic changeTopic
            }
        }
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

    static Section generateSummaryContentSection(List<ChangedItem> changedItems, String stereotype) {
        return Section.build {
            title "${stereotype} Definitions"
            simpletable(relColWidth: new SpaceSeparatedStringList(["70*","30*"])) {
                changedItems.each {changedItem ->
                    strow {
                        stentry {
                            xRef(keyRef: changedItem.publishStructure.xrefId) {
                                txt changedItem.publishStructure.name
                            }
                        }
                        stentry {
                            p changedItem.publishStructure.description
                        }
                    }
                }
            }
        }
    }

    static List<Topic> generateChangeTopics(StereotypedChange stereotypedChange, PublishContext publishContext) {
        List<Topic> topics = []

        stereotypedChange.changedItems.each {changedItem ->
            Topic subTopic = changedItem.publishStructure.generateDita(publishContext)
            topics.add(subTopic)
        }
        return topics
    }

    static List<Topic> generateChangeTopics(List<StereotypedChange> stereotypedChanges, PublishContext publishContext) {
        List<Topic> topics = []
        if (stereotypedChanges && stereotypedChanges.size() != 0) {
            stereotypedChanges.each {stereotypedChange ->
                topics.addAll(generateChangeTopics(stereotypedChange, publishContext))
            }
        }

        return topics
    }
}

