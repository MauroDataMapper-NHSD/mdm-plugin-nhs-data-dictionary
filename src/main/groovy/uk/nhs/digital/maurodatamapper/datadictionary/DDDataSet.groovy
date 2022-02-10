package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

import groovy.util.logging.Slf4j
import groovy.xml.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.CDSDataSetParser
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.DataSetParser
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Body
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Topic
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.XRef
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.ColSpec
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.Entry
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.Row
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.TBody
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.TGroup
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.THead
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.Table

@Slf4j
class DDDataSet extends DataDictionaryComponent<DataModel> {


    //def clazz
    def property

    def explanatoryWebPageDefinition

    def overview

    Map catalogueXml

    List<DDAttribute> attributes = []
    List<Map<String, String>> dataClassDescriptions = []

    @Override
    void fromXml(Object xml) {

        //clazz = xml."class"
        definition = xml.definition

        property = xml.property

        super.fromXml(xml)

        this.base_uri = base_uri.replaceFirst("/Content/", "/")

        this.overview = definition
        //((GPathResult)overview).children().dropWhile {it.name() != "table"}.each {it.replaceNode {}}

    }

    /*    void createAttributes(Collection<DDAttribute> allAttributes) {

            property.each { property ->
                DDAttribute referencedAttribute = allAttributes.find{ it.uin == property.referencedElement.text() }
                if(referencedAttribute) {
                    DDAttribute existingAttribute = attributes.find {it.name == referencedAttribute.name}
                    if(!existingAttribute) {
                        attributes.add(referencedAttribute)
                    }
                    else {
                        log.debug("Already existing data element '${existingAttribute.name}' in class '${name}'")
                    }
                } else {
                    log.debug("Cannot de-reference attribute from dataset: " + property.referencedElement.text())
                }

            }

        }
    */

    void addDataClassDescriptions(String dataModelId, def catalogueXml, String parentId = null) {

        if (catalogueXml.id && catalogueXml.description) {
            Map<String, String> obj = [:]
            obj["dataClassId"] = catalogueXml.id
            obj["parentClassId"] = parentId
            obj["description"] = catalogueXml.description
            obj["dataModelId"] = dataModelId
            obj["dataClassLabel"] = catalogueXml.label
            dataClassDescriptions.add(obj)
        }
        if (catalogueXml.dataClasses) {
            catalogueXml.dataClasses.each {
                addDataClassDescriptions(dataModelId, it, catalogueXml.id)
            }
        }

    }


    void joinExplanatoryPage(DataDictionary dataDictionary) {
        def overViewPage = dataDictionary.webPages.values().find {it.uin == stringValues["explanatoryPage"]}
        if (overViewPage) {
            explanatoryWebPageDefinition = overViewPage.definition
        }
    }

    @Override
    DataModel toCatalogueItem(DataDictionary dataDictionary) {

        String label = DDHelperFunctions.tidyLabel(name)
        String description = null
        if (explanatoryWebPageDefinition) {
            description = DDHelperFunctions.parseHtml(explanatoryWebPageDefinition)
        } else {
            if (overview) {
                description = DDHelperFunctions.parseHtml(overview)
            } else {
                log.warn("No description found for data set: " + name)
            }
        }
        DataModel catalogueItem = new DataModel(label: label, description: description)
        addAllMetadata(catalogueItem)
        DDHelperFunctions.addMetadata(catalogueItem, "overview", DDHelperFunctions.parseHtml(overview) ?: '')

        if (label.startsWith("CDS")) {
            CDSDataSetParser.parseCDSDataSet((GPathResult) definition, catalogueItem, (NhsDataDictionary) dataDictionary)
        } else {
            DataSetParser.parseDataSet((GPathResult) definition, catalogueItem, (NhsDataDictionary) dataDictionary)
        }

        return catalogueItem
    }

