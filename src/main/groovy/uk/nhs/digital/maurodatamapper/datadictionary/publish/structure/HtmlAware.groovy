package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import groovy.xml.MarkupBuilder

interface HtmlAware {
    String generateHtml()
}

interface HtmlBuilder {
    void buildHtml(MarkupBuilder builder)
}

class HtmlConstants {
    static final CSS_TOPIC_BODY = "- topic/body body"
    static final CSS_TOPIC_TITLE = "title topictitle1"
    static final CSS_TOPIC_SHORTDESC = "- topic/shortdesc shortdesc"
    static final CSS_TOPIC_PARAGRAPH = "- topic/p p"
    static final CSS_TOPIC_DIV = "- topic/div div"

    static final CSS_TABLE = "table table-sm table-striped"
    static final CSS_TABLE_HTML_CONTAINER = "simpletable-container"
    static final CSS_TABLE_HTML_TOPIC_SIMPLETABLE = "- topic/simpletable simpletable"
    static final CSS_TABLE_HEAD = "thead-light"
    static final CSS_TABLE_TOPIC_HEAD = "- topic/sthead sthead"
    static final CSS_TABLE_ROW = "- topic/strow strow"
    static final CSS_TABLE_ENTRY = "- topic/stentry stentry"
}