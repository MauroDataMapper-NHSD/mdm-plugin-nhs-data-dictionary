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
package uk.nhs.digital.maurodatamapper.datadictionary.datasets

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary

@Slf4j
class OtherDataSetParser {
    static List<DataClass> parseDataSetWithHeaderTables(Node definition, DataModel dataModel, NhsDataDictionary dataDictionary) {
        DataClass currentClass = null
        DataClass originalParentClass = null // for when we have to drop down into a choice block
        Integer classWebOrder = 0
        Integer elementWebOrder = 0
        Integer choiceNo = 1
        List<DataClass> returnDataClasses = []
        //log.debug(partitionByDuckBlueClasses(definition).size())
        DataSetParser.partitionByDuckBlueClasses(definition).each {sect ->
            List<Node> section = sect.findAll { it instanceof Node && it.name() != "br" }
            List<Node> tables = section.findAll { it instanceof Node && it.name() == "table" }
            List<Node> ors = section.findAll {it instanceof Node &&
                it.name() == "strong" &&
                it.text().toLowerCase() == "or"
            }
            if (tables.size() <= 1) {
                // ignore... this is probably just the general, basic instructions
            } else if (tables.size() == 2) { // header and definition
                List<DataClass> dataClasses = parseDataClassTable(tables[1], dataModel, dataDictionary, 1)
                if (dataClasses.size() == 1) {
                    dataClasses.get(0).label = tables.get(0).tbody.tr.th.text()
                    returnDataClasses.add(dataClasses.get(0))
                } else {
                    log.error("Oh no!  More than 1 data class returned!")
                }
            } else if (tables.size() >= 3 &&
                       ors.size() > 0 && tables[0].text() != "TWO YEAR NEONATAL OUTCOMES ASSESSMENT"
                && tables[0].text() != "IMAGING: CANCER SITE LOCATION CHOICE - CORE") {
                // header and choice between n
                DataClass choiceDataClass = new DataClass(label: tables[0].tbody.tr.th.text())
                dataModel.addToDataClasses(choiceDataClass)
                DataSetParser.setChoice(choiceDataClass)
                DataSetParser.setOrder(choiceDataClass, elementWebOrder)
                elementWebOrder++
                returnDataClasses.add(choiceDataClass)
                tables.eachWithIndex { table, index ->
                    if (index > 0 && index < ors.size() + 2) { // ignore the first, header table
                        List<DataClass> dataClasses = parseDataClassTable(table, dataModel, dataDictionary, 1)
                        if (dataClasses.size() > 1) {
                            log.error("Oh no!  More than 1 data class returned!")
                            dataClasses.each {
                                log.error(it.label)
                            }
                        } else {
                            DataSetParser.setOrder(dataClasses.get(0), index)
                            choiceDataClass.addToDataClasses(dataClasses.get(0))
                        }
                    } else if (index != 0) {
                        List<DataClass> childClasses = parseDataClassTable(table, dataModel, dataDictionary, choiceNo)
                        childClasses.eachWithIndex { childDataClass, idx ->
                            choiceDataClass.addToDataClasses(childDataClass)
                            DataSetParser.setNotOption(childDataClass)
                            DataSetParser.setOrder(childDataClass, elementWebOrder)
                            elementWebOrder++
                        }
                    }
                }
            } else if (tables.size() >= 3 &&
                       ors.size() > 0 && tables[0].text() != "TWO YEAR NEONATAL OUTCOMES ASSESSMENT"
                && tables[0].text() == "IMAGING: CANCER SITE LOCATION CHOICE - CORE") {
                // header and choice between n
                DataClass topDataClass = new DataClass(label: tables[0].tbody.tr.th.text())
                dataModel.addToDataClasses(topDataClass)
                DataSetParser.setOrder(topDataClass, elementWebOrder)
                elementWebOrder++
                returnDataClasses.add(topDataClass)
                DataClass choiceDataClass = new DataClass(label: "Choice")
                dataModel.addToDataClasses(choiceDataClass)
                DataSetParser.setChoice(choiceDataClass)
                DataSetParser.setOrder(choiceDataClass, 1)
                topDataClass.addToDataClasses(choiceDataClass)
                tables.eachWithIndex { table, index ->
                    if(table != tables.last()) {
                        if (index > 0 && index < ors.size() + 2) { // ignore the first, header table
                            List<DataClass> dataClasses = parseDataClassTable(table, dataModel, dataDictionary, 1)
                            if (dataClasses.size() > 1) {
                                log.error("Oh no!  More than 1 data class returned!")
                                dataClasses.each {
                                    log.error(it.label)
                                }
                            } else {
                                DataSetParser.setOrder(dataClasses.get(0), index)
                                choiceDataClass.addToDataClasses(dataClasses.get(0))
                            }
                        } else if (index != 0) {
                            List<DataClass> childClasses = parseDataClassTable(table, dataModel, dataDictionary, choiceNo)
                            childClasses.eachWithIndex { childDataClass, idx ->
                                choiceDataClass.addToDataClasses(childDataClass)
                                DataSetParser.setNotOption(childDataClass)
                                DataSetParser.setOrder(childDataClass, elementWebOrder)
                                elementWebOrder++
                            }
                        }
                    } else {
                        List<DataClass> dataClasses = parseDataClassTable(tables.last(), dataModel, dataDictionary, 1)
                        DataSetParser.setOrder(dataClasses.get(0), 2)
                        topDataClass.addToDataClasses(dataClasses.get(0))
                    }

                }
            } else {
                DataClass dataClass = new DataClass()
                dataClass.label = tables.get(0).tbody.tr.th.text()
                dataClass.description = ""
                dataModel.addToDataClasses(dataClass)
                elementWebOrder = 0

                section.findAll {it instanceof Node && it.name() == "strong" && it.text() != "OR" }.each { par ->
                    if(par instanceof Node) {
                        dataClass.description += par.text()
                    } else {
                        dataClass.description += par
                    }
                }

                dataClass.description = dataClass.description.trim() ?: null
                for (int idx = 1; idx < section.size(); idx++) {
                    if (section[idx].name() == "table") {
                        if (section[idx + 1] && section[idx + 1].name() == "strong" && section[idx + 1].text() == "OR") {
                            List<DataClass> childClasses1 = parseDataClassTable(section[idx], dataModel, dataDictionary, choiceNo)
                            List<DataClass> childClasses2 = parseDataClassTable(section[idx + 2], dataModel, dataDictionary, choiceNo)
                            if (childClasses1.size() == 1 && childClasses2.size() == 1) {
                                DataClass choiceDataClass = new DataClass(label: "Choice " + choiceNo)
                                dataModel.addToDataClasses(choiceDataClass)
                                DataSetParser.setChoice(choiceDataClass)
                                choiceNo++
                                DataSetParser.setOrder(choiceDataClass, elementWebOrder)
                                elementWebOrder++
                                dataClass.addToDataClasses(choiceDataClass)
                                DataSetParser.setOrder(childClasses1.get(0), 0)
                                DataSetParser.setOrder(childClasses2.get(0), 1)
                                choiceDataClass.addToDataClasses(childClasses1.get(0))
                                choiceDataClass.addToDataClasses(childClasses2.get(0))
                            } else {
                                log.error("Oh no!  More than 1 data class returned!")
                            }
                            idx += 2
                        } else {
                            List<DataClass> childClasses = parseDataClassTable(section[idx], dataModel, dataDictionary, choiceNo)
                            childClasses.eachWithIndex { childDataClass, index ->
                                dataClass.addToDataClasses(childDataClass)
                                DataSetParser.setOrder(childDataClass, elementWebOrder)
                                elementWebOrder++
                            }
                        }
                    }
                }
                returnDataClasses.add(dataClass)
            }
        }
        return returnDataClasses
    }

