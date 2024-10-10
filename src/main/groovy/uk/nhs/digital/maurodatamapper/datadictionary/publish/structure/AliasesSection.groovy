package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Body

import groovy.xml.MarkupBuilder

class AliasesSection extends Section {
    final AliasesTable table

    AliasesSection(DictionaryItem parent, AliasesTable table) {
        super(parent, "aliases", "Also Known As")

        this.table = table
    }

    AliasesSection(DictionaryItem parent, List<AliasesRow> rows) {
        this(parent, new AliasesTable(rows))
    }

    @Override
    protected Body generateBodyDita() {
        Body.build() {
            p headerText
            simpletable table.generateDita()
        }
    }

    @Override
    void buildHtml(MarkupBuilder builder) {
        builder.p(class: "- topic/p p") {
            mkp.yield(headerText)
        }

        table.buildHtml(builder)
    }

    String getHeaderText() {
        "This ${parent.stereotype} is also known by these names:"
    }
}
