package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder

class Dl extends BodyElement {

    List<DlEntry> dlEntries = []

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.dl{
            dlEntries.each { dle ->
                dle.writeAsXml(builder)
            }
        }
    }
}
