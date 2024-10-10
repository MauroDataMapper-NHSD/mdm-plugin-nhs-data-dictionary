package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Body

import groovy.xml.MarkupBuilder

class WhereUsedSection extends Section {
    final WhereUsedTable table

    WhereUsedSection(DictionaryItem parent, WhereUsedTable table) {
        super(parent, "whereUsed", "Where Used")

        this.table = table
    }

    WhereUsedSection(DictionaryItem parent, List<WhereUsedRow> rows) {
        this(parent, new WhereUsedTable(rows))
    }

    @Override
    protected Body generateBodyDita() {
        Body.build() {
            simpletable table.generateDita()
        }
    }

    @Override
    void buildHtml(MarkupBuilder builder) {
        table.buildHtml(builder)
    }
}
