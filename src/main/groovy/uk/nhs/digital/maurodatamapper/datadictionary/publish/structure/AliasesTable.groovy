package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure


import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Strow

import groovy.xml.MarkupBuilder

class AliasesTable extends StandardTable<AliasesRow> {
    static final List<StandardColumn> COLUMNS = [
        new StandardColumn("Context", "1*", "34%"),
        new StandardColumn("Alias", "2*", "66%"),
    ]

    AliasesTable(List<AliasesRow> rows) {
        super(rows)
    }

    @Override
    protected List<StandardColumn> getColumnDefinitions() {
        COLUMNS
    }
}

class AliasesRow extends StandardRow {
    final String context
    final String alias

    AliasesRow(String context, String alias) {
        this.context = context
        this.alias = alias
    }

    @Override
    Strow generateDita() {
        Strow.build() {
            stentry context
            stentry alias
        }
    }

    @Override
    void buildHtml(MarkupBuilder builder) {
        builder.tr(class: HtmlConstants.CSS_TABLE_ROW) {
            builder.td(class: HtmlConstants.CSS_TABLE_ENTRY) {
                mkp.yield(context)
            }
            builder.td(class: HtmlConstants.CSS_TABLE_ENTRY) {
                mkp.yield(alias)
            }
        }
    }
}