    List<String> getPath() {
        List<String> path
        try {
            path = DDHelperFunctions.getPath(base_uri, "Messages", ".txaClass20")
        } catch (Exception e) {
            path = DDHelperFunctions.getPath(base_uri, "Web_Site_Content", ".txaClass20")
        }
        path.removeAll {it.equalsIgnoreCase("Data_Sets")}
        path.removeAll {it.equalsIgnoreCase("Content")}

        if (stringValues["isRetired"] == "true") {
            path.add(0, "Retired")
        }
        path = path.collect {DDHelperFunctions.tidyLabel(it)}
        return path
    }

    @Override
    void fromCatalogueExport(DataDictionary dataDictionary, def catalogueXml, UUID parentId, UUID containerId) {

        //clazz = xml."class"

        this.catalogueXml = catalogueXml

        definition = catalogueXml.description
        name = catalogueXml.label


        catalogueId = UUID.fromString(catalogueXml.id)
        parentCatalogueId = parentId
        containerCatalogueId = containerId

        super.fromCatalogueExport(dataDictionary, catalogueXml, parentId, containerId)
        catalogueItem = new DataModel(catalogueXml)
    }

    @Override
    void fromCatalogueItem(DataDictionary dataDictionary, DataModel dataModel, UUID parentId, UUID containerId, MetadataService metadataService) {

        //clazz = xml."class"

        definition = dataModel.description
        name = dataModel.label

        catalogueId = dataModel.id
        this.parentCatalogueId = parentId
        this.containerCatalogueId = containerId

        super.fromCatalogueItem(dataDictionary, dataModel, parentId, containerId, metadataService)


        catalogueItem = dataModel
    }

    /*    @Override
        void updateDescriptionInCatalogue(BindingMauroDataMapperClient mdmClient) {
            mdmClient.updateDataModelDescription(catalogueId, definition)
        }
    */

    @Override
    String getDitaKey() {
        return "dataset_" + DDHelperFunctions.makeValidDitaName(name)
    }

    @Override
    String getOutputClassForLink() {
        String outputClass = "dataSet"
        if (stringValues["isRetired"] == "true") {
            outputClass += " retired"
        }
        return outputClass

    }

    @Override
    String getOutputClassForTable() {
        return "Data Set"
    }

    @Override
    List<Topic> toDitaTopics(DataDictionary dataDictionary) {
        List<String> newPath = getPath().collect {it -> DDHelperFunctions.makeValidDitaName(it)}

        Topic dataSetOverviewTopic = createSubTopic("Overview")
        String ov = DDHelperFunctions.parseHtml(definition) ?: ''
        if (!ov || ov == "null") {
            ov = DDHelperFunctions.parseHtml(stringValues["overview"])
        }
        dataSetOverviewTopic.body.bodyElements.add(new Html(content: "<div><p>" + ov + "</p></div>"))

        List<Topic> returnTopics = [dataSetOverviewTopic]
        if (!isRetired) {
            Topic dataSetSpecificationTopic = createSubTopic("Specification")

            if (catalogueItem && catalogueItem.dataClasses && catalogueItem.dataClasses.size() > 0) {
                catalogueItem.dataClasses.sort {
                    dataClass -> Integer.parseInt(DDHelperFunctions.getMetadataValue(dataClass, "Web Order") ?: "1")
                }.each {dataClass ->
                    if (catalogueItem.label.startsWith("CDS")) {
                        outputCDSClassAsDita(dataClass, dataSetSpecificationTopic.body, dataDictionary, catalogueItem)
                    } else {
                        outputClassAsDita(dataClass, dataSetSpecificationTopic.body, dataDictionary)
                    }
                }
            }

            returnTopics.add(dataSetSpecificationTopic)
        }

        Topic aliasTopic = getAliasTopic()
        if (aliasTopic) {
            returnTopics.add(aliasTopic)
        }

        return returnTopics
    }

    @Override
    String getTypeText() {
        return "data set"
    }

    @Override
    String getTypeTitle() {
        return "Data Set"
    }

