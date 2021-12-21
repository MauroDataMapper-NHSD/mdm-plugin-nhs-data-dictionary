package uk.nhs.digital.maurodatamapper.datadictionary.datasets

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import groovy.xml.XmlUtil
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary

@Slf4j
class CDSDataSetParser {


    static void parseCDSDataSet(GPathResult definition, DataModel dataModel, NhsDataDictionary dataDictionary) {
        List<DataClass> dataClasses = []
        dataClasses = parseCDSDataSetWithHeaderTables(definition, dataModel, dataDictionary)

        dataClasses.eachWithIndex {dataClass, index ->
            dataModel.addToDataClasses(dataClass)
            DataSetParser.setOrder(dataClass, index + 1)
        }
        DataSetParser.fixPotentialDuplicates(dataModel)
    }

    static List<DataClass> parseCDSDataSetWithHeaderTables(GPathResult definition, DataModel dataModel, NhsDataDictionary dataDictionary) {
        List<DataClass> returnDataClasses = []
        // log.debug(partitionByDuckBlueClasses(definition).size())
        List<List<GPathResult>> sections = DataSetParser.partitionByDuckBlueClasses(definition)
        for (int sectIdx = 0; sectIdx < sections.size(); sectIdx++) {
            List<GPathResult> sect = sections.get(sectIdx)
            List<GPathResult> ors = sect.findAll {
                (it.name() == "table" && it.text().trim() == "OR") ||
                (it.name() == "strong" && it.text().trim() == "OR")
            }
            if (ors.size() == 1 && sect.size() == 3) {
                DataClass choiceClass = new DataClass(label: "Choice")
                DataSetParser.setChoice(choiceClass)
                List<DataClass> classes1 = parseCDSSection(sect, dataModel, dataDictionary)
                sectIdx++
                sect = sections.get(sectIdx)
                List<DataClass> classes2 = parseCDSSection(sect, dataModel, dataDictionary)

                if (classes1.size() != 1 || classes2.size() != 1) {
                    log.warn("Oh no!  More than one CDS class returned!")
                } else {
                    choiceClass.addToDataClasses(classes1.get(0))
                    choiceClass.addToDataClasses(classes2.get(0))
                }
                returnDataClasses.add(choiceClass)
            } else {
                List<DataClass> sectionClasses = parseCDSSection(sect, dataModel, dataDictionary)
                returnDataClasses.addAll(sectionClasses)
            }

        }
        return returnDataClasses
    }

