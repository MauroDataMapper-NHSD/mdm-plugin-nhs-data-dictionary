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

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass

import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import org.apache.commons.lang3.StringUtils
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.DataSetParser
import groovy.xml.XmlUtil

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


    List<String> path
    String definitionAsXml
    String overviewPageUrl

    boolean isCDS

    List<NhsDDDataSetClass> dataSetClasses

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
        String shortDescription = name
        if(isPreparatory()) {
            shortDescription = "This item is being used for development purposes and has not yet been approved."
        } else {

            List<String> aliases = [name]
            aliases.addAll(getAliases().values())

            try {

                List<String> allSentences = calculateSentences(definition)

                if(isRetired()) {
                    shortDescription = allSentences[0]
                } else {
                    shortDescription = allSentences.find {sentence ->
                        aliases.find {alias ->
                            sentence.contains(alias)
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace()
                log.error("Couldn't parse: " + definition)
                shortDescription = name
            }
        }
        otherProperties["shortDescription"] = shortDescription
        return shortDescription
    }

    @Override
    String getXmlNodeName() {
        "DDDataSet"
    }



    String getMauroPath() {
        String mauroPath = StringUtils.join(path.collect{"fo:${it}"}, "|")

        mauroPath += "dm:${name}"

    }

    String getStructureAsHtml() {
        StringWriter stringWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(stringWriter)
        if(name.startsWith("CDS")) {
            outputAsCdsHtml(markupBuilder)
        } else {
            outputAsHtml(markupBuilder)
        }

        return stringWriter.toString().replaceAll(">\\s+<", "><").trim()
    }

    void outputAsHtml(MarkupBuilder markupBuilder) {
        ((DataModel) catalogueItem).dataClasses.each {dataClass ->
            outputClassAsHtml(dataClass, markupBuilder)
        }
    }

    void outputAsCdsHtml(MarkupBuilder markupBuilder) {
        dataSetClasses.each {dataSetClass ->
            dataSetClass.outputAsCdsHtml(markupBuilder)
        }
    }

    void outputClassAsHtml(DataClass dataClass, MarkupBuilder markupBuilder) {
        if (DataSetParser.isChoice(dataClass) && dataClass.label.startsWith("Choice")) {
            markupBuilder.b "One of the following options must be used:"
            dataClass.dataClasses.
                sort {it.metadata.find {md -> md.key == "Web Order"}.value }.
                eachWithIndex{DataClass childDataClass, int idx ->
                    if (idx != 0) {
                        markupBuilder.b "Or"
                    }
                    outputClassAsHtml(childDataClass, markupBuilder)
                }
        } else {

            markupBuilder.table (class:"simpletable table table-sm") {
                tr {
                    th (colspan: 2, class: "thead-light") {
                        b dataClass.label
                        if(dataClass.description) {
                            p dataClass.description
                        }
                    }
                }
                if(dataClass.importedDataElements) {
                    markupBuilder.tr {
                        td (width: "20%") { p "Mandation" }
                        td (width: "80%") { p "Data Elements" }
                    }
                }
                dataClass.importedDataElements.each {dataElement ->
                    markupBuilder.tr {
                        td ""
                        td dataElement.label
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


}