    static void outputClassAsDita(DataClass dataClass, Body body, DataDictionary dataDictionary) {

        if (DataSetParser.isChoice(dataClass) && dataClass.label.startsWith("Choice")) {
            body.bodyElements.add(new Html(content: "<b>One of the following options must be used:</b>"))
            getSortedChildElements(dataClass).eachWithIndex {childDataClass, idx ->
                if (idx != 0) {
                    body.bodyElements.add(new Html(content: "<b>Or</b>"))
                }
                if (childDataClass instanceof DataClass) {
                    outputClassAsDita((DataClass) childDataClass, body, dataDictionary)
                }

            }


        } else {
            Table table = new Table()
            TGroup tGroup = new TGroup(cols: 2)
            table.tGroups.add(tGroup)
            ColSpec column1 = new ColSpec(colNum: 1, colName: "col1", colWidth: "2*", align: "center")
            ColSpec column2 = new ColSpec(colNum: 2, colName: "col2", colWidth: "8*")
            tGroup.colSpecs.addAll([column1, column2])
            THead tHead = new THead()
            tGroup.tHead = tHead
            Row headerRow1 = new Row(outputClass: "thead-light")
            tHead.rows.add(headerRow1)
            Entry nameEntry = new Entry(nameSt: "col1", nameEnd: "col2")
            nameEntry.contents.add(new Html(content: "<b>${dataClass.label}</b>"))
            if (dataClass.description) {
                nameEntry.contents.add(new Html(content: "<p>${dataClass.description}</p>"))
            }
            headerRow1.entries.add(nameEntry)

            if (dataClass.dataElements && dataClass.dataElements.size() > 0) {
                Row headerRow2 = new Row()
                Entry mandationHeader = new Entry()
                mandationHeader.contents.add(new Html(content: "Mandation"))
                Entry dataElementsHeader = new Entry()
                dataElementsHeader.contents.add(new Html(content: "Data Elements"))
                headerRow2.entries.addAll([mandationHeader, dataElementsHeader])
                tHead.rows.add(headerRow2)
            }

            TBody tBody = new TBody()
            tGroup.tBody = tBody

            addAllChildRows(dataClass, tBody, dataDictionary)

            body.bodyElements.add(table)
        }
    }

    static void addSubClass(DataClass dataClass, TBody tBody, DataDictionary dataDictionary) {

        Row headerRow = new Row(outputClass: "table-primary")
        Entry mandationEntry = new Entry(nameSt: "col1", nameEnd: "col1")
        mandationEntry.contents.add(new Html(content: "<b>Mandation</b>"))
        Entry nameEntry = new Entry(nameSt: "col2", nameEnd: "col2")
        nameEntry.contents.add(new Html(content: "<b>${dataClass.label}</b>"))
        if (dataClass.description) {
            nameEntry.contents.add(new Html(content: "<p>${dataClass.description}</p>"))
        }
        if (dataClass.label != "Choice") {
            headerRow.entries.addAll([
                mandationEntry,
                nameEntry])
            tBody.rows.add(headerRow)
        }
        addAllChildRows(dataClass, tBody, dataDictionary)

    }

    static void addAllChildRows(DataClass parentDataClass, TBody tBody, DataDictionary dataDictionary) {
        if (parentDataClass.dataElements) {
            getSortedChildElements(parentDataClass).each {child ->
                addChildRow(child, tBody, dataDictionary)
            }

        } else {
            // no dataElements
            getSortedChildElements(parentDataClass).eachWithIndex {childDataClass, idx ->
                if (DataSetParser.isChoice(parentDataClass) && idx != 0) {
                    Entry entry = new Entry("<b>Or</b>")
                    entry.nameSt = "col1"
                    entry.nameEnd = "col2"
                    tBody.rows.add(new Row(entries: [entry]))
                }
                addSubClass((DataClass) childDataClass, tBody, dataDictionary)
            }
        }
    }


