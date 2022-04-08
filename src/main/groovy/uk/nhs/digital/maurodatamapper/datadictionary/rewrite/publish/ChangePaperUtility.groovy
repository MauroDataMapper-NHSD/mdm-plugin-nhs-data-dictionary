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
package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish

import uk.ac.ox.softeng.maurodatamapper.dita.DitaProject
import uk.ac.ox.softeng.maurodatamapper.dita.elements.Body
import uk.ac.ox.softeng.maurodatamapper.dita.elements.ShortDesc
import uk.ac.ox.softeng.maurodatamapper.dita.elements.Title
import uk.ac.ox.softeng.maurodatamapper.dita.elements.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Toc
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList

import groovy.util.logging.Slf4j
import net.lingala.zip4j.ZipFile
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.regex.Matcher

@Slf4j
class ChangePaperUtility {

    static class Change {
        String changeType
        String stereotype
        NhsDataDictionaryComponent oldItem
        NhsDataDictionaryComponent newItem
        Change() { }

        String changeText(String stereotype) {
            if(changeType == "New") {
                return "New " + stereotype
            }
            if(changeType == "Retired") {
                return "Changed status to retired"
            }
            if(changeType == "Description") {
                return "Changed description"
            }
        }

    }

    List<Change> calculateChanges(NhsDataDictionary thisDataDictionary, NhsDataDictionary previousDataDictionary) {
        List<Change> changes = []
        changes.addAll(compareMaps(thisDataDictionary.businessDefinitions, previousDataDictionary.businessDefinitions, "NHS Business Definition"))
        changes.addAll(compareMaps(thisDataDictionary.supportingInformation, previousDataDictionary.supportingInformation, "Supporting Information"))
        changes.addAll(compareMaps(thisDataDictionary.attributes, previousDataDictionary.attributes, "Attribute"))
        changes.addAll(compareMaps(thisDataDictionary.elements, previousDataDictionary.elements, "Data Element"))
        changes.addAll(compareMaps(thisDataDictionary.classes, previousDataDictionary.classes, "Class"))
        changes.addAll(compareMaps(thisDataDictionary.dataSets, previousDataDictionary.dataSets, "Data Set"))
        changes.addAll(compareMaps(thisDataDictionary.dataSetConstraints, previousDataDictionary.dataSetConstraints, "Data Set Constraint"))


        return changes


    }




    static File generateChangePaper(NhsDataDictionary thisDataDictionary, NhsDataDictionary previousDataDictionary, Path outputPath) {

        Map<String, String> backgroundDetails = getBackgroundDetails(thisDataDictionary)

        DitaProject ditaProject = new DitaProject().tap {
            title = backgroundDetails['Subject']
            filename = 'changePaper'
        }
        ditaProject.addTopic("", createBackgroundTopic(backgroundDetails))
        // ditaProject.addTopic("", createSummaryTopic(dataDictionary))

        Topic summaryOfChangesTopic = new Topic(id: "summary", title: new Title("Summary of Changes"))
        Topic changesTopic = new Topic(id: "changes", title: new Title("Changes"), body: new Body("<p></p>"))

        ditaProject.addTopic("", summaryOfChangesTopic)
        ditaProject.addTopic("", changesTopic)

        Closure summaryContent = genera

        Map<String, String> pathLookup = [:]

        thisDataDictionary.getAllComponents().each { it ->
            ditaProject.addExternalKey(it.getDitaKey(), it.otherProperties["ddUrl"])
            pathLookup[it.getMauroPath()] = it.getDitaKey()
        }




        summaryContent >>= generateChanges(thisDataDictionary.businessDefinitions, previousDataDictionary.businessDefinitions,
                                         "NHS Business Definition", changesTopic, pathLookup)
        summaryContent >>= generateChanges(thisDataDictionary.supportingInformation, previousDataDictionary.supportingInformation,
                                         "Supporting Information", changesTopic, pathLookup)
        summaryContent >>= generateChanges(thisDataDictionary.attributes, previousDataDictionary.attributes,
                                         "Attribute", changesTopic, pathLookup)
        summaryContent >>= generateChanges(thisDataDictionary.elements, previousDataDictionary.elements,
                                         "Data Element", changesTopic, pathLookup)
        summaryContent >>= generateChanges(thisDataDictionary.classes, previousDataDictionary.classes,
                                         "Class", changesTopic, pathLookup)
        summaryContent >>= generateChanges(thisDataDictionary.dataSets, previousDataDictionary.dataSets,
                                         "Data Set", changesTopic, pathLookup)
        summaryContent >>= generateChanges(thisDataDictionary.dataSetConstraints, previousDataDictionary.dataSetConstraints,
                                         "Data Set Constraint", changesTopic, pathLookup)



        summaryOfChangesTopic.body = new Body(summaryContent)
        String ditaOutputDirectory = outputPath.toString() + File.separator + "dita"
        ditaProject.writeToDirectory(ditaOutputDirectory)

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy")
        String date = simpleDateFormat.format(new Date())

        String filename = backgroundDetails['Reference'] + '-' + date + ".zip"

        ZipFile zipFile = new ZipFile(outputPath.toString() + File.separator + filename)
        zipFile.addFolder(new File(ditaOutputDirectory))

        return zipFile.getFile()

    }


