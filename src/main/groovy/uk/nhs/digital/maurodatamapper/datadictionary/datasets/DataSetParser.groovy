package uk.nhs.digital.maurodatamapper.datadictionary.datasets

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import groovy.xml.XmlUtil
import uk.nhs.digital.maurodatamapper.datadictionary.DDElement
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

@Slf4j
class DataSetParser {

    static XmlParser xmlParser = XmlParser.newInstance()

    static final String xmlFileName = "valuesets-expanded-september.xml"

    static Map<String, DataModel> dataModels = [:]


    static void verifyDataModel(String name, String hash, Boolean print = false) {
        String generatedHash = generateDataSetHash(dataModels[name])
        if (generatedHash != hash || hash == "") {
            printDataModel(dataModels[name])
            log.error(generateDataSetHash(dataModels[name]))
        }
        assert (generatedHash == hash)
        assert (hash != "")
    }

    static void parseDataSet(GPathResult definition, DataModel dataModel, DataDictionary dataDictionary) {
        List<DataClass> dataClasses = []
        if (definition.children().count { it.name() == "table" && DDHelperFunctions.tableIsClassHeader(it) }) {
            dataClasses = OtherDataSetParser.parseDataSetWithHeaderTables(definition, dataModel, dataDictionary)
        } else {
            definition.children().findAll { it.name() == "table" }.each { GPathResult table ->
                dataClasses.addAll(OtherDataSetParser.parseDataClassTable(table, dataModel, dataDictionary, 1))
            }
        }
        if (dataClasses.size() == 1 && !dataClasses[0].label) {
            dataClasses[0].label = "Data Set Data Elements"
        }
        dataClasses.eachWithIndex { dataClass, index ->
            dataModel.addToDataClasses(dataClass)
            setOrder(dataClass, index + 1)
        }
        fixPotentialDuplicates(dataModel)
    }

    static boolean isSkyBlue(GPathResult gPathResult) {
        return gPathResult.@class.text() == "skyblue"
    }


    static void printDataModel(DataModel dataModel) {
        log.error("===============================")
        log.error(dataModel.label)
        dataModel.dataClasses.sort { getOrder(it) }.each { dataClass ->
            printDataClass(dataClass, 3)
        }
    }

    static void printDataClass(DataClass dataClass, int indent) {
        String indentString = ""
        for (int i = 0; i < indent; i++) {
            indentString += " "
        }
        String choiceString = ""
        if (isChoice(dataClass)) {
            choiceString = " (choice)"
        } else if (isAnd(dataClass)) {
            choiceString = " (and)"
        } else if (isNotOption(dataClass)) {
            choiceString = " (not option)"
        }
        String webOrder = DataSetParser.getOrder(dataClass)
        log.error("${indentString}${dataClass.label}${choiceString} (${webOrder})")
        List<CatalogueItem> components = []
        if (dataClass.dataElements) {
            components.addAll(dataClass.dataElements)
        }
        if (dataClass.dataClasses) {
            components.addAll(dataClass.dataClasses)
        }
        indentString += "   "
        components.sort { getOrder(it) }.each { component ->
            if (component instanceof DataClass) {
                printDataClass(component, indent + 3)
            } else if (component instanceof DataElement) {
                log.error("${indentString}${component.label}")
            }
        }
    }

    static String generateDataSetHash(DataModel dataModel) {
        String ret = ""
        if (dataModel.dataClasses) {
            ret += dataModel.dataClasses.size() + "C"
            dataModel.dataClasses.sort { getOrder(it) }.each { dataClass ->
                ret += generateDataClassHash(dataClass)
            }
        }
        return ret

    }

    static String generateDataClassHash(DataClass dataClass) {

        String ret = ""
        if (dataClass.dataClasses) {
            ret += dataClass.dataClasses.size() + "C"
            dataClass.dataClasses.sort { getOrder(it) }.each { childDataClass ->
                ret += generateDataClassHash(childDataClass)
            }
        }
        if (dataClass.dataElements) {
            ret += "" + dataClass.dataElements.size() + "E"
        }
        return ret
    }

    static Integer getOrder(MetadataAware item) {
        Integer.parseInt(item.getMetadata().find { it.key == "Web Order" }.value)
    }

    static void setOrder(MetadataAware item, Integer order) {
        item.addToMetadata(namespace: DDHelperFunctions.metadataNamespace, key: "Web Order", value: "" + order)
    }

    static void setChoice(MetadataAware item) {
        item.addToMetadata(namespace: DDHelperFunctions.metadataNamespace, key: "Choice", value: "true")
    }

