package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder

class OtherMeta extends DitaElement{

    String name
    String content

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.othermeta (name: name, content: content)
    }



}
