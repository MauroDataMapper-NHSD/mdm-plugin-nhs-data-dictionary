package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.datasets.other

class OtherDataSetGroupRows extends OtherDataSetGroup {
    final OtherDataSetHeader header
    final List<OtherDataSetRow> rows

    OtherDataSetGroupRows(OtherDataSetHeader header, List<OtherDataSetRow> rows) {
        this.header = header
        this.rows = rows
    }
}
