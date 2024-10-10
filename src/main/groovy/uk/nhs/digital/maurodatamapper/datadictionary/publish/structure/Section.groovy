package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Body
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic

import groovy.xml.MarkupBuilder

abstract class Section implements DitaAware<Topic>, HtmlAware, HtmlBuilder {
    final DictionaryItem parent

    final String type
    final String title

    Section(DictionaryItem parent, String type, String title) {
        this.parent = parent
        this.type = type
        this.title = title
    }

    @Override
    Topic generateDita() {
        String xrefId = "${parent.xrefId}_${type}"

        Topic.build(id: xrefId) {
            title title
            body generateBodyDita()
        }
    }

    protected abstract Body generateBodyDita()

    @Override
    String generateHtml() {
        StringWriter writer = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(writer)

        builder.div(class: "- topic/body body") {
            buildHtml(builder)
        }

        writer.toString()
    }
}