    static void parseCDSElementTable(List<GPathResult> tableRows, DataClass currentClass, DataModel dataModel, NhsDataDictionary dataDictionary,
                                     int position) {
        if (tableRows.size() == 0) {return} // We're done
        GPathResult firstRow = tableRows[0]

        if (tableRows.size() == 1 && firstRow.td[0].@bgcolor.text() == "#DCDCDC" && firstRow.td.size() == 3) {

            // Single row, pointer to data class
            DataSetParser.setMRO(currentClass, firstRow.td[0].text())
            DataSetParser.setGroupRepeats(currentClass, firstRow.td[1].text())
            DataSetParser.setDataSetReference(currentClass)
            if (firstRow.td[2].strong.a.size() > 0) {
                DataSetParser.setDataSetReferenceTo(currentClass, firstRow.td[2].strong.a[0].@href.text())
            } else {
                DataSetParser.setDataSetReferenceTo(currentClass, firstRow.td[2].a[0].@href.text())
            }
            Node tdNode = DataSetParser.xmlParser.parseText(XmlUtil.serialize(firstRow.td[2]).replaceFirst("<\\?xml version=\"1.0\".*\\?>", ""))
            tdNode.children()[0].replaceNode {}

            String multiplicityText = XmlUtil.serialize(tdNode).replaceFirst("<\\?xml version=\"1.0\".*\\?>", "")
            multiplicityText = multiplicityText
                .replaceFirst("<td[^>]*>", "")
                .replaceFirst("</td>", "")


            DataSetParser.setMultiplicityText(currentClass, multiplicityText)
        } else if (firstRow.td.size() == 4 && firstRow.td[2].@bgcolor.text() == "#DCDCDC") {
            // Let's start parsing a class
            DataClass childClass = getClassFromTD(firstRow.td[2], dataDictionary)
            //DataClass childClass = new DataClass(label: "Option 2")
            DataSetParser.setMRO(childClass, firstRow.td[0].text())
            DataSetParser.setGroupRepeats(childClass, firstRow.td[1].text())
            DataSetParser.setOrder(childClass, position)
            currentClass.addToDataClasses(childClass)
            Integer noRows = 1
            if (firstRow.td[0].@rowspan) {
                noRows = Integer.parseInt(firstRow.td[0].@rowspan.text())
                //log.debug("noRows: " + noRows)
            }
            if (tableRows[noRows] && tableRows[noRows].td.size() == 6
                && (tableRows[noRows].td[4].text() == "PERSON BIRTH DATE" || tableRows[noRows].td[4].text() == "PERSON_BIRTH_DATE")) {
                noRows++
            }
            List<GPathResult> classRows = tableRows.take(noRows).drop(1)
            parseCDSElementTable(classRows, childClass, dataModel, dataDictionary, position)
            List<GPathResult> otherRows = tableRows.drop(noRows)
            parseCDSElementTable(otherRows, currentClass, dataModel, dataDictionary, position + 1)
        } else if ((firstRow.td.size() == 2 || firstRow.td.size() == 3)
            && firstRow.td[1].@bgcolor.text() == "#DCDCDC") {
            // This is a class like that of "Patient Identity"
            DataClass childClass = getClassFromTD(firstRow.td[1], dataDictionary)
            //DataSetParser.setMRO(childClass, firstRow.td[0].text())
            DataSetParser.setGroupRepeats(childClass, firstRow.td[0].text())
            DataSetParser.setOrder(childClass, position)
            currentClass.addToDataClasses(childClass)
            tableRows = tableRows.drop(1)

            parseCDSElementTable(tableRows, childClass, dataModel, dataDictionary, 1)

        } else if (firstRow.td.size() == 1 && tableRows.size() == 1 && firstRow.td.strong.size() == 1) {
            // This is probably "One of the following two options must be used",
            // "OR", or similar.
            log.debug("Ignoring: " + firstRow.td.strong.text())
        } else if (firstRow.td.size() == 1 && firstRow.td.@bgcolor.text() == "white"
            && firstRow.td.strong.size() == 0
            && tableRows[1].td.size() == 1) {
            // This is in the "BABY" groups, where we're starting a choice
            //log.debug("Blank line")
            //log.debug(tableRows.size())
            tableRows = tableRows.drop(1)

            DataClass choiceClass = getClassFromTD(tableRows[0].td[0], dataDictionary)
            DataSetParser.setOrder(choiceClass, position)
            DataSetParser.setChoice(choiceClass)
            currentClass.addToDataClasses(choiceClass)
            tableRows = tableRows.drop(1)
            List<List<GPathResult>> partitions =
                DataSetParser.partitionList(tableRows,
                                            {it ->
                                                (it.td.size() == 1
                                                    && it.td.strong.size() == 1
                                                    && (it.td.text() == "OR" || it.td.text() == "and"))
                                            })
            //log.debug(partitions.size())
            partitions.eachWithIndex {part, idx ->
                parseCDSElementTable(part, choiceClass, dataModel, dataDictionary, idx + 1)
            }

        } else if (firstRow.td.size() == 4 &&
                   (firstRow.td[2].@bgcolor.text() == "white" || firstRow.td[2].@bgcolor.size() == 0)) {
            parseCDSElement(dataModel, currentClass, firstRow, dataDictionary, position)
            List<GPathResult> nextRows = tableRows.drop(1)
            parseCDSElementTable(nextRows, currentClass, dataModel, dataDictionary, position + 1)
        } else if (firstRow.td.size() == 6 &&
                   (firstRow.td[4].@bgcolor.text() == "white" || firstRow.td[4].@bgcolor.size() == 0)
            && (firstRow.td[4].text() == "PERSON BIRTH DATE" || firstRow.td[4].text() == "PERSON_BIRTH_DATE" || firstRow.td[4].text() ==
                "PROFESSIONAL_REGISTRATION_ISSUER_CODE")) {
            parseCDSElement(dataModel, currentClass, firstRow, dataDictionary, position)
            List<GPathResult> nextRows = tableRows.drop(1)
            parseCDSElementTable(nextRows, currentClass, dataModel, dataDictionary, position + 1)
        } else {
            log.error("Cannot pattern match row: {}\n{}\n{}",
                      dataModel.label,
                      currentClass.label,
                      firstRow.toString())
        }


    }

