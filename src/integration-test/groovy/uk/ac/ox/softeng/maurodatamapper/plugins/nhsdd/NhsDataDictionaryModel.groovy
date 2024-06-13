package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import groovy.transform.CompileStatic

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

    DataModelModel classesAndAttributes

    TerminologyModel dataSetConstraints

    NhsDataDictionaryModel() {
        this.label = "NHS Data Dictionary"

        this.classesAndAttributes = new DataModelModel("Classes and Attributes")

        this.dataSetConstraints = new TerminologyModel("Data Set Constraints")
    }
}

@CompileStatic
class DataModelModel {
    String id
    String label

    /**
     * Represents the single data type required for data elements
     */
    String dataTypeId

    List<DataClassModel> classes = []

    DataModelModel(String label) {
        this.label = label
    }
}

@CompileStatic
class DataClassModel {
    String id
    String label

    List<DataElementModel> elements = []

    DataClassModel(String label, List<DataElementModel> elements) {
        this.label = label
        this.elements = elements
    }
}

@CompileStatic
class DataElementModel {
    String id
    String label

    DataElementModel(String label) {
        this.label = label
    }
}

@CompileStatic
class TerminologyModel {
    String id
    String label
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

    TermModel(String code, String definition) {
        this.code = code
        this.definition = definition
    }
}