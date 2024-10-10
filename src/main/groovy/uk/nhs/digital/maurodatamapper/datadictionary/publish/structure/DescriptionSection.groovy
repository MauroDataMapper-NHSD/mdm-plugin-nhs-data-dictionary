package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Body
import uk.ac.ox.softeng.maurodatamapper.dita.helpers.HtmlHelper

import groovy.xml.MarkupBuilder

class DescriptionSection extends Section {
    final String text

    DescriptionSection(DictionaryItem parent, String text) {
        super(parent, "description", "Description")

        this.text = text
            ? text.replace('<table', '<table class=\"table-striped\"')
            : ""
    }

    @Override
    protected Body generateBodyDita() {
        Body.build() {
            div HtmlHelper.replaceHtmlWithDita(text)
        }
    }

    @Override
    void buildHtml(MarkupBuilder builder) {
        builder.div(class: "- topic/div div") {
            // TODO: prefix sentence - for Element page type (X is the same as Y)
            builder.p(class: "- topic/p p") {
                mkp.yieldUnescaped(text)
            }
        }
    }
}
