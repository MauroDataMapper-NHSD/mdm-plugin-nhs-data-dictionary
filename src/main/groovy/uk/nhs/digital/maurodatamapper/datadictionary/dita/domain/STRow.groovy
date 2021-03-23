package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder

class STRow extends BodyElement {

    List<STEntry> stEntries = []

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.strow(outputclass: outputClass) {
            stEntries.each { entry ->
                entry.writeAsXml(builder)
            }
        }
    }
}
