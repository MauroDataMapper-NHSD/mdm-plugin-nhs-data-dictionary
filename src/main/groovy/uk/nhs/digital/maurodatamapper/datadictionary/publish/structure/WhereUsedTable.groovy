package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div

class WhereUsedTable implements ContentAware {
    final List<WhereUsedRow> rows = []

    @Override
    Div generateDita() {
        return null
    }
}

class WhereUsedRow {
    final String type
    final ItemLink link
    final String usage

    WhereUsedRow(String type, ItemLink link, String usage) {
        this.type = type
        this.link = link
        this.usage = usage
    }
}