    static List<DataClass> parseDataClassTable(Node table,
                                               DataModel dataModel,
                                               NhsDataDictionary dataDictionary,
                                               Integer choiceNo = 1) {
        Integer elementWebOrder = 0
        DataClass currentClass = new DataClass(label: "")
        dataModel.addToDataClasses(currentClass)
        List<DataClass> dataClasses = [currentClass]

        table.tbody.tr.eachWithIndex { tr, idx ->
            //System.err.println("Processing row: " + tr)
            if (tr.th.size() == 2 && DataSetParser.isSkyBlue(tr.th[0])) {
                // ignore
            } else if (tr.td.size() == 2 && DataSetParser.isSkyBlue(tr.td[0])) {
                if (dataModel.label.startsWith("National Joint Registry Data Set") ||
                    dataModel.label.startsWith("National_Joint_Registry_Data_Set")) {
                    if ((currentClass.dataElements && currentClass.dataElements.size() > 0) ||
                        (currentClass.dataClasses && currentClass.dataClasses.size() > 0)) {
                        currentClass = new DataClass()
                        dataModel.addToDataClasses(currentClass)
                        dataClasses.add(currentClass)
                        elementWebOrder = 0
                    }
                    DataSetParser.setNameAndDescriptionFromCell(currentClass, (Node) tr.td[1])

                } else {
                    // ignore
                }
            } else if (tr.td.size() == 1 &&
                       (tr.td.text().startsWith("One of the following")
                           || tr.td.text().startsWith("At least one of the following")
                           || tr.td.text().startsWith("One of the following may be provided per")
                           || tr.td.text().startsWith("One occurrence of this group"))) {
                // ignore
            } else if (tr.th.size() == 1
                && (DataSetParser.isSkyBlue(tr.th[0])  || tr.th[0].@class == "duckblue" )) {
                if (((String) tr.th.text()).toLowerCase() != "data set data elements") {
                    // We're starting a new class.  Let's finish with the old one...
                    if ((currentClass.dataElements && currentClass.dataElements.size() > 0) ||
                        (currentClass.dataClasses && currentClass.dataClasses.size() > 0)) {
                        currentClass = new DataClass()
                        dataModel.addToDataClasses(currentClass)
                        dataClasses.add(currentClass)
                        elementWebOrder = 0
                    }
                    DataSetParser.setNameAndDescriptionFromCell(currentClass, tr.th[0])
                }
            } else if (tr.td.size() == 1
                && DataSetParser.isSkyBlue(tr.td[0])) {
                if (((String) tr.td[0].text()).toLowerCase() != "data set data elements") {
                    // We're starting a new class.  Let's finish with the old one...
                    if ((currentClass.dataElements && currentClass.dataElements.size() > 0) ||
                        (currentClass.dataClasses && currentClass.dataClasses.size() > 0)) {
                        currentClass = new DataClass()
                        dataModel.addToDataClasses(currentClass)
                        dataClasses.add(currentClass)
                        elementWebOrder = 0
                    }
                    DataSetParser.setNameAndDescriptionFromCell(currentClass, tr.td[0])
                }
            } else {
                if (!currentClass) { // We're probably in a duckblue section, and there's one table, and it
                    // doesn't have any further header information.
                    currentClass = new DataClass()
                    dataModel.addToDataClasses(currentClass)
                    dataClasses.add(currentClass)
                } else {
                    CatalogueItem catalogueItem = parseDataElementRow(tr, dataModel, dataDictionary, currentClass)
                    DataSetParser.setOrder(catalogueItem, elementWebOrder)
                    elementWebOrder++
                    if(catalogueItem instanceof DataClass) {
                        currentClass.addToDataClasses(catalogueItem)
                    } else if(catalogueItem instanceof DataElement) {

                        // Shouldn't need to do this any more!
/*                        if (catalogueItem.dataClass) {
                            currentClass.addToImportedDataElements(catalogueItem)
                        } else {
                            // If the DE has been created/not found in the core DM then we need to add it to the DC rather than import it
                            currentClass.addToDataElements(catalogueItem)
                        }

 */
                        //log.debug("Added ${catalogueItem.label} to ${currentClass.label}")
                    }
                }



                /*                if (tr.td[1].a.size() == 2 && tr.td[1].em[0].text() == "or") { // a or b
                                    DataClass choiceClass = new DataClass(label: "Choice " + choiceNo)
                                    choiceNo++
                                    Integer choiceElementWebOrder = 0
                                    tr.td[1].children().each { child ->
                                        if (child.name() == "a") {
                                            DataElement dataElement = DataSetParser.getElementFromText(child, dataModel, dataDictionary, choiceClass.label)
                                            choiceClass.addToDataElements(dataElement)
                                            DataSetParser.setOrder(dataElement, choiceElementWebOrder)
                                            choiceElementWebOrder++
                                        }
                                    }
                                    DataSetParser.setChoice(choiceClass)
                                    DataSetParser.setMRO(choiceClass, tr.td[0].text())
                                    currentClass.addToDataClasses(choiceClass)
                                    DataSetParser.setOrder(choiceClass, elementWebOrder)
                                    elementWebOrder++

                                } else if (tr.td[1].a.size() == 2 && tr.td[1].em[0].text() == "and") { // a or b
                                    DataClass choiceClass = new DataClass(label: "And " + choiceNo)
                                    choiceNo++
                                    Integer choiceElementWebOrder = 0
                                    tr.td[1].children().each { child ->
                                        if (child.name() == "a") {
                                            DataElement dataElement = DataSetParser.getElementFromText(child, dataModel, dataDictionary, choiceClass.label)
                                            choiceClass.addToDataElements(dataElement)
                                            DataSetParser.setOrder(dataElement, choiceElementWebOrder)
                                            choiceElementWebOrder++
                                        }
                                    }
                                    DataSetParser.setAnd(choiceClass)
                                    DataSetParser.setMRO(choiceClass, tr.td[0].text())
                                    currentClass.addToDataClasses(choiceClass)
                                    DataSetParser.setOrder(choiceClass, elementWebOrder)
                                    elementWebOrder++

                                } else if (tr.td[1].a.size() == 2 && tr.td[1].em[0].text() == "and/or") { // a or b
                                    DataClass choiceClass = new DataClass(label: "Choice " + choiceNo)
                                    DataSetParser.setInclusiveOr(choiceClass)
                                    choiceNo++
                                    Integer choiceElementWebOrder = 0
                                    tr.td[1].children().each { child ->
                                        if (child.name() == "a") {
                                            DataElement dataElement = DataSetParser.getElementFromText(child, dataModel, dataDictionary, choiceClass.label)
                                            choiceClass.addToDataElements(dataElement)
                                            DataSetParser.setOrder(dataElement, choiceElementWebOrder)
                                            choiceElementWebOrder++
                                        }
                                    }
                                    DataSetParser.setChoice(choiceClass)
                                    DataSetParser.setMRO(choiceClass, tr.td[0].text())
                                    currentClass.addToDataClasses(choiceClass)
                                    DataSetParser.setOrder(choiceClass, elementWebOrder)
                                    elementWebOrder++

                                } else if (tr.td[1].a.size() == 4 && tr.td[1].em.size() == 1 && tr.td[1].em[0].text() == "or") {
                                    // a1 - a2 or b1 - b2
                                    DataClass choiceClass = new DataClass(label: "Choice " + choiceNo)
                                    choiceNo++
                                    Integer choiceElementWebOrder = 0

                                    DataElement dataElement = DataSetParser.getElementFromText(tr.td[1].a[0], dataModel, dataDictionary, choiceClass.label)
                                    choiceClass.addToDataElements(dataElement)
                                    DataSetParser.setOrder(dataElement, choiceElementWebOrder)
                                    choiceElementWebOrder++

                                    DataSetParser.setChoice(choiceClass)
                                    DataSetParser.setAddress(choiceClass)
                                    DataSetParser.setMRO(choiceClass, tr.td[0].text())
                                    currentClass.addToDataClasses(choiceClass)
                                    DataSetParser.setOrder(choiceClass, elementWebOrder)
                                    elementWebOrder++

                                } else if (tr.td[1].a.size() == 4 && tr.td[1].em.size() == 3 && tr.td[1].em[0].text() == "or") {
                                    // a1 or a2 or a3 or a4
                                    DataClass choiceClass = new DataClass(label: "Choice " + choiceNo)
                                    choiceNo++
                                    Integer choiceElementWebOrder = 0

                                    tr.td[1].a.each { anchor ->
                                        DataElement dataElement = DataSetParser.getElementFromText(anchor, dataModel, dataDictionary, choiceClass.label)
                                        choiceClass.addToDataElements(dataElement)
                                        DataSetParser.setOrder(dataElement, choiceElementWebOrder)
                                        choiceElementWebOrder++
                                    }
                                    DataSetParser.setChoice(choiceClass)
                                    DataSetParser.setMRO(choiceClass, tr.td[0].text())
                                    currentClass.addToDataClasses(choiceClass)
                                    DataSetParser.setOrder(choiceClass, elementWebOrder)
                                    elementWebOrder++

                                } else if (tr.td[1].a.size() == 3 &&
                                        tr.td[1].em[0].text() == "or" &&
                                        tr.td[1].em[1].text() == "and") { // a or (b and c)
                                    DataClass choiceClass = new DataClass(label: "Choice " + choiceNo)
                                    DataSetParser.setMRO(choiceClass, tr.td[0].text())
                                    choiceNo++
                                    DataElement dataElement = DataSetParser.getElementFromText(tr.td[1].a[0], dataModel, dataDictionary, currentClass.label)
                                    choiceClass.addToDataElements(dataElement)
                                    DataSetParser.setOrder(dataElement, 0)
                                    DataSetParser.setChoice(choiceClass)

                                    DataClass andClass = new DataClass(label: "And " + choiceNo)
                                    DataSetParser.setAnd(andClass)
                                    choiceNo++
                                    dataElement = DataSetParser.getElementFromText(tr.td[1].a[1], dataModel, dataDictionary, currentClass.label)
                                    DataSetParser.setOrder(dataElement, 0)
                                    andClass.addToDataElements(dataElement)
                                    dataElement = DataSetParser.getElementFromText(tr.td[1].a[2], dataModel, dataDictionary, currentClass.label)
                                    DataSetParser.setOrder(dataElement, 1)
                                    andClass.addToDataElements(dataElement)
                                    choiceClass.addToDataClasses(andClass)
                                    DataSetParser.setOrder(andClass, 1)

                                    currentClass.addToDataClasses(choiceClass)
                                    DataSetParser.setOrder(choiceClass, elementWebOrder)
                                    elementWebOrder++

                                } else if (tr.td[1].a.size() == 3 &&
                                        tr.td[1].em[0].text() == "and" &&
                                        tr.td[1].em[1].text() == "or") { // (a and b) or c)
                                    DataClass choiceClass = new DataClass(label: "Choice " + choiceNo)
                                    DataSetParser.setMRO(choiceClass, tr.td[0].text())
                                    choiceNo++
                                    DataSetParser.setChoice(choiceClass)

                                    DataClass andClass = new DataClass(label: "And " + choiceNo)
                                    DataSetParser.setAnd(andClass)
                                    choiceNo++
                                    DataElement dataElement = DataSetParser.getElementFromText(tr.td[1].a[0], dataModel, dataDictionary, choiceClass.label)
                                    DataSetParser.setOrder(dataElement, 0)
                                    andClass.addToDataElements(dataElement)
                                    dataElement = DataSetParser.getElementFromText(tr.td[1].a[1], dataModel, dataDictionary, choiceClass.label)
                                    DataSetParser.setOrder(dataElement, 1)
                                    andClass.addToDataElements(dataElement)
                                    choiceClass.addToDataClasses(andClass)
                                    DataSetParser.setOrder(andClass, 0)

                                    dataElement = DataSetParser.getElementFromText(tr.td[1].a[2], dataModel, dataDictionary, choiceClass.label)
                                    choiceClass.addToDataElements(dataElement)
                                    DataSetParser.setOrder(dataElement, 1)


                                    currentClass.addToDataClasses(choiceClass)
                                    DataSetParser.setOrder(choiceClass, elementWebOrder)
                                    elementWebOrder++

                                } else if (tr.td[1].a.size() == 3) {
                                    if (!currentClass) { // We're probably in a duckblue section, and there's one table, and it
                                        // doesn't have any further header information.
                                        currentClass = new DataClass()
                                        dataClasses.add(currentClass)
                                    }
                                    DataElement dataElement = DataSetParser.getElementFromText(tr.td[1].a[0], dataModel, dataDictionary, currentClass.label)
                                    dataElement.description == tr.td[1]
                                    currentClass.addToDataElements(dataElement)
                                    DataSetParser.setOrder(dataElement, elementWebOrder)
                                    elementWebOrder++
                                } else {
                                    if (!currentClass) { // We're probably in a duckblue section, and there's one table, and it
                                        // doesn't have any further header information.
                                        currentClass = new DataClass()
                                        dataClasses.add(currentClass)
                                    }
                                    DataElement dataElement = DataSetParser.getElementFromText(tr.td[1].a, dataModel, dataDictionary, currentClass.label)
                                    DataSetParser.setMRO(dataElement, tr.td[0].text())
                                    currentClass.addToDataElements(dataElement)
                                    DataSetParser.setOrder(dataElement, elementWebOrder)
                                    elementWebOrder++
                                }
                            */
            }
        }
        //log.debug("data classes " + dataClasses.size())

        //log.debug(currentClass.dataElements.size())
        return dataClasses

    }

