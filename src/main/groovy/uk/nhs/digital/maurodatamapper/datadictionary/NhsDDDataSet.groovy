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
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import org.apache.commons.lang3.StringUtils
import groovy.xml.XmlUtil
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic

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
            aliases.add(path.last())

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
        String mauroPath = StringUtils.join(path.collect{"fo:${it}"}, "|")

        mauroPath += "dm:${name}"
        return mauroPath
    }

    @Override
    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []
        topics.add(descriptionTopic())

        if(!isRetired() && getDataSetClasses()) {
            topics.add(specificationTopic())
        }
        if(getAliases()) {
            topics.add(aliasesTopic())
        }
        if(whereUsed) {
            topics.add(whereUsedTopic())
        }
        return topics
    }

    Topic specificationTopic() {
        log.info("Generating specification for: ${name}")
        Topic.build {
            title "Specification"
            id getDitaKey() + "_specification"
            body {
                getDataSetClasses().sort { it.webOrder }.each { dataSetClass ->
                    if (name.startsWith('CDS') || name.startsWith('ECDS') || name.startsWith('Emergency Care Data Set')) {
                        div dataSetClass.outputCDSClassAsDita(dataDictionary)
                    } else {
                        div dataSetClass.outputClassAsDita(dataDictionary)
                    }
                }
            }
        }
    }



    String getStructureAsHtml() {
        StringWriter stringWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(stringWriter)
        markupBuilder.setEscapeAttributes(false)
        markupBuilder.setDoubleQuotes(true)
        if(name.startsWith('CDS') || name.startsWith('ECDS') || name.startsWith('Emergency Caee Data Set')) {
            outputAsCdsHtml(markupBuilder)
        } else {
            outputAsHtml(markupBuilder)
        }

        return stringWriter.toString().replaceAll(">\\s+<", "><").trim()
    }

    void outputAsHtml(MarkupBuilder markupBuilder) {
        dataSetClasses.each {ddDataClass ->
            outputClassAsHtml(ddDataClass, markupBuilder)
        }
    }

    void outputAsCdsHtml(MarkupBuilder markupBuilder) {
        dataSetClasses.each {dataSetClass ->
            dataSetClass.outputAsCdsHtml(markupBuilder)
        }
    }

    void outputClassAsHtml(NhsDDDataSetClass ddDataClass, MarkupBuilder markupBuilder) {
        if (ddDataClass.isChoice && ddDataClass.name.startsWith("Choice")) {
            markupBuilder.b "One of the following options must be used:"
            ddDataClass.dataSetClasses.
                eachWithIndex{childDataClass, int idx ->
                    if (idx != 0) {
                        markupBuilder.b "Or"
                    }
                    outputClassAsHtml(childDataClass, markupBuilder)
                }
        } else {

            markupBuilder.table (class:"simpletable table table-sm") {
                thead {
                    tr {
                        th(colspan: 2, class: "thead-light") {
                            b ddDataClass.name
                            if (ddDataClass.description) {
                                p ddDataClass.description
                            }
                        }
                    }
                }
                tbody {
                    if (ddDataClass.dataSetElements) {
                        markupBuilder.tr {
                            td(style: "width: 20%;") {p "Mandation"}
                            td(style: "width: 80%;") {p "Data Elements"}
                        }
                    }
                    ddDataClass.dataSetElements.each {dataElement ->
                        markupBuilder.tr {
                            td ""
                            td {
                                dataElement.createLink(markupBuilder)
                            }
                        }
                    }
                }
            }
        }

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

}
