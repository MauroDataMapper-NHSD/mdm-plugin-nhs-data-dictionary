package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Simpletable
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList

import groovy.xml.MarkupBuilder

class WhereUsedTable implements DitaAware<Simpletable>, HtmlBuilder {
    final List<WhereUsedRow> rows

    WhereUsedTable(List<WhereUsedRow> rows) {
        this.rows = rows
    }

    @Override
    Simpletable generateDita() {
        Simpletable.build(relColWidth: new SpaceSeparatedStringList (["1*","3*","2*"]), outputClass: "table table-sm table-striped") {
            stHead (outputClass: "thead-light") {
                stentry "Type"
                stentry "Link"
                stentry "How used"
            }
            rows.each { row ->
                strow {
                    stentry row.type
                    stentry {
                        xRef row.link.generateDita()
                    }
                    stentry row.usage
                }
            }
        }
    }

    @Override
    void buildHtml(MarkupBuilder builder) {
        builder.div(class: "simpletable-container") {
            builder.table(class: "- topic/simpletable simpletable table table-striped table-sm") {
                colgroup {
                    builder.col(style: "width: 15%")
                    builder.col(style: "width: 45%")
                    builder.col(style: "width: 40%")
                }
                thead {
                    builder.tr(class: "- topic/sthead sthead thead-light") {
                        builder.th(class: "- topic/stentry stentry") {
                            mkp.yield("Type")
                        }
                        builder.th(class: "- topic/stentry stentry") {
                            mkp.yield("Link")
                        }
                        builder.th(class: "- topic/stentry stentry") {
                            mkp.yield("How used")
                        }
                    }
                }
                tbody {
                    rows.each { row ->
                        builder.tr(class: "- topic/strow strow") {
                            builder.td(class: "- topic/stentry stentry") {
                                mkp.yield(row.type)
                            }
                            builder.td(class: "- topic/stentry stentry") {
                                row.link.buildHtml(builder)
                            }
                            builder.td(class: "- topic/stentry stentry") {
                                mkp.yield(row.usage)
                            }
                        }
                    }
                }
            }
        }
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
