package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import groovy.transform.CompileStatic
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary

/**
 * Basic/simplified model of the NHS Data Dictionary for testing
 */
@CompileStatic
class NhsDataDictionaryModel {
    /**
     * The ID of the root versioned folder for the data dictionary.
     */
    String id
    String label

    List<DataModelModel> dataSets = []
    String dataSetsFolderId
    String dataSetsChildFolderId

    DataModelModel classesAndAttributes
    DataModelModel dataElements

    TerminologyModel dataSetConstraints
    TerminologyModel nhsBusinessDefinitions
    TerminologyModel supportingInformation

    NhsDataDictionaryModel() {
        this.label = "NHS Data Dictionary"

        this.classesAndAttributes = new DataModelModel(NhsDataDictionary.CLASSES_MODEL_NAME, "")
        this.dataElements = new DataModelModel(NhsDataDictionary.DATA_ELEMENTS_CLASS_NAME, "")

        this.dataSetConstraints = new TerminologyModel(NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME)
        this.nhsBusinessDefinitions = new TerminologyModel(NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME)
        this.supportingInformation = new TerminologyModel(NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME)
    }
}

@CompileStatic
class DataModelModel {
    String id
    String label
    String description
    String path

    /**
     * Represents the single data type required for data elements
     */
    String dataTypeId

    List<DataClassModel> classes = []

    DataModelModel(String label, String description) {
        this.label = label
        this.description = description
    }
}

@CompileStatic
class DataClassModel {
    String id
    String label
    String description
    String path

    List<DataElementModel> elements = []

    DataClassModel(String label, String description, List<DataElementModel> elements) {
        this.label = label
        this.description = description
        this.elements = elements
    }
}

@CompileStatic
class DataElementModel {
    String id
    String label
    String description
    String path

    DataElementModel(String label, String description) {
        this.label = label
        this.description = description
    }
}

@CompileStatic
class TerminologyModel {
    String id
    String label
    String path
    List<TermModel> terms = []

    TerminologyModel(String label) {
        this.label = label
    }
}

@CompileStatic
class TermModel {
    String id
    String code
    String definition
    String description
    String path

    TermModel(String code, String definition, String description) {
        this.code = code
        this.definition = definition
        this.description = description
    }
}