    static void addChildRow(CatalogueItem child, TBody tBody, DataDictionary dataDictionary) {
        Row elementRow = new Row(entries: [])
        tBody.rows.add(elementRow)
        elementRow.entries.add(new Entry(DataSetParser.getMRO(child)))
        String xrefs = getXRefStringFromItem(child, dataDictionary)
        elementRow.entries.add(new Entry(xrefs))

    }


    static void outputCDSClassAsDita(DataClass dataClass, Body body, DataDictionary dataDictionary, DataModel dataModel) {

        int totalDepth = calculateClassDepth(dataClass)

        if (DataSetParser.isChoice(dataClass) && dataClass.label.startsWith("Choice")) {
            body.bodyElements.add(new Html(content: "<p> <b>One of the following options must be used:</b></p>"))
            dataClass.dataClasses
                .sort {childDataClass -> Integer.parseInt(DDHelperFunctions.getMetadataValue(childDataClass, "Web Order") ?: "1")}
                .eachWithIndex {childDataClass, idx ->
                    outputCDSClassAsDita(childDataClass, body, dataDictionary, dataModel)
                    if (idx < dataClass.dataClasses.size() - 1) {
                        body.bodyElements.add(new Html(content: "<p> <b>Or</b></p>"))
                    }
                }


        } else if (!dataClass.dataClasses && !dataClass.dataElements && DataSetParser.isDataSetReference(dataClass)) {
            // We've got a pointer to another dataset
            //String linkedDataSetUrl = DataSetParser.getDataSetReferenceTo(dataClass)
            //log.debug(dataClass.label.replaceFirst("DATA GROUP: ", ""))
            DDDataSet linkedDataSet = dataDictionary.dataSets.values().find {
                it.ddUrl == DataSetParser.getDataSetReferenceTo(dataClass)
            }
            if (!linkedDataSet) {
                log.error("Cannot link dataset: ${DataSetParser.getDataSetReferenceTo(dataClass)}")
            }
            Table table = addTopLevelCDSHeader(dataClass, linkedDataSet.name,
                                               dataClass.description, totalDepth, false)

            Entry descriptionEntry = new Entry(nameSt: "col3", nameEnd: "col${totalDepth * 2 + 4}")
            descriptionEntry.contents = [
                new Html(content: "<p> <b>Data Group:</b>" + linkedDataSet.getXRef()
                    .outputAsString(linkedDataSet.name) + "</p>" +
                                  "<p>${DataSetParser.getMultiplicityText(dataClass)}</p>")
            ]

            Row dataSetReferenceRow = new Row(entries: [
                new Entry(contents: [new Html(content: DataSetParser.getMRO(dataClass))]),
                new Entry(contents: [new Html(content: DataSetParser.getGroupRepeats(dataClass))]),
                descriptionEntry
            ])
            dataSetReferenceRow.entries[0].align = "center"
            dataSetReferenceRow.entries[1].align = "center"
            table.tGroups[0].tBody.rows.add(dataSetReferenceRow)

            body.bodyElements.add(table)

        } else {
            String label = dataClass.label
            if (label.endsWith(" 1")) {
                label = label.replaceAll(" 1", "")
            }
            Table table = addTopLevelCDSHeader(dataClass, label, dataClass.description, totalDepth)

            addCDSClassContents(dataClass, table.tGroups[0].tBody, dataModel, dataDictionary, totalDepth, 0)

            body.bodyElements.add(table)
        }
    }

    static int calculateClassDepth(DataClass dataClass) {
        if (dataClass.dataElements) {
            return 0
        } else {
            int maxDepth = 0
            dataClass.dataClasses.each {childDataClass ->
                int thisDepth = calculateClassDepth(childDataClass)
                if (thisDepth > maxDepth) {
                    maxDepth = thisDepth
                }
            }
            return maxDepth + 1
        }

    }

