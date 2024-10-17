package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.datasets.other

class OtherDataSetHeader {
    final String name
    final String description    // Optional

    OtherDataSetHeader(String name) {
        this(name, "")
    }

    OtherDataSetHeader(String name, String description) {
        this.name
        this.description = description
    }
}
