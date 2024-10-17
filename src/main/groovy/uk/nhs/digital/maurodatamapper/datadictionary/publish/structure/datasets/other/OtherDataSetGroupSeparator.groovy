package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.datasets.other

class OtherDataSetGroupSeparator extends OtherDataSetGroup {
    final String text

    OtherDataSetGroupSeparator() {
        this("")
    }

    OtherDataSetGroupSeparator(String text) {
        this.text = text
    }
}
