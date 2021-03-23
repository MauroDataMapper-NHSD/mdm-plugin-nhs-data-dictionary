package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder

class Body extends DitaElement{

    List<BodyElement> bodyElements = []

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.body {
            bodyElements.each {be ->
                be.writeAsXml(builder)
            }
        }
    }



}
