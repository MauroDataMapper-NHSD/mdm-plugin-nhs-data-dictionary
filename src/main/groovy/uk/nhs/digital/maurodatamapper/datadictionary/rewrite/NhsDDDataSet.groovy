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
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import groovy.xml.slurpersupport.GPathResult
import org.apache.commons.lang3.StringUtils
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.DataSetParser
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.ColSpec
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.Entry
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.Row
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.TBody
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.TGroup
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.THead
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.Table

@Slf4j
class NhsDDDataSet implements NhsDataDictionaryComponent {

    @Override
    String getStereotype() {
        "Data Set"
    }

    @Override
    String getStereotypeForPreview() {
        "dataSet"
    }


    List<String> path
    GPathResult definitionAsXml

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
        path = getPath()
        definitionAsXml = xml.definition
        otherProperties["approvingOrganisation"] = "Data Alliance Partnership Board (DAPB)"
    }

    @Override
    String calculateShortDescription() {
        String shortDescription = name
        if(isPreparatory()) {
            shortDescription = "This item is being used for development purposes and has not yet been approved."
        } else {

            List<String> aliases = [name]
            aliases.addAll(otherProperties.findAll{key, value -> key.startsWith("alias")}.collect {it.value})

            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + definition + "</xml>")
                String allParagraphs = ""
                xml.p.each { paragraph ->
                    allParagraphs += paragraph.text()
                }
                String nextSentence = ""
                while (!aliases.find { it -> nextSentence.contains(it) } && allParagraphs.contains(".")) {
                    nextSentence = allParagraphs.substring(0, allParagraphs.indexOf(".") + 1)
                    if (aliases.find { it -> nextSentence.contains(it) }) {
                        if(nextSentence.startsWith("Introduction")) {
                            nextSentence = nextSentence.replaceFirst("Introduction", "")
                        }
                        shortDescription = nextSentence
                        break
                    }
                    allParagraphs = allParagraphs.substring(allParagraphs.indexOf(".") + 1)
                }
            } catch (Exception e) {
                log.error("Couldn't parse: " + definition)
                shortDescription = name
            }
        }
        //shortDescription = shortDescription.replaceAll("\\s+", " ")
        otherProperties["shortDescription"] = shortDescription
        return shortDescription
    }

    @Override
    String getXmlNodeName() {
        "DDDataSet"
    }

    List<String> getPath() {
        List<String> path = []
        try {
            path.addAll(DDHelperFunctions.getPath(otherProperties["baseUri"], "Messages", ".txaClass20"))
        } catch (Exception e) {
            path.addAll(DDHelperFunctions.getPath(otherProperties["baseUri"], "Web_Site_Content", ".txaClass20"))
        }
        path.removeAll {it.equalsIgnoreCase("Data_Sets")}
        path.removeAll {it.equalsIgnoreCase("Content")}

        if (isRetired()) {
            path.add(0, "Retired")
        }
        path = path.collect {DDHelperFunctions.tidyLabel(it)}
        return path
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

            markupBuilder.table (class:"simpletable table table-striped table-sm") {
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


}
