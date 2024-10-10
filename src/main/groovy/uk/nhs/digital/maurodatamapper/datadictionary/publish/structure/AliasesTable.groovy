package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Simpletable
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList

import groovy.xml.MarkupBuilder

class AliasesTable implements DitaAware<Simpletable>, HtmlBuilder {
    final List<AliasesRow> rows

    AliasesTable(List<AliasesRow> rows) {
        this.rows = rows
    }

    @Override
    Simpletable generateDita() {
        Simpletable.build(relColWidth: new SpaceSeparatedStringList (["1*", "2*"]), outputClass: "table table-sm table-striped") {
            stHead (outputClass: "thead-light") {
                stentry "Context"
                stentry "Alias"
            }
            rows.each { row ->
                strow {
                    stentry row.context
                    stentry row.alias
                }
            }
        }
    }

    @Override
    void buildHtml(MarkupBuilder builder) {
        builder.div(class: "simpletable-container") {
            builder.table(class: "- topic/simpletable simpletable table table-striped table-sm") {
                colgroup {
                    builder.col(style: "width: 34%")
                    builder.col(style: "width: 66%")
                }
                thead {
                    builder.tr(class: "- topic/sthead sthead thead-light") {
                        builder.th(class: "- topic/stentry stentry") {
                            mkp.yield("Context")
                        }
                        builder.th(class: "- topic/stentry stentry") {
                            mkp.yield("Alias")
                        }
                    }
                }
                tbody {
                    rows.each { row ->
                        builder.tr(class: "- topic/strow strow") {
                            builder.td(class: "- topic/stentry stentry") {
                                mkp.yield(row.context)
                            }
                            builder.td(class: "- topic/stentry stentry") {
                                mkp.yield(row.alias)
                            }
                        }
                    }
                }
            }
        }
    }
}

class AliasesRow {
    final String context
    final String alias

    AliasesRow(String context, String alias) {
        this.context = context
        this.alias = alias
    }
}