    static int calculateClassRows(DataClass dataClass) {

        if (DataSetParser.isChoice(dataClass) && (!dataClass.dataClasses || dataClass.dataClasses.size() == 0)) {
            return 0 // nominal header row will be added later on
        }
        int total = 0
        if (dataClass.dataElements) {
            total += dataClass.dataElements.size()
        }

        dataClass.dataClasses.each {
            total += calculateClassRows(it) + 1
        }
        if (DataSetParser.isChoice(dataClass) && dataClass.dataClasses && dataClass.dataClasses.size() > 0) {
            // To cope with the 'or' rows
            total += dataClass.dataClasses.size()
        }

        log.debug("Data Class Rows: ${dataClass.label} - ${total}")
        return total

    }


    static Table addTopLevelCDSHeader(DataClass dataClass, String groupName, String function, int totalDepth, boolean includeMRO = true) {
        List<String> columnWidths = []
        for (int i = 0; i < totalDepth * 2; i++) {
            columnWidths.add("1*")
        }
        columnWidths.addAll(["1*", "1*", "8*", "1*"])
        Table table = new Table(columnWidths)
        /*for(int i=0;i<totalDepth*2+4;i++) {
            table.tGroups[0].colSpecs[i].align = "center"
        }
        table.tGroups[0].colSpecs[totalDepth*2+2].align = "left"
        */
        Row headerRow1 = new Row()
        Entry entry1 = new Entry(nameSt: "col1", nameEnd: "col2", align: "center")
        entry1.contents.add(new XRef(keyref: "supporting_definition_commissioning_data_set_notation", text: "Notation"))
        Entry entry2 = new Entry(nameSt: "col3", nameEnd: "col${totalDepth * 2 + 4}", outputClass: "thead-light")
        entry2.contents.add(new Html(content: "<p> <b>Data Group:</b> ${groupName}</p>"))
        headerRow1.entries.addAll([entry1, entry2])
        Row headerRow2 = new Row(outputClass: "thead-light")
        Entry entry3 = new Entry(nameSt: "col1", nameEnd: "col1", align: "center")
        String groupStatusString = "<p>Group Status</p>"
        if (includeMRO) {
            groupStatusString += "<p>${DataSetParser.getMRO(dataClass)}</p>"
        }
        String groupRepeatsString = "<p>Group Repeats</p>"
        if (includeMRO) {
            groupRepeatsString += "<p>${DataSetParser.getGroupRepeats(dataClass)}</p>"
        }
        entry3.contents.add(new Html(content: groupStatusString))
        Entry entry4 = new Entry(nameSt: "col2", nameEnd: "col2", align: "center")
        entry4.contents.add(new Html(content: groupRepeatsString))
        Entry entry5 = new Entry(nameSt: "col3", nameEnd: "col${totalDepth * 2 + 4}")
        entry5.contents.add(new Html(content: "<p> <b>Function:</b> ${function}</p>"))
        headerRow2.entries.addAll([entry3, entry4, entry5])
        table.tGroups[0].tHead.rows.addAll(headerRow1, headerRow2)
        return table

    }

