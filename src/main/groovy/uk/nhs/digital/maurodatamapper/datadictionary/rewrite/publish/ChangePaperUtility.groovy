package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish

import uk.ac.ox.softeng.maurodatamapper.dita.DitaProject
import uk.ac.ox.softeng.maurodatamapper.dita.elements.Body
import uk.ac.ox.softeng.maurodatamapper.dita.elements.Title
import uk.ac.ox.softeng.maurodatamapper.dita.elements.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Toc
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList

import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary

import java.util.regex.Matcher

@Slf4j
class ChangePaperUtility {


    static void generateChangePaper(NhsDataDictionary dataDictionary, String outputPath) {

        DitaProject ditaProject = new DitaProject().tap {
            title = "Example Change Paper"
            filename = "changePaper"
        }
        ditaProject.addTopic("", createBackgroundTopic())
        // ditaProject.addTopic("", createSummaryTopic(dataDictionary))

        Topic summaryOfChangesTopic = new Topic(id: "summary", title: new Title("Summary of Changes"))
        Topic changesTopic = new Topic(id: "changes", title: new Title("Changes"), body: new Body("<p>Changes</p>"))

        ditaProject.addTopic("", summaryOfChangesTopic)
        ditaProject.addTopic("", changesTopic)

        Closure summaryContent = {}

        Map<String, String> pathLookup = [:]

        dataDictionary.getAllComponents().each { it ->
            ditaProject.addExternalKey(it.getDitaKey(), it.otherProperties["ddUrl"])
            pathLookup[it.getMauroPath()] = it.getDitaKey()
        }


        //System.err.println(pathLookup)

        dataDictionary.getAllComponents().sort{it.name}.eachWithIndex { component, index ->
            if(index % 100 == 0) {
                String topicId = component.getDitaKey()

                summaryContent >>= {
                    strow {
                        stentry {
                            xref ("keyref": topicId, component.name)
                        }
                        stentry "Changed Description"
                    }
                }

                Topic subTopic = new Topic(id: topicId, title: new Title(component.name), toc: Toc.NO)

                String definition = component.definition
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


                subTopic.body = new Body(uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html.tidyAndClean("<p>${definition}</p>").toString())

                changesTopic.subTopics.add(subTopic)
                SpaceSeparatedStringList keys = new SpaceSeparatedStringList()
                keys.add(topicId)
                //ditaMap.keyDefs.add(new KeyDef(keys: keys, href:"changes.dita"))
            }
        }

        summaryOfChangesTopic.body = new Body({
                                              simpletable(relcolwidth: "70* 30*") {
                                                 owner.with summaryContent
                                            } })
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

        def backgroundContent = { dl {
            properties.each { key, value ->
                dlentry {
                    dt key
                    dd value
                }
            }
        } }

        backgroundTopic.body = new Body(backgroundContent)
        return backgroundTopic


    }


    static def createBackgroundTopic(NhsDataDictionary dataDictionary) {

        //return summaryOfChangesTopic
    }


    static def backgroundText = {
        p "NHS England and NHS Improvement have worked together since 1 April 2019 and are now known as a single organisation."
        p "The NHS England and NHS Improvement websites have now merged; therefore changes are required to the NHS Data Model and Dictionary to support the change"
        p "This Data Dictionary Change Notice (DDCN):"
        ul {
            li "Updates the NHS England NHS Business Definition to create a single definition for NHS England and NHS Improvement"
            li "Retires the NHS Improvement NHS Business Definition"
            li "Updates all items that reference NHS England and NHS Improvement to reflect the change"
        }
    }


}