    static void setAnd(MetadataAware item) {
        item.addToMetadata(namespace: DDHelperFunctions.metadataNamespace, key: "And", value: "true")
    }

    static void setAddress(MetadataAware item) {
        item.addToMetadata(namespace: DDHelperFunctions.metadataNamespace, key: "Address Choice", value: "true")
    }

    static void setNameChoice(MetadataAware item) {
        item.addToMetadata(namespace: DDHelperFunctions.metadataNamespace, key: "Name Choice", value: "true")
    }

    static void setInclusiveOr(MetadataAware item) {
        item.addToMetadata(namespace: DDHelperFunctions.metadataNamespace, key: "Inclusive", value: "true")
    }

    static void setDataSetReference(MetadataAware item) {
        item.addToMetadata(namespace: DDHelperFunctions.metadataNamespace, key: "Data Set Reference", value: "true")
    }

    static boolean isDataSetReference(MetadataAware item) {
        item.metadata.find {
            (it.namespace == DDHelperFunctions.metadataNamespace
                    && it.key == "Data Set Reference"
                    && it.value == "true")
        }
    }

    static void setDataSetReferenceTo(MetadataAware item, String dataSetName) {
        item.addToMetadata(namespace: DDHelperFunctions.metadataNamespace, key: "Data Set Reference To", value: dataSetName)
    }



    static String getDataSetReferenceTo(MetadataAware item) {
        item.getMetadata().find { it.key == "Data Set Reference To" }.value
    }

    static void setMultiplicityText(MetadataAware item, String multiplicity) {
        item.addToMetadata(namespace: DDHelperFunctions.metadataNamespace, key: "Multiplicity Text", value: multiplicity)
    }
    static String getMultiplicityText(MetadataAware item) {
        item.getMetadata().find { it.key == "Multiplicity Text" }.value
    }

    static void setMRO(MetadataAware item, String mro) {
        item.addToMetadata(namespace: DDHelperFunctions.metadataNamespace, key: "MRO", value: mro)
    }

    static String getMRO(MetadataAware item) {
        item.getMetadata().find { it.key == "MRO" }?.value
    }

    static void setRules(MetadataAware item, String rules) {
        item.addToMetadata(namespace: DDHelperFunctions.metadataNamespace, key: "Rules", value: rules)
    }

    static String getRules(MetadataAware item) {
        item.getMetadata().find { it.key == "Rules" }?.value
    }

    static void setGroupRepeats(MetadataAware item, String groupRepeats) {
        item.addToMetadata(namespace: DDHelperFunctions.metadataNamespace, key: "Group Repeats", value: groupRepeats)
    }

    static String getGroupRepeats(MetadataAware item) {
        item.getMetadata().find { it.key == "Group Repeats" }?.value
    }


    static void setNotOption(MetadataAware item) {
        item.addToMetadata(namespace: DDHelperFunctions.metadataNamespace, key: "NotOption", value: "true")
    }

    static boolean isChoice(MetadataAware item) {
        item.metadata.find {
            (it.namespace == DDHelperFunctions.metadataNamespace
                    && it.key == "Choice"
                    && it.value == "true")
        }
    }

    static boolean isAnd(MetadataAware item) {
        item.metadata.find {
            (it.namespace == DDHelperFunctions.metadataNamespace
                    && it.key == "And"
                    && it.value == "true")
        }
    }
    static boolean isInclusiveOr(MetadataAware item) {
        item.metadata.find {
            (it.namespace == DDHelperFunctions.metadataNamespace
                    && it.key == "Inclusive"
                    && it.value == "true")
        }
    }
    static boolean isAddress(MetadataAware item) {
        item.metadata.find {
            (it.namespace == DDHelperFunctions.metadataNamespace
                    && it.key == "Address Choice"
                    && it.value == "true")
        }
    }

    static boolean isNotOption(MetadataAware item) {
        item.metadata.find {
            (it.namespace == DDHelperFunctions.metadataNamespace
                    && it.key == "NotOption"
                    && it.value == "true")
        }
    }


    static List<List<GPathResult>> partitionByDuckBlueClasses(GPathResult dataSetDefinition) {

        List<List<GPathResult>> topLevelClasses = []
        List<GPathResult> list = []
        dataSetDefinition.children().each { childNode ->
            if (DDHelperFunctions.tableIsClassHeader(childNode)) {
                if (list.size() > 0) {
                    topLevelClasses.add(list)
                }
                list = []
                list.add(childNode)
            } else {
                list.add(childNode)
            }
        }
        topLevelClasses.add(list)
        topLevelClasses
    }