    static void addCDSClassHeader(DataClass dataClass, TBody tBody, DataModel dataModel, DataDictionary dataDictionary, int totalDepth, int currentDepth) {
        int moreRows = calculateClassRows(dataClass)
        Row headerRow = new Row(outputClass: "thead-light table-primary")
        Entry nameEntry = new Entry(nameSt: "col${currentDepth * 2 + 1}", nameEnd: "col${totalDepth * 2 + 3}")
        String name = dataClass.label
        if (name.endsWith(" 1")) {
            name = name.replace(" 1", "")
        }
        nameEntry.contents.add(new Html(content: "<b>${name}</b>"))
        if (dataClass.description) {
            log.debug("description")
            log.debug(dataClass.description)
            String description = dataClass.description
            description = description
                .replace(
                    "<a href=\"https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/nhs_number_status_indicator_code_(baby)" +
                    ".html\">NHS_NUMBER_STATUS_INDICATOR_CODE_(BABY)</a>",
                    "<xref keyref=\"element_nhs_number_status_indicator_code__baby_\">NHS STATUS INDICATOR CODE (BABY)</xref>")
                .replace("<a href=\"https://v3.datadictionary.nhs.uk/web_site_content/supporting_definitions/secondary_uses_service.html\">Secondary_Uses_Service</a>",
                         "<xref keyref=\"supporting_definition_secondary_uses_service\">Secondary Uses Service</xref>")
                .replace(
                    "<a href=\"https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/nhs_number_status_indicator_code.html\">NHS_NUMBER_STATUS_INDICATOR_CODE</a>",
                    "<xref keyref=\"element_nhs_number_status_indicator_code\">NHS NUMBER STATUS INDICATOR CODE</xref>")
                .replace(
                    "<a href=\"https://v3.datadictionary.nhs.uk/data_dictionary/messages/cds_v6-2_type_003_-_cds_message_header.html\">CDS V6-2 Type 003 - Commissioning " +
                    "Data Set Message Header</a>",
                    "<xref keyref=\"dataset_cds_v6-2_type_003_-_cds_message_header\">CDS V6-2 Type 003 - Commissioning Data Set Message Header</xref>")
            nameEntry.contents.add(new Html(content: "<p>${description}</p>"))
        }
        Entry rulesEntry = new Entry()
        rulesEntry.contents.add(new XRef(keyref: "supporting_definition_commissioning_data_set_business_rules", text: "Rules"))
        headerRow.entries.addAll([
            new Entry(DataSetParser.getMRO(dataClass)),
            new Entry(DataSetParser.getGroupRepeats(dataClass)),
            nameEntry,
            rulesEntry])
        headerRow.entries[0].moreRows = moreRows
        headerRow.entries[1].moreRows = moreRows
        headerRow.entries[0].align = "center"
        headerRow.entries[1].align = "center"
        headerRow.entries[3].align = "center"
        tBody.rows.add(headerRow)
    }

    static void addCDSClassContents(DataClass dataClass, TBody tBody, DataModel dataModel, DataDictionary dataDictionary, int totalDepth, int currentDepth) {
        if (dataClass.dataElements) {
            getSortedChildElements(dataClass).each {child ->
                addCDSChildRow(child, tBody, dataDictionary, totalDepth, currentDepth)
            }
        } else {
            // no dataElements
            if (dataClass.dataClasses) {
                if (DataSetParser.isChoice(dataClass)) {
                    Entry choiceEntry = new Entry(contents: [new Html(content: "<p> <b>One of the following DATA GROUPS must be used:</b></p>")])
                    choiceEntry.nameSt = "col${currentDepth * 2 + 1}"
                    choiceEntry.nameEnd = "col${totalDepth * 2 + 4}"
                    tBody.rows.add(new Row(entries: [choiceEntry]))
                }
                List<DataClass> sortedClasses = getSortedChildElements(dataClass)
                sortedClasses.each {childDataClass ->
                    addCDSSubClass(childDataClass, tBody, dataModel, dataDictionary, totalDepth, currentDepth + 1)
                    if (DataSetParser.isChoice(dataClass) &&
                        childDataClass != sortedClasses.last()) {
                        Entry choiceEntry = new Entry(contents: [new Html(content: "<p> <b>Or</b></p>")])
                        choiceEntry.nameSt = "col${currentDepth * 2 + 1}"
                        choiceEntry.nameEnd = "col${totalDepth * 2 + 4}"
                        tBody.rows.add(new Row(entries: [choiceEntry]))
                    }
                }
            } else {
                log.error("{}>{} No data elements or data classes... ",
                          dataModel.label,
                          dataClass.label)
            }
        }
    }

    static void addCDSSubClass(DataClass dataClass, TBody tBody, DataModel dataModel, DataDictionary dataDictionary, int totalDepth, int currentDepth) {
        addCDSClassHeader(dataClass, tBody, dataModel, dataDictionary, totalDepth, currentDepth)
        addCDSClassContents(dataClass, tBody, dataModel, dataDictionary, totalDepth, currentDepth)
    }