    static Map<String, String> getBackgroundDetails(NhsDataDictionary nhsDataDictionary) {
        Map<String, String> response = [:]

        response['Reference'] = nhsDataDictionary.workItemDetails['reference'] ?: 'CRXXXX'
        response['Type'] = nhsDataDictionary.workItemDetails['type'] ?: 'Change Request'
        response['Version no.'] = nhsDataDictionary.workItemDetails['versionNo'] ?: '1.0'
        response['Subject'] = nhsDataDictionary.workItemDetails['subject'] ?: 'NHS England and NHS Improvement'
        response['Effective date'] = nhsDataDictionary.workItemDetails['effectiveDate'] ?: 'Immediate'
        response['Reason for change'] = nhsDataDictionary.workItemDetails['reasonForChange'] ?: 'Change to definitions'
        response['Publication Date'] = new Date().toString()
        response['Background'] = nhsDataDictionary.workItemDetails['backgroundText'] ?: backgroundText.toString()
        response['Sponsor'] = nhsDataDictionary.workItemDetails['sponsor'] ?: 'NHS Digital'

        return response
    }


    static def createBackgroundTopic(Map<String, String> backgroundProperties) {

        Topic backgroundTopic = new Topic(id: "background", title: new Title("Background"))

        def backgroundContent = {
            dl {
                backgroundProperties.each { key, value ->
                    dlentry {
                        dt key
                        dd {
                            mkp.yieldUnescaped Html.tidyAndClean(value).toString()

                        }
                    }
                }
            }
            p {
                mkp.yield "Note: New text is shown with a "
                b(outputclass: "new", "blue background")
                mkp.yield ". Deleted text is "
                b(outputclass: "deleted", "crossed out")
                mkp.yield ". Retired text is shown "
                b(outputclass: "retired", "in grey")
                mkp.yield "."
            }
        }

        backgroundTopic.body = new Body(backgroundContent)
        return backgroundTopic


    }


    static def createBackgroundTopic(NhsDataDictionary dataDictionary) {

        //return summaryOfChangesTopic
    }


    static def backgroundText = {
        p "This Change Request adds the ???? Data Set and supporting definitions to the NHS Data Model and Dictionary to support the Information Standard."
        p "A short demonstration is available which describes \"How to Read an NHS Data Model and Dictionary Change Request\", in an easy to " +
          "understand screen capture including a voice over and readable captions. This demonstration can be viewed at: https://datadictionary.nhs" +
          ".uk/elearning/change_request/index.html."
        p {
            "Note: if the web page does not open, please copy the link and paste into the web browser. A guide to how to use the demonstration can " +
            "be found at: "
            a (href: "https://datadictionary.nhs.uk/help/demonstrations.html", "Demonstrations")
        }
    }

