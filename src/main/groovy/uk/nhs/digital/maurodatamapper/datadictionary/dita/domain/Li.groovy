package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder


class Li extends DitaElement {

    String value
    XRef xref

    @Override
    def writeAsXml(MarkupBuilder builder) {
        if(value) {
            builder.li (value)
        } else if (xref) {
            builder.li {
                xref.writeAsXml(builder)
            }
        }
    }
}