    static void addCDSChildRow(CatalogueItem child, TBody tBody, DataDictionary dataDictionary, int totalDepth, int currentDepth) {
        if (child instanceof DataElement) {
            String uin = DDHelperFunctions.getMetadataValue(child, "uin")
            if (uin) {
                DDElement ddElement = dataDictionary.elements[uin]
                Row elementRow = new Row(entries: [])
                if (ddElement) {

                    elementRow.entries.addAll([new Entry(DataSetParser.getMRO(child)),
                                               new Entry(DataSetParser.getGroupRepeats(child)),
                                               new Entry(ddElement.getXRef()),
                                               new Entry(DataSetParser.getRules(child))])
                    tBody.rows.add(elementRow)
                } else {
                    log.error("Cannot lookup element: " + uin)
                    elementRow.entries.addAll([new Entry(DataSetParser.getMRO(child)),
                                               new Entry(DataSetParser.getGroupRepeats(child)),
                                               new Entry(child.label),
                                               new Entry(DataSetParser.getRules(child))])
                    tBody.rows.add(elementRow)
                }
                elementRow.entries[0].align = "center"
                elementRow.entries[1].align = "center"
                elementRow.entries[3].align = "center"
                elementRow.entries[2].nameSt = "col${currentDepth * 2 + 3}"
                elementRow.entries[2].nameEnd = "col${totalDepth * 2 + 3}"
            } else {
                log.error("Cannot find uin: " + child.label)
            }
        } else if (child instanceof DataClass) {
            if (DataSetParser.isChoice(child) && child.label.startsWith("Choice")) {
                Row elementRow = new Row(entries: [])
                String mro = ""
                String groupRepeats = ""
                String xRefs = ""
                String rules = ""
                ((DataClass) child).dataElements
                    .sort {childDataElement -> Integer.parseInt(DDHelperFunctions.getMetadataValue(childDataElement, "Web Order") ?: "1")}
                    .eachWithIndex {childDataElement, idx ->
                        String uin = DDHelperFunctions.getMetadataValue(childDataElement, "uin")
                        DDElement ddElement = dataDictionary.elements[uin]
                        if (idx != 0) {
                            xRefs += "<p>Or</p>"
                            mro += "<p>Or</p>"
                            groupRepeats += "<p>Or</p>"
                            rules += "<p>-</p>"
                        }
                        mro += "<p>${DataSetParser.getMRO(childDataElement)}</p>"
                        groupRepeats += "<p>${DataSetParser.getGroupRepeats(childDataElement)}</p>"
                        rules += "<p>${DataSetParser.getRules(childDataElement)}</p>"

                        if (ddElement) {
                            xRefs += "<p>${ddElement.getXRef().outputAsString(ddElement.name)}</p>"
                        } else {
                            xRefs += "<p>${childDataElement.label}</p>"
                            log.error("no ddElement! ${uin} ${childDataElement.label}")
                        }
                    }
                elementRow.entries.addAll([
                    new Entry(mro),
                    new Entry(groupRepeats),
                    new Entry(xRefs),
                    new Entry(rules)
                ])
                elementRow.entries[0].align = "center"
                elementRow.entries[1].align = "center"
                elementRow.entries[3].align = "center"

                elementRow.entries[2].nameSt = "col${currentDepth * 2 + 3}"
                elementRow.entries[2].nameEnd = "col${totalDepth * 2 + 3}"
                tBody.rows.add(elementRow)
            }
        }
    }


    static List<CatalogueItem> getSortedChildElements(DataClass dataClass) {
        List<CatalogueItem> children = []
        if (dataClass.dataElements) {
            children.addAll(dataClass.dataElements)
        }
        if (dataClass.dataClasses) {
            children.addAll(dataClass.dataClasses)
        }

        return children.sort {
            child -> Integer.parseInt(DDHelperFunctions.getMetadataValue(child, "Web Order") ?: "1")
        }

    }

