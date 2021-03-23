package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder

class Prolog extends DitaElement{

    Metadata metadata

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.prolog {
            metadata.writeAsXml(builder)
        }
    }



}
