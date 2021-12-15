package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import uk.nhs.digital.maurodatamapper.datadictionary.DDDataSet
import uk.nhs.digital.maurodatamapper.datadictionary.DDElement
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionaryComponent
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.CDSDataSetParser
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.DataSetParser
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.Row

@Slf4j
@Transactional
class DataSetService extends DataDictionaryComponentService<DataModel> {

    @Autowired
    MessageSource messageSource

    @Override
    Map indexMap(DataModel catalogueItem) {
        [
            catalogueId: catalogueItem.id.toString(),
            name       : catalogueItem.label,
            stereotype : "dataSet"

        ]
    }

    @Override
    def show(String branch, String id) {
        DataModel dataModel = dataModelService.get(id)

        String description = convertLinksInDescription(branch, dataModel.description)
        String shortDesc = replaceLinksInShortDescription(getShortDescription(dataModel, null))

        DataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(branch)

        def result = [
            catalogueId     : dataModel.id.toString(),
            name            : dataModel.label,
            stereotype      : "dataSet",
            shortDescription: shortDesc,
            description     : description,
            alsoKnownAs     : getAliases(dataModel)
        ]

        StringBuffer stringBuffer = new StringBuffer()
        if (dataModel && dataModel.dataClasses && dataModel.dataClasses.size() > 0) {
            log.debug("Data Classes: " + dataModel.dataClasses.size())
            dataModel.dataClasses.sort {
                dataClass -> Integer.parseInt((DDHelperFunctions.getMetadataValue(dataClass, "Web Order") ?: "0"))
            }.each {dataClass ->
                if (dataModel.label.startsWith("CDS")) {
                    stringBuffer.append outputCDSClassAsDita(dataClass, dataDictionary, dataModel).toString()
                } else {
                    stringBuffer.append outputClassAsDita(dataClass, dataDictionary).toString()
                }
            }
            result["specifications"] = stringBuffer.toString()
        }
        return result
    }

    @Override
    Set<DataModel> getAll() {
        Folder folder = folderService.findByPath(DataDictionary.FOLDER_NAME.toString())
        Folder dataSetsFolder = folder.childFolders.find {it.label == DataDictionary.DATA_SETS_FOLDER_NAME}

        getAllDataSets(dataSetsFolder)
    }

    @Override
    DataModel getItem(UUID id) {
        dataModelService.get(id)
    }

    Set<DataModel> getAllDataSets(Folder dataSetsFolder) {
        if (dataSetsFolder.label != "Retired") {
            Set<DataModel> returnModels = []
            dataSetsFolder.childFolders.each {childFolder ->
                returnModels.addAll(getAllDataSets(childFolder))
            }
            returnModels.addAll(dataModelService.findAllByFolderId(dataSetsFolder.id))
            return returnModels
        } else return []
    }

    @Override
    String getMetadataNamespace() {
        DDHelperFunctions.metadataNamespace + ".data set"
    }