    static void setNameAndDescriptionFromCell(DataClass dataClass, GPathResult cell) {
        String cellContent = cell.text()
        // System.err.println(cellContent)
        int colonIndex = cellContent.indexOf(":")

        if (colonIndex > 0 && !cellContent.startsWith("CHOICE")) {
            dataClass.label = cellContent.subSequence(0, colonIndex)
            dataClass.description = cellContent.substring(colonIndex).replaceFirst(":", "")
        } else {
            int hyphenIndex = cellContent.indexOf(" - ")
            int carryIndex = cellContent.indexOf("To carry details")
            if (hyphenIndex > 0 && !cellContent.startsWith("CHOICE")) {
                dataClass.label = cellContent.subSequence(0, hyphenIndex)
                dataClass.description = cellContent.substring(hyphenIndex).replaceFirst("-", "")
            } else if (carryIndex > 0 && !cellContent.startsWith("CHOICE")) {
                dataClass.label = cellContent.subSequence(0, carryIndex)
                dataClass.description = cellContent.substring(carryIndex)
            } else {
                if ((!dataClass.label || dataClass.label == "") && cellContent.startsWith("To carry the ")
                        && cellContent.indexOf("details.") > 0) {
                    // to deal with the GUMCAD data model...
                    int detailsLocation = cellContent.indexOf("details.")
                    dataClass.label = cellContent.subSequence(13, detailsLocation)
                    dataClass.description = cellContent
                } else if ((!dataClass.label || dataClass.label == "") && cellContent.contains(".")) {
                    int detailsLocation = cellContent.indexOf(".")
                    dataClass.label = cellContent.subSequence(0, detailsLocation)
                    dataClass.description = cellContent
                } else {
                    if (dataClass.label && dataClass.label != "" && (!dataClass.description || dataClass.description == "")) {
                        dataClass.description = cellContent
                    } else {
                        dataClass.label = cellContent
                    }
                }
            }
        }

    }

    static DataElement getElementFromText(Object anchor, DataModel dataModel, DataDictionary dataDictionary, String currentClass) {
        //String elementLabel = ((String)anchor.@href).replaceAll(" ", "%20")
        String elementUrl = ((String) anchor.@href)

        if (fileNameFixes[elementUrl]) {
            elementUrl = fileNameFixes[elementUrl]
        }


        DDElement de = null
        if (dataDictionary) {
            de = dataDictionary.elements.values().find { it.ddUrl == elementUrl }
        }
        if (!de) {
            log.error("Cannot find element: ${anchor.text()} - ${elementUrl}")
            log.error("In class: ${currentClass}, model: ${dataModel.label}")
            DataType defaultDataType = dataModel.dataTypes.find {it.label == "Unmatched datatype" }
            if (!defaultDataType) {
                defaultDataType = new PrimitiveType(label: "Unmatched datatype")
                //dataModel.addToDataTypes(defaultDataType)
            }
            DataElement dataElement = new DataElement(label: anchor.text(), dataType: defaultDataType)
            return dataElement

        } else {
            return de.toCatalogueItem(dataDictionary)
        }
    }

