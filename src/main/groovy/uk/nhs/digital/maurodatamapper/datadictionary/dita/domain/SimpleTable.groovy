package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder

class SimpleTable extends BodyElement {

    String relColWidth = ""
    STHead stHead
    List<STRow> stRows = []

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.simpletable (relcolwidth: relColWidth, outputclass: "table table-striped table-sm"){
            stHead.writeAsXml(builder)
            stRows.each { row ->
                row.writeAsXml(builder)
            }
        }
    }
}
