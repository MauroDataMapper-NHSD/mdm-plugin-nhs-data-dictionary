package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.datasets.other

import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.datasets.DataSetTable

class OtherDataSetTable implements DataSetTable {
    final OtherDataSetHeader header
    final List<OtherDataSetGroup> groups

    OtherDataSetTable(OtherDataSetHeader header, List<OtherDataSetGroup> groups) {
        this.header = header
        this.groups = groups
    }
}
