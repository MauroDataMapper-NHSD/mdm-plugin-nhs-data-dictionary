package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.DitaElement

class THead extends DitaElement {

    List<Row> rows = []

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.thead (){
            rows.each { row ->
                row.writeAsXml(builder)
            }
        }
    }
}
