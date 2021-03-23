package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder

class STHead extends BodyElement {

    List<STEntry> stEntries = []

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.sthead(outputclass:"thead-light" ) {
            stEntries.each { entry ->
                entry.writeAsXml(builder)
            }
        }
    }
}