    @Override
    String getShortDescription(DataModel dataSet, DataDictionary dataDictionary) {
        if (isPreparatory(dataSet)) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {

            List<String> aliases = [dataSet.label]
            DataDictionaryComponent.aliasFields.keySet().each {aliasType ->
                String alias = DDHelperFunctions.getMetadataValue(dataSet, aliasType)
                if (alias) {
                    aliases.add(alias)
                }
            }


            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + DDHelperFunctions.parseHtml(dataSet.description.toString()) + "</xml>")
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
                return dataSet.label
            } catch (Exception e) {
                log.debug("Couldn't parse: " + dataSet.description)
                return dataSet.label
            }
        }
    }

    String outputClassAsDita(DataClass dataClass, DataDictionary dataDictionary) {

        StringBuffer result = new StringBuffer()
        if (DataSetParser.isChoice(dataClass) && dataClass.label.startsWith("Choice")) {
            result.append "<b>One of the following options must be used:</b>"

            getSortedChildElements(dataClass).eachWithIndex {childDataClass, idx ->
                if (idx != 0) {
                    result.append "<b>Or</b>"
                }
                outputClassAsDita((DataClass) childDataClass, dataDictionary)
            }


        } else {
            result.append "<table>"
            result.append "<tbody>"
            result.append "<tr class='thead-light'>"
            result.append "<td colspan='2'><b>${dataClass.label}</b>"
            if (dataClass.description) {
                result.append "<p>${dataClass.description}</p>"
            }
            result.append "</td>"
            result.append "</tr>"

            if (dataClass.dataElements && dataClass.dataElements.size() > 0) {
                result.append "<tr>"
                result.append "<td style='width:20%'>Mandation</td>"
                result.append "<td style='width:80%'>Data Elements</td>"
                result.append "</tr>"
            }

            result.append addAllChildRows(dataClass, dataDictionary)
            result.append "</tbody>"
            result.append "</table>"
            return result.toString()

        }
    }

    String addAllChildRows(DataClass parentDataClass, DataDictionary dataDictionary) {
        StringBuffer result = new StringBuffer()
        if (parentDataClass.dataElements) {
            getSortedChildElements(parentDataClass).each {child ->
                result.append addChildRow(child, dataDictionary)
            }

        } else {
            // no dataElements
            getSortedChildElements(parentDataClass).eachWithIndex {childDataClass, idx ->
                if (DataSetParser.isChoice(parentDataClass) && idx != 0) {
                    result.append "<tr><td colspan='2'><b>Or</b></td></tr>"
                }
                result.append addSubClass((DataClass) childDataClass, dataDictionary)
            }
        }
        return result
    }

    String addChildRow(CatalogueItem child, DataDictionary dataDictionary) {
        return "<tr><td>${DataSetParser.getMRO(child)}</td><td>${createLinkFromItem(child, dataDictionary)}</td></tr>"
    }

    String createLinkFromItem(CatalogueItem catalogueItem, DataDictionary dataDictionary) {
        if (catalogueItem instanceof DataElement) {
            return createLinkFromDataElement((DataElement) catalogueItem, dataDictionary)
        } else if (catalogueItem instanceof DataClass) {
            String xRefs = ""
            if (DataSetParser.isAddress(catalogueItem)) {
                DataElement childElement = (DataElement) getSortedChildElements(catalogueItem)[0]
                String link = createLinkFromItem(childElement, dataDictionary)
                xRefs += link.replace("</p>", " - <a class='class' href=''>ADDRESS STRUCTURED</a></p>")
                xRefs += "<p>Or</p>"
                xRefs += link.replace("</p>", " - <a class='class' href=''>ADDRESS UNSTRUCTURED</a></p>")
            } else if (DataSetParser.isChoice(catalogueItem) && catalogueItem.label.startsWith("Choice")) {
                getSortedChildElements(catalogueItem).eachWithIndex {childItem, idx ->
                    if (idx != 0) {
                        xRefs += "<p>Or</p>"
                    }
                    xRefs += createLinkFromItem(childItem, dataDictionary)
                }
            } else if (DataSetParser.isAnd(catalogueItem) && (catalogueItem.label.startsWith("Choice") || catalogueItem.label.startsWith("And"))) {
                getSortedChildElements(catalogueItem).eachWithIndex {childItem, idx ->
                    if (idx != 0) {
                        xRefs += "<p>And</p>"
                    }
                    xRefs += createLinkFromItem(childItem, dataDictionary)
                }
            } else if (DataSetParser.isInclusiveOr(catalogueItem) && catalogueItem.label.startsWith("Choice")) {
                getSortedChildElements(catalogueItem).eachWithIndex {childItem, idx ->
                    if (idx != 0) {
                        xRefs += "<p>And/Or</p>"
                    }
                    xRefs += createLinkFromItem(childItem, dataDictionary)
                }
            }
            return xRefs
        }
    }

    String addSubClass(DataClass dataClass, DataDictionary dataDictionary) {

        StringBuffer result = new StringBuffer()

        if (dataClass.label != "Choice") {
            result.append "<tr class='table-primary'><td><b>Mandation</b></td>"
            result.append "<td><b>${dataClass.label}</b>"
            if (dataClass.description) {
                result.append "<p>${dataClass.description}</p>"
            }
            result.append "</td></tr>"

        }
        result.append addAllChildRows(dataClass, dataDictionary)
        return result
    }

    String createLinkFromDataElement(DataElement dataElement, DataDictionary dataDictionary) {
        String uin = DDHelperFunctions.getMetadataValue(dataElement, "uin")
        DDElement ddElement = dataDictionary.elements[uin]
        String retValue
        if (ddElement) {
            retValue = "<p><a class='element' href='#/preview/element/${ddElement.catalogueItem.id.toString()}'></a>${ddElement.name}</p>"
        } else {
            log.debug("Cannot find uin: ${dataElement.label}, ${uin}")
            retValue = "<p>${dataElement.label}</p>"
        }
        if (dataElement.maxMultiplicity == -1) {
            retValue += "<p>Multiple occurrences of this item are permitted</p>"
        }
        return retValue
    }

    List<CatalogueItem> getSortedChildElements(DataClass dataClass) {
        List<CatalogueItem> children = []
        if (dataClass.dataElements) {
            children.addAll(dataClass.dataElements)
        }
        if (dataClass.dataClasses) {
            children.addAll(dataClass.dataClasses)
        }

        return children.sort {
            child -> Integer.parseInt(DDHelperFunctions.getMetadataValue(child, "Web Order") ?: "0")
        }

    }

    String outputCDSClassAsDita(DataClass dataClass, DataDictionary dataDictionary, DataModel dataModel) {

        int totalDepth = calculateClassDepth(dataClass)
        StringBuffer result = new StringBuffer()
        if (DataSetParser.isChoice(dataClass) && dataClass.label.startsWith("Choice")) {
            result.append "<p> <b>One of the following options must be used:</b></p>"
            dataClass.dataClasses
                .sort {childDataClass -> Integer.parseInt(DDHelperFunctions.getMetadataValue(childDataClass, "Web Order") ?: "0")}
                .eachWithIndex {childDataClass, idx ->
                    result.append outputCDSClassAsDita(childDataClass, dataDictionary, dataModel)
                    if (idx < dataClass.dataClasses.size() - 1) {
                        result.append "<p> <b>Or</b></p>"
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
                log.debug("Cannot link dataset: ${DataSetParser.getDataSetReferenceTo(dataClass)}")
            }
            result.append "<table>"
            result.append addTopLevelCDSHeader(dataClass, linkedDataSet.name, dataClass.description, totalDepth, false)

            result.append "<tr>"
            result.append "<td style='text-align: center'>${DataSetParser.getMRO(dataClass)}</td>"
            result.append "<td style='text-align: center'>${DataSetParser.getGroupRepeats(dataClass)}</td>"
            result.append "<td colspan='${totalDepth * 2 + 3}'><p> <b>Data Group:</b><p>" +
                          "<a href='#/preview/dataSet/${linkedDataSet.catalogueItem.id}' class='dataSet'>${linkedDataSet.name}</a></p></td>"
            result.append "</table>"
        } else {
            String label = dataClass.label
            if (label.endsWith(" 1")) {
                label = label.replaceAll(" 1", "")
            }
            result.append "<table>"
            result.append addTopLevelCDSHeader(dataClass, label, dataClass.description, totalDepth)
            result.append "<tbody>"
            result.append addCDSClassContents(dataClass, dataModel, dataDictionary, totalDepth, 0)
            result.append "</tbody>"
            result.append "</table>"

        }
    }

    int calculateClassDepth(DataClass dataClass) {
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

    int calculateClassRows(DataClass dataClass) {

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

    String addTopLevelCDSHeader(DataClass dataClass, String groupName, String function, int totalDepth, boolean includeMRO = true) {
        StringBuffer result = new StringBuffer()
        result.append "<thead>"
        result.append "<tr>"
        result.append "<td colspan='2'>Notation</td>"
        result.append "<td colspan='${totalDepth * 2 + 2}'><p> <b>Data Group:</b> ${groupName}</p></td>"
        result.append "</tr>"
        result.append "<tr class='thead-light'>"
        String groupStatusString = "<p>Group Status</p>"
        if (includeMRO) {
            groupStatusString += "<p>${DataSetParser.getMRO(dataClass) ?: ""}</p>"
        }
        String groupRepeatsString = "<p>Group Repeats</p>"
        if (includeMRO) {
            groupRepeatsString += "<p>${DataSetParser.getGroupRepeats(dataClass) ?: ""}</p>"
        }
        result.append "<td colspan='${totalDepth * 2}'>${groupStatusString}</td>"
        result.append "<td colspan='${totalDepth * 2}'>${groupRepeatsString}</td>"
        result.append "<td colspan='${totalDepth * 2 + 1}'><p> <b>Function:</b> ${function}</p></td>"
        result.append "</tr>"
        result.append "</thead>"
        return result
    }

    String addCDSClassContents(DataClass dataClass, DataModel dataModel, DataDictionary dataDictionary, int totalDepth, int currentDepth) {

        StringBuffer result = new StringBuffer()

        if (dataClass.dataElements) {
            getSortedChildElements(dataClass).each {child ->
                result.append addCDSChildRow(child, dataDictionary, totalDepth, currentDepth)
            }
        } else {
            // no dataElements
            if (dataClass.dataClasses) {
                if (DataSetParser.isChoice(dataClass)) {
                    result.append "<tr><td><p> <b>One of the following DATA GROUPS must be used:</b></p></td></tr>"
                }
                List<DataClass> sortedClasses = getSortedChildElements(dataClass)
                sortedClasses.each {childDataClass ->
                    result.append addCDSSubClass(childDataClass, dataModel, dataDictionary, totalDepth, currentDepth + 1)
                    if (DataSetParser.isChoice(dataClass) &&
                        childDataClass != sortedClasses.last()) {
                        result.append "<tr><td><p> <b>Or</b></p></td></tr>"
                    }
                }
            } else {
                log.debug("No data elements or data classes... ")
                log.debug(dataModel.label)
                log.debug(dataClass.label)
            }
        }
        return result.toString()
    }

    String addCDSSubClass(DataClass dataClass, DataModel dataModel, DataDictionary dataDictionary, int totalDepth,
                          int currentDepth) {
        StringBuffer result = new StringBuffer()
        result.append addCDSClassHeader(dataClass, dataModel, dataDictionary, totalDepth, currentDepth)
        result.append addCDSClassContents(dataClass, dataModel, dataDictionary, totalDepth, currentDepth)
        return result.toString()
    }


    String addCDSClassHeader(DataClass dataClass, DataModel dataModel, DataDictionary dataDictionary, int totalDepth,
                             int currentDepth) {
        int moreRows = calculateClassRows(dataClass)
        StringBuffer result = new StringBuffer()
        String name = dataClass.label
        if (name.endsWith(" 1")) {
            name = name.replace(" 1", "")
        }
        String nameEntry = "<b>${name}</b>"
        if (dataClass.description) {
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
            nameEntry += "<p>${description}</p>"
        }
        result.append "<tr class='thead-light table-primary'>"
        result.append "<td rowspan='${moreRows + 1}'>${DataSetParser.getMRO(dataClass)}</td>"
        result.append "<td rowspan='${moreRows + 1}'>${DataSetParser.getGroupRepeats(dataClass)}</td>"
        result.append "<td colspan='3'><p>${nameEntry}</p></td>"
        result.append "<td>Rules</td>"

        return result
    }

    String addCDSChildRow(CatalogueItem child, DataDictionary dataDictionary, int totalDepth, int currentDepth) {
        StringBuffer result = new StringBuffer()
        if (child instanceof DataElement) {
            String uin = DDHelperFunctions.getMetadataValue(child, "uin")
            if (uin) {
                DDElement ddElement = dataDictionary.elements[uin]
                Row elementRow = new Row(entries: [])
                if (ddElement) {
                    result.append "<tr>"
                    result.append "<td style='text-align: center'>${DataSetParser.getMRO(child)}</td>"
                    result.append "<td style='text-align: center'>${DataSetParser.getGroupRepeats(child)}</td>"
                    result.append "<td colspan='${(totalDepth - currentDepth) * 2 + 1}'>" +
                                  "<a href='#/preview/elements/${ddElement.catalogueItem.id.toString()}'>${ddElement.name}</a></td>"
                    result.append "<td style='text-align: center'>${DataSetParser.getRules(child).replaceAll("\\n", "<br/>")}</td>"
                    result.append "</tr>"

                } else {
                    result.append "<tr>"
                    result.append "<td style='text-align: center'>${DataSetParser.getMRO(child)}</td>"
                    result.append "<td style='text-align: center'>${DataSetParser.getGroupRepeats(child)}</td>"
                    result.append "<td colspan='${(totalDepth - currentDepth) * 2 + 1}'>${child.label}</td>"
                    result.append "<td style='text-align: center'>${DataSetParser.getRules(child).replaceAll("\\n", "<br/>")}</td>"
                    result.append "</tr>"
                }
            } else {
                log.debug("Cannot find uin: " + child.label)
            }
        } else if (child instanceof DataClass) {
            if (DataSetParser.isChoice(child) && child.label.startsWith("Choice")) {
                Row elementRow = new Row(entries: [])
                String mro = ""
                String groupRepeats = ""
                String xRefs = ""
                String rules = ""
                ((DataClass) child).dataElements
                    .sort {childDataElement -> Integer.parseInt(DDHelperFunctions.getMetadataValue(childDataElement, "Web Order"))}
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
                        rules += "<p>${DataSetParser.getRules(childDataElement)?.replaceAll("\\n", "<br/>") ?: ""}</p>"

                        if (ddElement) {
                            xRefs += "<p><a href='#/preview/elements/${ddElement.catalogueItem.id.toString()}' " +
                                     "class='element'>${ddElement.name}</a></p>"
                        } else {
                            xRefs += "<p>${childDataElement.label}</p>"
                            log.debug("no ddElement! ${uin} ${childDataElement.label}")
                        }
                    }
                result.append "<tr>"
                result.append "<td style='text-align: center'>${mro}</td>"
                result.append "<td style='text-align: center'>${groupRepeats}</td>"
                result.append "<td colspan='${(totalDepth - currentDepth) * 2 + 1}'>${xRefs}</td>"
                result.append "<td style='text-align: center'>${rules}</td>"
                result.append "</tr>"

            }
        }
        return result.toString()
    }


    void ingestFromXml(def xml, Folder dictionaryFolder, DataModel coreDataModel, String currentUserEmailAddress,
                       NhsDataDictionary nhsDataDictionary) {

        Folder dataSetsFolder = new Folder(label: "Data Sets", createdBy: currentUserEmailAddress)
        dictionaryFolder.addToChildFolders(dataSetsFolder)
        if (!folderService.validate(dataSetsFolder)) {
            throw new ApiInvalidModelException('NHSDD', 'Invalid model', dataSetsFolder.errors)
        }
        folderService.save(dataSetsFolder)

        xml.DDDataSet.each {ddDataSetXml ->
            createAndSaveDataModel(ddDataSetXml, dataSetsFolder, dictionaryFolder, currentUserEmailAddress, nhsDataDictionary)
        }
    }

    void createAndSaveDataModel(def ddDataSetXml, Folder dataSetsFolder, Folder dictionaryFolder, String currentUserEmailAddress, NhsDataDictionary nhsDataDictionary) {
        String dataSetName = ddDataSetXml.TitleCaseName.text()
        boolean isRetired = ddDataSetXml.isRetired.text() == "true"
        List<String> path = getPath(ddDataSetXml."base-uri".text(), isRetired)
        Folder folder = getFolderAtPath(dataSetsFolder, path, currentUserEmailAddress)
        log.debug('Ingesting {}', dataSetName)

        DataModel dataSetDataModel = new DataModel(
            label: dataSetName,
            description: DDHelperFunctions.parseHtml(ddDataSetXml.definition),
            createdBy: currentUserEmailAddress,
            type: DataModelType.DATA_STANDARD,
            authority: authorityService.defaultAuthority,
            folder: folder
        )

        if (dataSetName.startsWith("CDS")) {
            CDSDataSetParser.parseCDSDataSet((GPathResult) ddDataSetXml.definition, dataSetDataModel, nhsDataDictionary)
        } else {
            DataSetParser.parseDataSet((GPathResult) ddDataSetXml.definition, dataSetDataModel, nhsDataDictionary)
        }

        addMetadataFromXml(dataSetDataModel, ddDataSetXml, currentUserEmailAddress)

        // Fix the created by field and any other associations
        dataModelService.checkImportedDataModelAssociations(nhsDataDictionary.currentUser, dataSetDataModel)

        log.info("Validating Data model: ${dataSetDataModel.label}")
        DataModel validated = dataModelService.validate(dataSetDataModel)
        if (validated.hasErrors()) {
            GormUtils.outputDomainErrors(messageSource, validated)
            //        TODO throw an exception instead???    throw new ApiInvalidModelException('NHSDD', 'Invalid model', validated.errors)
        } else {
            log.info("Saving Data model: ${validated.label}")
            dataModelService.saveModelWithContent(validated)
            log.info("Saved Data model: ${validated.label}")
        }
    }

    void addDataClassToDataModel(DataClass dataClass, DataModel dataModel) {
        dataModel.addToDataClasses(dataClass)
        dataClass.dataClasses.each {childDataClass -> addDataClassToDataModel(childDataClass, dataModel)}
    }

    List<String> getPath(String pathStr, boolean isRetired) {
        List<String> path
        try {
            path = DDHelperFunctions.getPath(pathStr, "Messages", ".txaClass20")
        } catch (Exception e) {
            path = DDHelperFunctions.getPath(pathStr, "Web_Site_Content", ".txaClass20")
        }
        path.removeAll {it.equalsIgnoreCase("Data_Sets")}
        path.removeAll {it.equalsIgnoreCase("Content")}

        if (isRetired) {
            path.add(0, "Retired")
        }
        path = path.collect {DDHelperFunctions.tidyLabel(it)}
        return path
    }

    Folder getFolderAtPath(Folder sourceFolder, List<String> path, String createdByEmail) {
        if (path.size() == 1) {
            return sourceFolder
        } else {
            String nextFolderName = path.remove(0)
            Folder nextFolder = sourceFolder.childFolders.find {it.label == nextFolderName}
            if (!nextFolder) {
                nextFolder = new Folder(label: nextFolderName, createdBy: createdByEmail)
                sourceFolder.addToChildFolders(nextFolder)
                if (!folderService.validate(nextFolder)) {
                    throw new ApiInvalidModelException('NHSDD', 'Invalid model', nextFolder.errors)
                }
                folderService.save(nextFolder)
            }
            return getFolderAtPath(nextFolder, path, createdByEmail)

        }

    }


}
