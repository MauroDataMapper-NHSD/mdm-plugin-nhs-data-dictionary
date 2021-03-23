package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder

class Section extends BodyElement{

    String title
    List<BodyElement> bodyElements = []

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.section {
        builder.title(title)
            bodyElements.each {be ->
                be.writeAsXml(builder)
            }
        }
    }
}
