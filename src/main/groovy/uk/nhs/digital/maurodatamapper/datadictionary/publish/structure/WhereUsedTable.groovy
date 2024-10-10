package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Strow

import groovy.xml.MarkupBuilder

class WhereUsedTable extends StandardTable<WhereUsedRow> {
    static final List<StandardColumn> COLUMNS = [
        new StandardColumn("Type", "1*", "15%"),
        new StandardColumn("Link", "3*", "45%"),
        new StandardColumn("How used", "2*", "40%"),
    ]

    WhereUsedTable(List<WhereUsedRow> rows) {
        super(rows)
    }

    @Override
    protected List<StandardColumn> getColumnDefinitions() {
        COLUMNS
    }
}

class WhereUsedRow extends StandardRow {
    final String type
    final ItemLink link
    final String usage

    WhereUsedRow(String type, ItemLink link, String usage) {
        this.type = type
        this.link = link
        this.usage = usage
    }

    @Override
    Strow generateDita() {
        Strow.build() {
            stentry type
            stentry {
                xRef link.generateDita()
            }
            stentry usage
        }
    }

    @Override
    void buildHtml(MarkupBuilder builder) {
        builder.tr(class: HtmlConstants.CSS_TABLE_ROW) {
            builder.td(class: HtmlConstants.CSS_TABLE_ENTRY) {
                mkp.yield(type)
            }
            builder.td(class: HtmlConstants.CSS_TABLE_ENTRY) {
                link.buildHtml(builder)
            }
            builder.td(class: HtmlConstants.CSS_TABLE_ENTRY) {
                mkp.yield(usage)
            }
        }
    }
}