    static String replaceLinksInDescription(String definition, Map<String, String> pathLookup) {
        if(definition) {
            Matcher matcher = NhsDataDictionary.pattern.matcher(definition)
            while(matcher.find()) {
                String matchedUrl = pathLookup[matcher.group(1)]
                if(matchedUrl) {
                    String replacement = "<xref keyref=\"${matchedUrl}\">${matcher.group(2).replaceAll("_"," ")}</xref>"
                    definition = definition.replace(matcher.group(0), replacement)
                    // System.err.println("Replacing: " + matcher.group(0) + " with " + replacement)
                }
            }
        }
        return definition
    }

    static List<Change> compareMaps(Map<String, NhsDataDictionaryComponent> newList, Map<String, NhsDataDictionaryComponent> oldList, String stereotype) {
        List<Change> changes = []
        newList.sort{ it.key }.each {name, component ->
            NhsDataDictionaryComponent previousComponent = oldList[name]
            if (!previousComponent) {
                changes += new Change(
                    changeType: "New",
                    stereotype: stereotype,
                    oldItem: null,
                    newItem: component
                )
            } else if(component.isRetired() && !previousComponent.isRetired()) {
                changes += new Change(
                    changeType: "Retired",
                    stereotype: stereotype,
                    oldItem: previousComponent,
                    newItem: component
                )
            } else if (component.definition != previousComponent.definition) {
                changes += new Change(
                    changeType: "Description",
                    stereotype: stereotype,
                    oldItem: previousComponent,
                    newItem: component
                )
            }
        }
        return changes
    }

    static Closure generateSummaryContent(List<Change> changes, String stereotype) {
        return {
            section {
                title "${stereotype} Definitions"
                simpletable(relcolwidth: "70* 30*") {
                    changes.each {change ->
                        strow {
                            stentry {
                                xref("keyref": change.newItem.getDitaKey(), change.newItem.name)
                            }
                            stentry change.changeText(stereotype)
                        }
                    }
                }
            }
        }
    }

    static Topic generateChangeTopic(Change change, Map<String, String> pathLookup) {
        Topic subTopic = new Topic(id: change.newItem.getDitaKey(), title: new Title(change.newItem.name), toc: Toc.NO)
        String newDefinition = replaceLinksInDescription(change.newItem.definition, pathLookup)

        subTopic.shortDesc = new ShortDesc("Change to ${change.stereotype}: ${change.changeText(change.stereotype)}".toString())
        if(change.changeType == "Description") {
            String oldDefinition = replaceLinksInDescription(change.oldItem.definition, pathLookup)
            subTopic.body = new Body("<div outputclass=\"deleted\">${Html.tidyAndClean(oldDefinition)}</div>".toString() +
                                                       "<div outputclass=\"new\">${Html.tidyAndClean(newDefinition)}</div>".toString())
        } else {
            if(newDefinition) {
                subTopic.body = new Body("<div>${Html.tidyAndClean(newDefinition)}</div>".toString())
            } else {
                subTopic.body = new Body("<p><em>No description</em></p>")
            }
        }

        return subTopic

    }

    static Closure generateChangeTopics(List<Change> changes,
                                Topic changesTopic,
                                Map<String, String> pathLookup) {

        if(changes && changes.size() != 0) {
            changes.each {change ->
                changesTopic.subTopics.add(generateChangeTopic(change, pathLookup))
            }
        }
        return {}
    }

    static Closure generateSummaryTopic(List<Change> changes) {
        Map<String, List<Change>> stereotypedChanges = [:]

        changes.each { change ->
            List<Change> changesForStereotype = stereotypedChanges[change.stereotype]
            if(!changesForStereotype) {
                changesForStereotype = [change]
                stereotypedChanges[change.stereotype] = changesForStereotype

            } else {
                changesForStereotype.add(change)
            }
        }

        Closure summaryTopic = {}

        stereotypedChanges.each { stereotype, changesForStereotype ->
            summaryTopic >>= generateSummaryContent(changesForStereotype, stereotype)
        }

        return summaryTopic




    }



    static String unescape(String input) {
        return input.replace('&lt;', '<').replace('&gt;', '>')
    }


}
