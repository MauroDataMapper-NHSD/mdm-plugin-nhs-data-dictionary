package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder

class XRef extends BodyElement {

    String href
    String keyref
    String outputclass
    String text = ""

    @Override
    def writeAsXml(MarkupBuilder builder) {
        if(href) {
            builder.xref(href: href, outputclass: outputclass, text)
        }
        else {
            builder.xref(keyref: keyref, outputclass: outputclass, text)
        }

    }

    String outputAsString(String text = "") {
        if(href) {
            return "<xref href='${href}' outputclass='${outputclass}'>${text}</xref>"
        }
        else {
            return "<xref keyref='${keyref}' outputclass='${outputclass}'>${text}</xref>"
        }

    }




}
