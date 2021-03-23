package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.DitaElement

class TBody extends DitaElement {

    List<Row> rows = []

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.tbody (){
            rows.each { row ->
                row.writeAsXml(builder)
            }
        }
    }
}
