package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder

class Paragraph extends BodyElement{

    String content

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.p(content)
    }
}