    static String cleanWhiteSpace(String content) {
        if (!content) return null
        // Replace then trim as NBSP is not whitespace and could be removed to result in WS left over
        String trimmed = content.replaceAll(' ', '').trim()
        trimmed ?: null
    }

    static DataClass getClassFromTD(GPathResult td, NhsDataDictionary dataDictionary) {
        DataClass dataClass = new DataClass(label: "")
        Node tdNode = DataSetParser.xmlParser.parseText(XmlUtil.serialize(td).replaceFirst("<\\?xml version=\"1.0\".*\\?>", ""))

        // Issue where the trimmed content == NBSP so we need to trim it then check that for NBSP
        def firstStringNode = tdNode.depthFirst().find {
            it instanceof String ||
            (it instanceof Node && it.localText().size() > 0 &&
             it.localText().any {cleanWhiteSpace(it)})
        }

        String label

        if (firstStringNode instanceof String) {
            label = firstStringNode
        } else {
            label = (firstStringNode as Node).localText().find {cleanWhiteSpace(it)}
        }
        /*
        dataClass.description = XmlUtil.serialize(tdNode).replaceFirst("<\\?xml version=\"1.0\".*\\?>", "")
        dataClass.description = dataClass.description.replaceFirst(dataClass.label, "")
            .replaceFirst("<td[^>]*>", "")
            .replaceFirst("</td>","")

         */
        dataClass.label = label.replaceFirst("DATA GROUP:", '').trim()

        if (dataClass.label == " ") {
            log.error("NBSP Found for DataClass name. Extracted Label:[{}]\nXML: {}", label, XmlUtil.serialize(tdNode))
        }

        return dataClass

    }

    static String getFirstStringNode(Node node) {
        if (node.children()) {
            node.children().each {it ->
                if ((it instanceof Node)) {
                    return getFirstStringNode(it)
                }

            }

        }
    }


