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

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.meta.DitaElement

import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic

import uk.nhs.digital.maurodatamapper.datadictionary.datasets.output.html.CDSDataSetToHtml
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.output.html.OtherDataSetToHtml
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change

@Slf4j
class NhsDDDataSet implements NhsDataDictionaryComponent <DataModel> {



    @Override
    String getStereotype() {
        "Data Set"
    }

    @Override
    String getStereotypeForPreview() {
        "dataSet"
    }


    @Override
    String getPluralStereotypeForWebsite() {
        "data_sets"
    }


    List<String> path = []
    String definitionAsXml
    String overviewPageUrl
    String htmlStructure

    boolean isCDS

    List<NhsDDDataSetClass> dataSetClasses = []

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)
        NhsDDWebPage explanatoryWebPage = dataDictionary.webPagesByUin[xml.explanatoryPage.text()]
        if(explanatoryWebPage) {
            definition = explanatoryWebPage.definition
        } else {
            if(isRetired()) {
                log.info("Cannot find explanatory page for dataset: {}", name)
            } else {
                log.warn("Cannot find explanatory page for dataset: {}", name)
            }
        }
        path = getWebPath()
        path.removeLast()
        definitionAsXml = XmlUtil.serialize(xml.definition[0])
        otherProperties["approvingOrganisation"] = "Data Alliance Partnership Board (DAPB)"
    }

    @Override
    String calculateShortDescription() {
        if(!definition || definition == "") {
            return name
        }
        if(isPreparatory()) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            List<String> aliases = [name]
            aliases.addAll(getAliases().values())
            if (path && !path.empty) {
                aliases.add(path.last())
            }

            try {

                List<String> allSentences = calculateSentences(definition)
                if(isRetired()) {
                    return allSentences[0]
                } else {
                    return allSentences.find {sentence ->
                        aliases.find {alias ->
                            sentence.contains(alias)
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace()
                log.error("Couldn't parse: " + definition)
                return name
            }
        }
    }

    @Override
    String getXmlNodeName() {
        "DDDataSet"
    }



    String getMauroPath() {
        "dm:${name}"
    }

    @Override
    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []
        topics.add(descriptionTopic())

        if (isActivePage()) {
            if (getDataSetClasses()) {
                topics.add(specificationTopic())
            }
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

    Topic specificationTopic() {
        log.info("Generating specification for: ${name}")
        Topic.build {
            title "Specification"
            id getDitaKey() + "_specification"
            body {
                getDataSetClasses().sort { it.webOrder }.each { dataSetClass ->
                    if (useCdsClassRender()) {
                        div dataSetClass.outputCDSClassAsDita(dataDictionary)
                    } else {
                        div dataSetClass.outputClassAsDita(dataDictionary)
                    }
                }
            }
        }
    }



    String getStructureAsHtml() {
        if (this.dataSetClasses.empty) {
            return ""
        }

        StringWriter stringWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(stringWriter)
        markupBuilder.setEscapeAttributes(false)
        markupBuilder.setDoubleQuotes(true)
        if(useCdsClassRender()) {
            new CDSDataSetToHtml(markupBuilder).outputAsCdsHtml(this)
        } else {
            new OtherDataSetToHtml(markupBuilder).outputAsHtml(this)
        }
        return stringWriter.toString().replaceAll(">\\s+<", "><").trim()
    }

    List<String> getDitaFolderPath() {
        webPath.collect {
            it.replaceAll("[^A-Za-z0-9 ]", "").replace(" ", "_")
        }
    }


    Set<NhsDDDataSetElement> getAllElements() {
        Set<NhsDDDataSetElement> dataSetElements = [] as Set<NhsDDDataSetElement>
        dataSetClasses.each {dataSetClass ->
            dataSetElements.addAll(dataSetClass.getAllElements())
        }
        return dataSetElements
    }

    @Override
    List<Change> getChanges(NhsDataDictionaryComponent previousComponent) {
        List<Change> changes = []

        Change descriptionChange = createDescriptionChange(previousComponent)
        if (descriptionChange) {
            changes.add(descriptionChange)
        }

        if (isActivePage()) {
            Change specificationChange = createSpecificationChange(previousComponent as NhsDDDataSet)
            if (specificationChange) {
                changes.add(specificationChange)
            }

            Change aliasesChange = createAliasesChange(previousComponent)
            if (aliasesChange) {
                changes.add(aliasesChange)
            }
        }

        changes
    }

    //    @Override
//    void buildComponentDetailsChangeList(List<Change> changes, NhsDataDictionaryComponent previousComponent, boolean includeDataSets) {
//        Change standardDescriptionChange = createStandardDescriptionChange(previousComponent, includeDataSets)
//        if (standardDescriptionChange) {
//            changes.add(standardDescriptionChange)
//        }
//
//        // Data set tables should only be included if specifically requested
//        if (includeDataSets) {
//            Change specificationChange = createSpecificationChange(previousComponent as NhsDDDataSet)
//            if (specificationChange) {
//                changes.add(specificationChange)
//            }
//        }
//
//        // Aliases should only be added if the component is new or updated
//        Change aliasesChange = createAliasesChange(previousComponent)
//        if (aliasesChange) {
//            changes.add(aliasesChange)
//        }
//    }

    Change createSpecificationChange(NhsDDDataSet previousDataSet) {
        String currentHtml = this.structureAsHtml
        String previousHtml = previousDataSet?.structureAsHtml ?: ""

        if (currentHtml == previousHtml) {
            // Identical specs. If this is a new item, this will never be true so will always include a "Specification" change
            return null
        }

        // TODO: show previous as well?
        // Showing a full comparison of the data set spec is too complicated right now, just have to show the current specification
        StringWriter htmlWriter = new StringWriter()
        htmlWriter.write(currentHtml)

        DitaElement ditaElement = this.createSpecificationChangeDita()

        createChange(Change.SPECIFICATION_TYPE, previousDataSet, htmlWriter, ditaElement, true)
    }

    DitaElement createSpecificationChangeDita() {
        Div.build {
            getDataSetClasses().sort { it.webOrder }.each { dataSetClass ->
                if (useCdsClassRender()) {
                    div dataSetClass.outputCDSClassAsDita(dataDictionary)
                } else {
                    div dataSetClass.outputClassAsDita(dataDictionary)
                }
            }
        }
    }

    boolean useCdsClassRender() {
        name.startsWith('CDS') || name.startsWith('ECDS') || name.startsWith('Emergency Care Data Set')
    }
}
