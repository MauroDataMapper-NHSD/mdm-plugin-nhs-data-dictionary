package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList

class AliasesTable implements ContentAware {
    final DictionaryItem dictionaryItem
    final List<AliasesRow> rows

    AliasesTable(DictionaryItem dictionaryItem, List<AliasesRow> rows) {
        this.dictionaryItem = dictionaryItem
        this.rows = rows
    }

    @Override
    Div generateDita() {
        Div.build() {
            p "This ${dictionaryItem.stereotype} is also known by these names:"
            simpletable(relColWidth: new SpaceSeparatedStringList (["1*", "2*"]), outputClass: "table table-sm table-striped") {
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