    static List<DataClass> parseCDSSection(List<GPathResult> components, DataModel dataModel, NhsDataDictionary dataDictionary) {
        List<DataClass> returnClasses = []
        DataClass currentClass = null
        int classWebOrder = 0
        int ors = components.count {
            (it.name() == "table" && it.text().trim() == "OR") ||
            (it.name() == "strong" && it.text().trim() == "OR")
        }

        components.each {component ->
            if (DDHelperFunctions.tableIsClassHeader(component)) {
                currentClass = new DataClass(label: component.tbody.tr[0].td[1].text())
                currentClass.label = currentClass.label.replaceFirst("DATA GROUP:", "").trim()

                Node tdNode = null
                if (component.tbody.tr[1].td.size() == 3) {
                    tdNode = DataSetParser.xmlParser.parseText(XmlUtil.serialize(component.tbody.tr[1].td[2]).replaceFirst("<\\?xml version=\"1.0\".*\\?>", ""))
                } else {
                    tdNode = DataSetParser.xmlParser.parseText(XmlUtil.serialize(component.tbody.tr[1].td[0]).replaceFirst("<\\?xml version=\"1.0\".*\\?>", ""))
                }

                currentClass.description = XmlUtil.serialize(tdNode)
                                               .replaceFirst("<\\?xml version=\"1.0\".*\\?>", "")
                                               .replaceFirst("<td[^>]*>", "")
                                               .replaceFirst("</td>", "")
                                               .replaceFirst("FUNCTION:", "")
                                               .trim() ?: null


                String mro = component.tbody.tr[1].td[0].text()
                mro = mro.replaceAll("Group", "")
                mro = mro.replaceAll("Status", "")
                DataSetParser.setMRO(currentClass, mro.trim())
                String groupRepeats = component.tbody.tr[1].td[1].text()
                groupRepeats = groupRepeats.replaceAll("Group", "")
                groupRepeats = groupRepeats.replaceAll("Repeats", "")
                DataSetParser.setGroupRepeats(currentClass, groupRepeats.trim())

                if (ors > 0) {
                    DataSetParser.setChoice(currentClass)
                }

                if (component.tbody.tr.size() > 2) {
                    //log.debug("Got it!")
                    //log.debug(component.tbody.tr[1].td.text())

                    List<GPathResult> tableRows = []
                    component.tbody.tr.each {tr -> tableRows.add(tr)}
                    tableRows = tableRows.drop(2)
                    parseCDSElementTable(tableRows, currentClass, dataModel, dataDictionary, classWebOrder)
                }
                DataSetParser.setOrder(currentClass, classWebOrder)
                classWebOrder++
                returnClasses.add(currentClass)


            } else if (component.name() == "table" && component.tbody.tr.size() >= 1 && currentClass) {
                if (ors > 0 && component.tbody.tr.count {tr -> tr.td[0]."@bgcolor".text() == "#DCDCDC"} > 1) {
                    log.debug("Found an exception: " + dataModel.label)
                    log.debug(component.tbody.tr[0].td[1].text() + " " + component.tbody.tr[0].td[2].text())
                    component.tbody.tr.findAll {tr -> tr.td[0]."@bgcolor".text() == "#DCDCDC"}.each {tr ->
                        log.debug(tr.td[1].text() + " " + tr.td[2].text())
                    }
                    DataClass andClass = new DataClass(label: "And")
                    DataSetParser.setAnd(andClass)
                    currentClass.addToDataClasses(andClass)
                    List<GPathResult> tableRows = []
                    component.tbody.tr.each {tr -> tableRows.add(tr)}
                    parseCDSElementTable(tableRows, andClass, dataModel, dataDictionary, classWebOrder)
                    DataSetParser.setOrder(andClass, classWebOrder)
                    classWebOrder++

                } else {
                    List<GPathResult> tableRows = []
                    component.tbody.tr.each {tr -> tableRows.add(tr)}
                    parseCDSElementTable(tableRows, currentClass, dataModel, dataDictionary, classWebOrder)
                    DataSetParser.setOrder(currentClass, classWebOrder)
                    classWebOrder++
                }
            }

        }
        return returnClasses

    }

