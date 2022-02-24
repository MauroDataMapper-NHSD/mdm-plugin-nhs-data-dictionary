package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish

import uk.ac.ox.softeng.maurodatamapper.dita.DitaProject
import uk.ac.ox.softeng.maurodatamapper.dita.elements.Body
import uk.ac.ox.softeng.maurodatamapper.dita.elements.ShortDesc
import uk.ac.ox.softeng.maurodatamapper.dita.elements.Title
import uk.ac.ox.softeng.maurodatamapper.dita.elements.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Toc
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList

import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

import java.util.regex.Matcher

@Slf4j
class ChangePaperUtility {

    static class Change {
        String changeType
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




    static void generateChangePaper(NhsDataDictionary thisDataDictionary, NhsDataDictionary previousDataDictionary, String outputPath) {

        DitaProject ditaProject = new DitaProject().tap {
            title = "Example Change Paper"
            filename = "changePaper"
        }
        ditaProject.addTopic("", createBackgroundTopic())
        // ditaProject.addTopic("", createSummaryTopic(dataDictionary))

        Topic summaryOfChangesTopic = new Topic(id: "summary", title: new Title("Summary of Changes"))
        Topic changesTopic = new Topic(id: "changes", title: new Title("Changes"), body: new Body("<p></p>"))

        ditaProject.addTopic("", summaryOfChangesTopic)
        ditaProject.addTopic("", changesTopic)

        Closure summaryContent = {}

        Map<String, String> pathLookup = [:]

        thisDataDictionary.getAllComponents().each { it ->
            ditaProject.addExternalKey(it.getDitaKey(), it.otherProperties["ddUrl"])
            pathLookup[it.getMauroPath()] = it.getDitaKey()
        }

        summaryContent >>= generateChanges(thisDataDictionary.businessDefinitions, previousDataDictionary.businessDefinitions,
                                         "Business Definition", changesTopic, pathLookup)
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
        summaryContent >>= generateChanges(thisDataDictionary.xmlSchemaConstraints, previousDataDictionary.xmlSchemaConstraints,
                                         "XMl Schema Constraint", changesTopic, pathLookup)



        summaryOfChangesTopic.body = new Body(summaryContent)
        ditaProject.writeToDirectory(outputPath)

    }

    static def createBackgroundTopic() {

        Topic backgroundTopic = new Topic(id: "background", title: new Title("Background"))

        Map<String, String> properties = [
            "Reference": "1828",
            "Version No": "1.0",
            "Subject": "NHS England and NHS Improvement",
            "Effective Date": "Immediate",
            "Reason For Change": "Change to Definitions",
            "Publication Date": "3rd January 2021",
            "Background": backgroundText,
            "Sponsor": "Nicholas Oughtibridge, Head of Clinical Data Architecture, NHS Digital"
        ]

        def backgroundContent = {
            dl {
                properties.each { key, value ->
                    dlentry {
                        dt key
                        dd value
                    }
                }
            }
            p  {
                "Note: New text is shown with a "
                span (class: "new", "blue background")
                ". Deleted text is "
                span (class: "deleted", "crossed out")
                ". Retired text is shown "
                span (class: "retired", "in grey")
                "."
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

    static List<Change> compareMaps(Map<String, NhsDataDictionaryComponent> newList, Map<String, NhsDataDictionaryComponent> oldList) {
        List<Change> changes = []
        newList.sort{ it.key }.each {name, component ->
            NhsDataDictionaryComponent previousComponent = oldList[name]
            if (!previousComponent) {
                changes += new Change(
                    changeType: "New",
                    oldItem: null,
                    newItem: component
                )
            } else if(component.isRetired() && !previousComponent.isRetired()) {
                changes += new Change(
                    changeType: "Retired",
                    oldItem: previousComponent,
                    newItem: component
                )
            } else if (component.definition != previousComponent.definition) {
                changes += new Change(
                    changeType: "Description",
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

    static Topic generateChangeTopic(Change change, Map<String, String> pathLookup, String stereotype ) {
        Topic subTopic = new Topic(id: change.newItem.getDitaKey(), title: new Title(change.newItem.name), toc: Toc.NO)
        String newDefinition = replaceLinksInDescription(change.newItem.definition, pathLookup)

        subTopic.shortDesc = new ShortDesc("Change to ${stereotype}: ${change.changeText(stereotype)}".toString())
        if(change.changeType == "Description") {
            String oldDefinition = replaceLinksInDescription(change.oldItem.definition, pathLookup)
            subTopic.body = new Body("<div outputclass=\"deleted\">${Html.tidyAndClean(oldDefinition)}</div>".toString() +
                                                       "<div outputclass=\"new\">${Html.tidyAndClean(newDefinition)}</div>".toString())
        } else {
            subTopic.body = new Body("<div>${Html.tidyAndClean(newDefinition)}</div>".toString())
        }

        return subTopic

    }

    static Closure generateChanges(Map<String, NhsDataDictionaryComponent> newMap,
                                Map<String, NhsDataDictionaryComponent> oldMap,
                                String stereotype,
                                Topic changesTopic,
                                Map<String, String> pathLookup) {
        List<Change> changes = compareMaps(newMap, oldMap)

        if(changes && changes.size() != 0) {
            changes.each {change ->
                changesTopic.subTopics.add(generateChangeTopic(change, pathLookup, stereotype))
            }
            return generateSummaryContent(changes, stereotype)
        }
        return {}
    }


}