    static final Map<String, String> fileNameFixes = [
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/pe/percentage_of_nhs_continuing_healthcare_referrals_concluded_within_28_days_(standard)_de.asp":
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/pe/percentage_of_nhs_continuing_healthcare_referrals_conluded_within_28_days_(standard)_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/c/care/care_professional_local_identifier_de.asp"                                                 :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/c/care/care_professional_identifier_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/c/care/care_professional_team_local_identifier_de.asp"                                            :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/c/care/care_professional_team_identifier_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/proc/procedure_date_(antenatal%20treatment)_de.asp"                                             :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/proc/procedure_date_(antenatal_treatment)_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/h/hu/human_papillomavirus_vaccination_dose_de.asp"                                                :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/h/hu/human_papillomavirus_vaccination_dose_given_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/r/rep/residual_disease_size_(gynaecological_cancer)_de.asp"                                       :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/r/rep/residual_disease_size_(gynaecology_cancer)_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/pri/primary_induction_chemotherapy_failure_indicator_de.asp"                                    :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/pri/primary_induction_failure_indicator_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/f/fo/french_american_british_classification_(acute_myeloid_leukaemia)_de.asp"                     :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/f/fo/french_american_british_classification_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/t/th/tissue_type_banked_at_diagnosis_de.asp"                                                      :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/t/th/tissue_banked_at_diagnosis_type_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/c/care/care_professional_operating_surgeon_type_(cancer)_de.asp"                                  :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/c/care/care_professional_surgeon_grade_(cancer)_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/proc/procedure_date_(transthoracic_echocardiogram_test)_de.asp"                                 :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/proc/procedure_date_(transthoracic_echocardiograml)_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/o/org/organisation_identifier_(local_patient_identifier_(baby))_de.asp"                           :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/o/org/organisation_identifier_(local_patient_identifier_baby)_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/o/org/organisation_identifier_(local_patient_identifier_(mother))_de.asp"                         :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/o/org/organisation_identifier_(local_patient_identifier_mother)_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/d/dea/decision_support_tool_completed_date_(nhs_continuing healthcare_standard)_de.asp"           :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/d/dea/decision_support_tool_completed_date_(nhs_continuing%20healthcare_standard)_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/g/gl/gleason_grade_(primary)_de.asp"                                                              :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/g/gl/gleason_score_(primary)_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/g/gl/gleason_grade_(secondary)_de.asp"                                                            :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/g/gl/gleason_score_(secondary)_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/g/gl/gleason_grade_(tertiary)_de.asp"                                                             :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/g/gl/gleason_score_(tertiary)_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/pe/peritoneal_involvement_indicator_(endometrial_cancer)_de.asp"                                :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/p/pe/peritoneal_involvement_indicator_(endomertrial_cancer)_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/g/gene_or_stratification_biomarker_authorised_date_de.asp"                                        :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/g/gene_or_stratification_biomarker_reported_date_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/s/st/stage_date_(cancer_site_specific_stage)_de.asp"                                              :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/s/st/stage_grouping_date_(cancer_site_specific_stage)_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/o/org/organisation_site_identifier_(of_acute_oncology_assessment)_de.asp"                         :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/o/org/organisation_site_identifier_(of_clinical_assessment)1_de.asp",
            "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/a/act/acute_oncology_assessment_patient_presentation_type_de.asp"                                 :
                    "https://v3.datadictionary.nhs.uk/data_dictionary/data_field_notes/a/act/acute_oncology_assessment_patient_presentattion_type_de.asp"
    ]

    static void fixPotentialDuplicates(DataModel dataModel) {
        List<String> classNames = []
        dataModel.dataClasses.each { childDataClass ->
            if (classNames.contains(childDataClass.label)) {
                log.error("Duplicate class label: ${childDataClass.label} in ${dataModel.label}")
                int idx = 1
                while(classNames.contains(childDataClass.label + " " + idx)) {
                    idx++
                }
                childDataClass.label = childDataClass.label + " " + idx
            }
            classNames.add(childDataClass.label)
            fixPotentialDuplicates(childDataClass, dataModel.label)
        }
    }

    static void fixPotentialDuplicates(DataClass dataClass, String modelName) {
        if (dataClass.dataClasses) {
            List<String> classNames = []
            dataClass.dataClasses.each { childDataClass ->
                if (classNames.contains(childDataClass.label)) {
                    log.error("Duplicate class label: ${childDataClass.label} in ${dataClass.label}  in ${modelName}")
                    int idx = 1
                    while(classNames.contains(childDataClass.label + " " + idx)) {
                        idx++
                    }
                    childDataClass.label = childDataClass.label + " " + idx
                }
                classNames.add(childDataClass.label)
                fixPotentialDuplicates(childDataClass, modelName)
            }
        }
        if (dataClass.dataElements) {
            List<String> elementNames = []
            dataClass.dataElements.each { childDataElement ->
                if (elementNames.contains(childDataElement.label)) {
                    log.error("Duplicate element label: ${childDataElement.label} in ${dataClass.label} in ${modelName}")
                    int idx = 1
                    while(elementNames.contains(childDataElement.label + " " + idx)) {
                        idx++
                    }
                    childDataElement.label = childDataElement.label + " " + idx

                }
                elementNames.add(childDataElement.label)
            }
        }
    }


    static void getAndSetMRO(GPathResult gPathResult, List<DataElement> elementList, DataClass choiceClass = null) {
        Node node = xmlParser.parseText(XmlUtil.serialize(gPathResult).replaceFirst("<\\?xml version=\"1.0\".*\\?>", ""))
        List<List<String>> params = getElementParameters(node)

        if(params.size() == 1 && elementList.size() > 1) {
            setMRO(choiceClass, paramsToHtml(params[0]))
        }
        else if(params.size() == elementList.size()) {
            elementList.eachWithIndex {element, idx ->
                setMRO(element, paramsToHtml(params[idx]))
            }
        } else {
            log.error("Params error! ")
            log.error(elementList.toString())
            elementList.each { DataElement entry ->
                log.error(entry.label)
            }
            log.error(gPathResult.toString())
        }

    }