    static void parseCDSElement(DataModel dataModel, DataClass currentClass, GPathResult tr, NhsDataDictionary dataDictionary, int position) {
        if (tr.td[2].a.size() == 6) {
            //
            DataSetParser.setChoice(currentClass)
            DataElement dataElement1 = DataSetParser.getElementFromText(tr.td[2].a[0], dataModel, dataDictionary, currentClass.label)

            DataSetParser.setOrder(dataElement1, 1)
            currentClass.addToImportedDataElements(dataElement1)

            DataClass andClass = new DataClass(label: "And")
            DataSetParser.setOrder(andClass, 2)
            DataSetParser.setAnd(andClass)
            List<DataElement> deList = [dataElement1]
            for (int i = 1; i < 6; i++) {
                DataElement dataElement = DataSetParser.getElementFromText(tr.td[2].a[i], dataModel, dataDictionary, currentClass.label)
                DataSetParser.setOrder(dataElement, i)
                andClass.addToImportedDataElements(dataElement)
                deList.add(dataElement)
            }
            currentClass.addToDataClasses(andClass)
            DataSetParser.getAndSetMRO(tr.td[0], deList, currentClass)
            DataSetParser.getAndSetRepeats(tr.td[1], deList, currentClass)
            DataSetParser.getAndSetRules(tr.td[3], deList, currentClass)
        } else if (tr.td[2].a.size() == 4) {
            // Address
            DataClass choiceClass = new DataClass(label: "Choice")
            DataSetParser.setOrder(choiceClass, position)
            DataSetParser.setChoice(choiceClass)
            if (tr.td[2].a[0].text().contains("NAME")) {
                DataSetParser.setNameChoice(choiceClass)
            } else {
                DataSetParser.setAddress(choiceClass)
            }
            DataElement dataElement1 = DataSetParser.getElementFromText(tr.td[2].a[0], dataModel, dataDictionary, currentClass.label)
            DataSetParser.getAndSetMRO(tr.td[0], [dataElement1], choiceClass)
            DataSetParser.getAndSetRepeats(tr.td[1], [dataElement1], choiceClass)
            DataSetParser.getAndSetRules(tr.td[3], [dataElement1], choiceClass)

            //DataElement dataElement2 = getElementFromText(tr.td[2].a[2], dataModel, dataDictionary, currentClass.label)
            DataSetParser.setOrder(dataElement1, 1)
            //setOrder(dataElement2, 2)
            choiceClass.addToImportedDataElements(dataElement1)
            //choiceClass.addToImportedDataElements(dataElement2)
            currentClass.addToDataClasses(choiceClass)


        } else if ((tr.td[2].em && tr.td[2].em.text().equalsIgnoreCase("Or")) ||
                   (tr.td[2].strong && tr.td[2].strong.text().equalsIgnoreCase("OR"))) {
            DataClass choiceClass = new DataClass(label: "Choice")
            DataSetParser.setOrder(choiceClass, position)
            DataSetParser.setChoice(choiceClass)
            DataElement dataElement1 = DataSetParser.getElementFromText(tr.td[2].a[0], dataModel, dataDictionary, currentClass.label)
            DataElement dataElement2 = DataSetParser.getElementFromText(tr.td[2].a[1], dataModel, dataDictionary, currentClass.label)

            DataSetParser.getAndSetMRO(tr.td[0], [dataElement1, dataElement2], choiceClass)
            DataSetParser.getAndSetRepeats(tr.td[1], [dataElement1, dataElement2], choiceClass)
            DataSetParser.getAndSetRules(tr.td[3], [dataElement1, dataElement2], choiceClass)

            DataSetParser.setOrder(dataElement1, 1)
            DataSetParser.setOrder(dataElement2, 2)
            choiceClass.addToImportedDataElements(dataElement1)
            choiceClass.addToImportedDataElements(dataElement2)
            currentClass.addToDataClasses(choiceClass)
        } else {
            if (tr.td.size() == 6) {
                // rare occurrence with unmerged first columns
                DataElement dataElement = DataSetParser.getElementFromText(tr.td[4].a, dataModel, dataDictionary, currentClass.label)
                DataSetParser.getAndSetMRO(tr.td[2], [dataElement])
                DataSetParser.getAndSetRepeats(tr.td[3], [dataElement])
                DataSetParser.getAndSetRules(tr.td[5], [dataElement])
                DataSetParser.setOrder(dataElement, position)
                currentClass.addToImportedDataElements(dataElement)

            } else {
                DataElement dataElement = null
                if (tr.td[2].p.size() == 1) {
                    // There's one example where the element is wrapped in a <p>
                    dataElement = DataSetParser.getElementFromText(tr.td[2].p.a, dataModel, dataDictionary, currentClass.label)
                } else {
                    dataElement = DataSetParser.getElementFromText(tr.td[2].a, dataModel, dataDictionary, currentClass.label)
                }
                // assume 4
                DataSetParser.getAndSetMRO(tr.td[0], [dataElement])
                DataSetParser.getAndSetRepeats(tr.td[1], [dataElement])
                DataSetParser.getAndSetRules(tr.td[3], [dataElement])
                DataSetParser.setOrder(dataElement, position)
                currentClass.addToImportedDataElements(dataElement)
            }
        }
    }
}
