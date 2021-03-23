package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.BodyElement
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.DitaElement
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

class Entry extends DitaElement {

    List<BodyElement> contents = []
    String nameSt
    String nameEnd
    Integer moreRows
    String scope
    String align = ""

    Entry() { }

    Entry(String text) {
        this.contents.add(new Html(content: text))
    }

    Entry(BodyElement bodyElement) {
        this.contents.add(bodyElement)
    }

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.entry (morerows: moreRows, namest: nameSt, nameend: nameEnd, outputclass: outputClass, scope: scope, align: align){
            contents.each { content ->
                content.writeAsXml(builder)
            }
        }
    }
}