    static void getAndSetRepeats(GPathResult gPathResult, List<DataElement> elementList, DataClass choiceClass = null) {
        Node node = xmlParser.parseText(XmlUtil.serialize(gPathResult).replaceFirst("<\\?xml version=\"1.0\".*\\?>", ""))
        List<List<String>> params = getElementParameters(node)

        if(params.size() == 1 && elementList.size() > 1) {
            setGroupRepeats(choiceClass, paramsToHtml(params[0]))
        }
        else if(params.size() == elementList.size()) {
            elementList.eachWithIndex {element, idx ->
                setGroupRepeats(element, paramsToHtml(params[idx]))
            }
        } else {
            log.error("Params error! ")
            log.error(elementList.toString())
            elementList.each { DataElement entry ->
                log.error(entry.label)
            }
            log.error(gPathResult.toString())
        }

    }


    static void getAndSetRules(GPathResult gPathResult, List<DataElement> elementList, DataClass choiceClass = null) {
        Node node = xmlParser.parseText(XmlUtil.serialize(gPathResult).replaceFirst("<\\?xml version=\"1.0\".*\\?>", ""))
        List<List<String>> params = getElementParameters(node)

        if(choiceClass && params.size() == 1 && elementList.size() > 1) {
            setRules(choiceClass, paramsToHtml(params[0]))
        }
        else if(params.size() == elementList.size()) {
            elementList.eachWithIndex {element, idx ->
                setRules(element, paramsToHtml(params[idx]))
            }
        } else {
            log.error("Params error! ")
            log.error(elementList.toString())
            elementList.each { DataElement entry ->
                log.error(entry.label)
            }
            log.error(gPathResult.toString())
        }

    }


    static String paramsToHtml(List<String> params) {
        String ret = ""
        params.each {ret += "<p>${it}</p>"}
        return ret
    }


    static List<List<String>> getElementParameters(Node td) {
        td.depthFirst().findAll{ n ->
            n instanceof Node && (n.name() == "strong" || n.name() == "em" || n.name() == "p")
        }.each {
            n -> Html.removeNodeKeepChildren(n)
        }


        // System.err.println("getElementParameters")
        List<List<String>> allValues = []
        List<String> currentValues = []
        for(int i=0;i<td.children().size(); i++) {
            if(!(td.children()[i] instanceof Node) && td.children()[i].equalsIgnoreCase("or")) {
                allValues.add(currentValues)
                currentValues = []
            } else if(!(td.children()[i] instanceof Node) &&
                    td.children()[i] != "\u00a0" &&
                    td.children()[i] != " ") {
                // System.err.println("Adding value: '${td.children()[i]}'")
                currentValues.add(td.children()[i])
            } else if(isBr(td.children()[i]) && td.children()[i+1] && isBr(td.children()[i+1]) ) {
                allValues.add(currentValues)
                currentValues = []
            } else if(isBr(td.children()[i]) &&
                    td.children()[i+1] && !(td.children()[i+1] instanceof Node) &&
                    (td.children()[i+1] == " " || td.children()[i+1] == "\u00a0") &&
                    td.children()[i+2] && isBr(td.children()[i+2]) ) {
                // System.err.println("Here")
                allValues.add(currentValues)
                currentValues = []
                i+=2
            } else if(isBr(td.children()[i])) {
                // skip
            } else if(td.children()[i] instanceof Node && td.children()[i].name() == "strong") {
                // This should be a String...
                currentValues.add(td.children()[i].children()[0])
            } else {
                log.error("Unknown node!!")
                log.error(td.children()[i])
            }
        }
        allValues.add(currentValues)

        return allValues

    }

    static boolean isBr(Object n) {
        return (n instanceof Node && n.name() == "br")
    }


    static List<List> partitionList(List input, Closure<Boolean> cl) {
        List ret = []
        List currentList = null
        input.each { inp ->
            if(cl.call(inp)) {
                if(currentList != null) {
                    ret.add(currentList)
                }
                currentList = null
            } else {
                if(currentList == null) {
                    currentList = [inp]
                } else {
                    currentList.add(inp)
                }
            }
        }
        if(currentList) {
            ret.add(currentList)
        }
        return ret


    }




}