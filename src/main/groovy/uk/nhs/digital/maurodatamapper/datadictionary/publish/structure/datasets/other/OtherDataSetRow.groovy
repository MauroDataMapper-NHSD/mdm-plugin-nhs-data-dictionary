package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.datasets.other

class OtherDataSetRow {
    final String mandation
    final OtherDataSetCell cell

    OtherDataSetRow(String mandation, OtherDataSetCell cell) {
        this.mandation = mandation
        this.cell = cell
    }
}
