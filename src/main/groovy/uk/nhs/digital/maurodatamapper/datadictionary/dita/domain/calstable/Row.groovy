package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.DitaElement

class Row extends DitaElement {

    List<Entry> entries = []

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.row (outputclass: outputClass){
            entries.each { entry ->
                entry.writeAsXml(builder)
            }
        }
    }
}
