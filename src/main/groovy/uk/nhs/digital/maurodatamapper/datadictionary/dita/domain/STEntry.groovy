package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder

class STEntry extends BodyElement {

    String value
    XRef xref
    BodyElement bodyElement

    @Override
    def writeAsXml(MarkupBuilder builder) {
        if(value) {
            builder.stentry(value)
        } else if (xref) {
            builder.stentry {
                xref.writeAsXml(builder)
            }
        } else if (bodyElement) {
            builder.stentry {
                bodyElement.writeAsXml(builder)
            }
        } else {
            builder.stentry {}
        }
    }
}
