package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder
import org.apache.commons.lang3.StringUtils

class TopicRef extends Ref {

    String href=""
    Boolean toc = true
    Boolean linking = true
    String navtitle
    String chunk
    String id
    List<Ref> subTopics = []
    List<String> keys = []

    @Override
    def writeAsXml(MarkupBuilder builder) {
        String keysString = StringUtils.join(keys, " ")
        builder.topicref(
                href: href,
                toc: (toc?"yes":"no"),
                linking: (linking?"normal":"none"),
                keys: keysString,
                navtitle: navtitle,
                chunk: chunk,
                id: id) {
            subTopics.each {
                it.writeAsXml(builder)
            }
        }
    }

}
