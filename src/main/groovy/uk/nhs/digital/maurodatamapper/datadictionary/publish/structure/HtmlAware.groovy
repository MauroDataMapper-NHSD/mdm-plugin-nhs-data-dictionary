package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import groovy.xml.MarkupBuilder

interface HtmlAware {
    String generateHtml()
}

interface HtmlBuilder {
    void buildHtml(MarkupBuilder builder)
}