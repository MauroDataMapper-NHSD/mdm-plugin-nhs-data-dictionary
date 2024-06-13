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

    TerminologyModel dataSetConstraints

    NhsDataDictionaryModel() {
        this.label = "NHS Data Dictionary"
        this.dataSetConstraints = new TerminologyModel("Data Set Constraints")
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