    static CatalogueItem parseDataElementRow(Node tr, DataModel dataModel, NhsDataDictionary dataDictionary, DataClass currentClass ) {
        //log.debug("parseDataElementRow")
        String currentClassName = currentClass.label
        CatalogueItem returnElement = null

        /*GPathResult elementTd = tr.td[0]
        if(tr.td.size() > 1) {
            elementTd = tr.td[1]
        }
        elementTd.children().each {

        }*/
        List<Node> tds = DataSetParser.tableRowCells(tr)

        if(tds.size() == 1) {
            if(tds[0].a.size() > 1) {
                unmatchedPattern("Found more than one link on a single column row", tr, dataModel.label, currentClassName)
            } else {
                returnElement = DataSetParser.getElementFromText(tds[0].a[0], dataModel, dataDictionary, currentClass)
                int multiples = getMultipleOccurrences(tds[0].text())
                if(multiples == 1) {
                    returnElement.maxMultiplicity = -1
                } else if(multiples != 0) {
                    unmatchedPattern("Wrong number of multiples", tr, dataModel.label, currentClassName)
                }
            }
        } else if(tds.size() == 2) {
            if(tds[1].a.size() == 1) {
                returnElement = DataSetParser.getElementFromText(tds[1].a[0], dataModel, dataDictionary, currentClass)
                int multiples = getMultipleOccurrences(tds[1].text())
                if(multiples == 1) {
                    returnElement.maxMultiplicity = -1
                } else if(multiples != 0) {
                    unmatchedPattern("Wrong number of multiples", tr, dataModel.label, currentClassName)
                }
            } else if(tds[1].a.size() == 2) {
                returnElement = new DataClass(label: "Choice")
                dataModel.addToDataClasses(returnElement)
                Integer choiceElementWebOrder = 0
                tds[1].children().each { child ->
                    if (child instanceof Node && child.name() == "a") {
                        DataElement dataElement = DataSetParser.getElementFromText(child, dataModel, dataDictionary, currentClass)
                        //log.debug("Added ${dataElement.label} to ${returnElement.label}")
                        DataSetParser.setOrder(dataElement, choiceElementWebOrder)
                        choiceElementWebOrder++
                    }
                }
                if(tds[1].em[0].text() == "or") {
                    DataSetParser.setChoice(returnElement)
                } else if(tds[1].em[0].text() == "and") {
                    DataSetParser.setAnd(returnElement)
                } else if(tds[1].em[0].text() == "and/or") {
                    DataSetParser.setInclusiveOr(returnElement)
                } else {
                    unmatchedPattern("Unmatched 2 link pattern", tr, dataModel.label, currentClassName)
                }

                int multiples = getMultipleOccurrences(tds[1].text())
                if(multiples == 2) {
                    returnElement.dataElements.each {de ->
                        de.maxMultiplicity = -1
                    }
                } else if(multiples != 0) {
                    unmatchedPattern("Wrong number of multiples", tr, dataModel.label, currentClassName)
                }


            } else if(tds[1].a.size() == 3) {
                if(tds[1].em.size() >= 2 && tds[1].em[0].text() == "or" && tds[1].em[1].text() == "and") {
                    returnElement = new DataClass(label: "Choice")
                    dataModel.addToDataClasses(returnElement)
                    DataElement dataElement = DataSetParser.getElementFromText(tds[1].a[0], dataModel, dataDictionary, currentClass)
                    //log.debug("Added ${dataElement.label} to ${returnElement.label}")
                    DataSetParser.setOrder(dataElement, 0)
                    DataSetParser.setChoice(returnElement)

                    DataClass andClass = new DataClass(label: "And")
                    dataModel.addToDataClasses(andClass)
                    DataSetParser.setAnd(andClass)
                    dataElement = DataSetParser.getElementFromText(tds[1].a[1], dataModel, dataDictionary, currentClass)
                    DataSetParser.setOrder(dataElement, 0)
                    //log.debug("Added ${dataElement.label} to ${andClass.label}")
                    dataElement = DataSetParser.getElementFromText(tds[1].a[2], dataModel, dataDictionary, currentClass)
                    DataSetParser.setOrder(dataElement, 1)
                    //log.debug("Added ${dataElement.label} to ${andClass.label}")
                    returnElement.addToDataClasses(andClass)
                    DataSetParser.setOrder(andClass, 1)

                } else if (tds[1].em.size() >= 2 && tds[1].em[0].text() == "and" && tds[1].em[1].text() == "or") {
                    returnElement = new DataClass(label: "Choice")
                    dataModel.addToDataClasses(returnElement)
                    DataSetParser.setChoice(returnElement)
                    DataClass andClass = new DataClass(label: "And")
                    dataModel.addToDataClasses(returnElement)
                    DataSetParser.setAnd(andClass)
                    DataElement dataElement = DataSetParser.getElementFromText(tds[1].a[0], dataModel, dataDictionary, currentClass)
                    DataSetParser.setOrder(dataElement, 0)
                    //log.debug("Added ${dataElement.label} to ${andClass.label}")
                    dataElement = DataSetParser.getElementFromText(tds[1].a[1], dataModel, dataDictionary, currentClass)
                    DataSetParser.setOrder(dataElement, 1)
                    //log.debug("Added ${dataElement.label} to ${andClass.label}")
                    returnElement.addToDataClasses(andClass)
                    DataSetParser.setOrder(andClass, 0)

                    dataElement = DataSetParser.getElementFromText(tds[1].a[2], dataModel, dataDictionary, currentClass)
                    //log.debug("Added ${dataElement.label} to ${returnElement.label}")
                    DataSetParser.setOrder(dataElement, 1)
                } else if (tds[1].em.size() >= 2 && tds[1].em[0].text() == "or" && tds[1].em[1].text() == "or") {
                    returnElement = new DataClass(label: "Choice")
                    dataModel.addToDataClasses(returnElement)
                    DataSetParser.setChoice(returnElement)
                    DataElement dataElement = DataSetParser.getElementFromText(tds[1].a[0], dataModel, dataDictionary, currentClass)
                    DataSetParser.setOrder(dataElement, 0)
                    //log.debug("Added ${dataElement.label} to ${returnElement.label}")
                    dataElement = DataSetParser.getElementFromText(tds[1].a[1], dataModel, dataDictionary, currentClass)
                    DataSetParser.setOrder(dataElement, 1)
                    //log.debug("Added ${dataElement.label} to ${returnElement.label}")
                    dataElement = DataSetParser.getElementFromText(tds[1].a[2], dataModel, dataDictionary, currentClass)
                    //log.debug("Added ${dataElement.label} to ${returnElement.label}")
                    DataSetParser.setOrder(dataElement, 2)
                } else if((dataModel.label == "National Joint Registry Data Set - Common Details" ||
                           dataModel.label == "National_Joint_Registry_Data_Set_-_Common_Details") &&
                          tr.td[1].a[0].text() == "POSTCODE_OF_USUAL_ADDRESS") {
                    returnElement = DataSetParser.getElementFromText(tds[1].a[0], dataModel, dataDictionary, currentClass)
                } else {
                    unmatchedPattern("Unmatched 3 link pattern: ", tr, dataModel.label, currentClassName)
                    returnElement = DataSetParser.getElementFromText(tds[1].a[0], dataModel, dataDictionary, currentClass)
                }

                int multiples = getMultipleOccurrences(tds[1].text())
                if(multiples == 3) {
                    log.trace("3 multiples!")
                    returnElement.dataElements.each {de ->
                        de.maxMultiplicity = -1
                    }
                    returnElement.dataClasses.each {dc ->
                        dc.dataElements.each { de ->
                            de.maxMultiplicity = -1
                        }
                    }
                } else if(multiples != 0) {
                    unmatchedPattern("Wrong number of multiples", tr, dataModel.label, currentClassName)
                }

            } else if (tds[1].a.size() == 4) {
                if(tds[1].em.size() == 3
                    && tds[1].em[0].text() == "or"
                    && tds[1].em[1].text() == "or"
                    && tds[1].em[2].text() == "or") {
                    // a1 - a2 or b1 or b2
                    returnElement = new DataClass(label: "Choice")
                    dataModel.addToDataClasses(returnElement)
                    Integer choiceElementWebOrder = 0

                    tds[1].a.each { anchor ->
                        DataElement dataElement = DataSetParser.getElementFromText(anchor, dataModel, dataDictionary, currentClass)
                        //log.debug("Added ${dataElement.label} to ${returnElement.label}")
                        DataSetParser.setOrder(dataElement, choiceElementWebOrder)
                        choiceElementWebOrder++
                    }
                    DataSetParser.setChoice(returnElement)

                } else if(tds[1].em.size() == 1 && tds[1].em[0].text() == "or") {
                    // a1 - a2 or b1 - b2
                    returnElement = new DataClass(label: "Choice")
                    dataModel.addToDataClasses(returnElement)

                    DataElement dataElement = DataSetParser.getElementFromText(tds[1].a[0], dataModel, dataDictionary, currentClass)
                    //log.debug("Added ${dataElement.label} to ${returnElement.label}")
                    DataSetParser.setOrder(dataElement, 1)

                    DataSetParser.setChoice(returnElement)
                    DataSetParser.setAddress(returnElement)
                    if(dataElement.importingDataClasses == null) {
                        dataElement.addToMetadata(new Metadata(namespace: NhsDataDictionary.METADATA_NAMESPACE, key: "Address 1", value: tds[1].a[1].@href))
                        dataElement.addToMetadata(new Metadata(namespace: NhsDataDictionary.METADATA_NAMESPACE, key: "Address 2", value: tds[1].a[3].@href))
                    }
                }  else {
                    unmatchedPattern("Unmatched 4 link pattern", tr, dataModel.label, currentClassName)
                }

                int multiples = getMultipleOccurrences(tds[1].text())
                if(multiples == 4) {
                    log.trace("4 multiples!")
                    returnElement.dataElements.each {de ->
                        de.maxMultiplicity = -1
                    }
                    returnElement.dataClasses.each {dc ->
                        dc.dataElements.each { de ->
                            de.maxMultiplicity = -1
                        }
                    }
                } else if (dataModel.label == "GUMCAD Sexually Transmitted Infection Surveillance System Data Set" &&
                           multiples == 3 && returnElement.dataElements && returnElement.dataElements.size() >= 3) {
                    returnElement.dataElements[0].maxMultiplicity = -1
                    returnElement.dataElements[1].maxMultiplicity = -1
                    returnElement.dataElements[2].maxMultiplicity = -1
                } else if (multiples != 0) {
                    unmatchedPattern("Wrong number of multiples", tr, dataModel.label, currentClassName)
                }

            } else {
                unmatchedPattern("Unknown number of links!", tr, dataModel.label, currentClassName)
            }
            // Replace then trim as NBSP is not whitespace and could be removed to result in WS left over
            String mro = tds[0].text().replaceAll(" ", "").trim()
            if (mro == "M" || mro == "R" || mro == "O" || mro == "P") {
                DataSetParser.setMRO(returnElement, mro)
            } else {
                unmatchedPattern("Unknown MRO", tds[0], dataModel.label, currentClassName)
            }

        } else {
            unmatchedPattern("Unknown number of columns!", tr, dataModel.label, currentClassName)
        }

        return returnElement
    }

    static void unmatchedPattern(String message, Node component, String... context) {
        log.error("Unmatched Pattern {}\n{}\n{}", message, component.toString(), context.join(','))
    }

    static int getMultipleOccurrences(String input) {
        return StringUtils.countMatches(input, "Multiple occurrences of this item are permitted") +
               StringUtils.countMatches(input, "Multiple occurrences of this data item are permitted")
    }


}
