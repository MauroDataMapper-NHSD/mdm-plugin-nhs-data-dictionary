package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder


class DlEntry extends DitaElement {

    String term
    List<String> definitions = []

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.dlentry {
            dt term
            definitions.each { definition ->
                dd definition
            }
        }
    }
}