    static String getXRefStringFromItem(CatalogueItem catalogueItem, DataDictionary dataDictionary) {
        if (catalogueItem instanceof DataElement) {
            return getXRefStringFromDataElement((DataElement) catalogueItem, dataDictionary)
        } else if (catalogueItem instanceof DataClass) {
            String xRefs = ""
            if (DataSetParser.isAddress(catalogueItem)) {
                DataElement childElement = (DataElement) getSortedChildElements(catalogueItem)[0]
                String link = getXRefStringFromItem(childElement, dataDictionary)
                xRefs += link.replace("</p>", " - <xref outputclass=\"class\" keyref=\"class_address_structured\">ADDRESS STRUCTURED</xref></p>")
                xRefs += "<p>Or</p>"
                xRefs += link.replace("</p>", " - <xref outputclass=\"class\" keyref=\"class_address_unstructured\">ADDRESS UNSTRUCTURED</xref></p>")
            } else if (DataSetParser.isChoice(catalogueItem) && catalogueItem.label.startsWith("Choice")) {
                getSortedChildElements(catalogueItem).eachWithIndex {childItem, idx ->
                    if (idx != 0) {
                        xRefs += "<p>Or</p>"
                    }
                    xRefs += getXRefStringFromItem(childItem, dataDictionary)
                }
            } else if (DataSetParser.isAnd(catalogueItem) && (catalogueItem.label.startsWith("Choice") || catalogueItem.label.startsWith("And"))) {
                getSortedChildElements(catalogueItem).eachWithIndex {childItem, idx ->
                    if (idx != 0) {
                        xRefs += "<p>And</p>"
                    }
                    xRefs += getXRefStringFromItem(childItem, dataDictionary)
                }
            } else if (DataSetParser.isInclusiveOr(catalogueItem) && catalogueItem.label.startsWith("Choice")) {
                getSortedChildElements(catalogueItem).eachWithIndex {childItem, idx ->
                    if (idx != 0) {
                        xRefs += "<p>And/Or</p>"
                    }
                    xRefs += getXRefStringFromItem(childItem, dataDictionary)
                }
            }
            return xRefs
        }
    }


    static String getXRefStringFromDataElement(DataElement dataElement, DataDictionary dataDictionary) {
        String uin = DDHelperFunctions.getMetadataValue(dataElement, "uin")
        DDElement ddElement = dataDictionary.elements[uin]
        String retValue
        if (ddElement) {
            retValue = "<p>${ddElement.getXRef().outputAsString(ddElement.name)}</p>"
        } else {
            log.error("Cannot find uin: ${dataElement.label}, ${uin}")
            retValue = "<p>${dataElement.label}</p>"
        }
        if (dataElement.maxMultiplicity == -1) {
            retValue += "<p>Multiple occurrences of this item are permitted</p>"
        }
        return retValue
    }

    @Override
    String getShortDesc(DataDictionary dataDictionary) {
        if (isPrepatory) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {

            List<String> aliases = [name]
            aliasFields.keySet().each {aliasType ->
                if (stringValues[aliasType]) {
                    aliases.add(stringValues[aliasType])
                }
            }


            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + DDHelperFunctions.parseHtml(definition.toString()) + "</xml>")
                String allParagraphs = ""
                xml.p.each {paragraph ->
                    allParagraphs += paragraph.text()
                }
                String nextSentence = ""
                while (!aliases.find {it -> nextSentence.contains(it)} && allParagraphs.contains(".")) {
                    nextSentence = allParagraphs.substring(0, allParagraphs.indexOf(".") + 1)
                    if (aliases.find {it -> nextSentence.contains(it)}) {
                        if (nextSentence.startsWith("Introduction")) {
                            nextSentence = nextSentence.replaceFirst("Introduction", "")
                        }
                        return nextSentence
                    }
                    allParagraphs = allParagraphs.substring(allParagraphs.indexOf(".") + 1)
                }
                return name
            } catch (Exception e) {
                log.error("Couldn't parse: " + definition)
                return name
            }
        }
    }

    @Override
    String getInternalLink() {
        return "](dm:${DDHelperFunctions.tidyLabel(name)})"
    }


}
