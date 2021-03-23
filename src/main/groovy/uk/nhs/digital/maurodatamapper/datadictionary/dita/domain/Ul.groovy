package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder

class Ul extends BodyElement {

    List<Li> entries = []

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.ul{
            entries.each { li ->
                li.writeAsXml(builder)
            }
        }
    }
}
