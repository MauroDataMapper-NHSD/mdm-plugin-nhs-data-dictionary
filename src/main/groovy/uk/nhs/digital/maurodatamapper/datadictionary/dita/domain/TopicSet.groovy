package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder
import org.apache.commons.lang3.StringUtils

class TopicSet extends Ref {

    String navtitle=""
    String id=""
    String href=""
    Boolean toc = true
    Boolean linking = true
    String chunk
    List<Ref> refs = []
    List<String> keys = []

    @Override
    def writeAsXml(MarkupBuilder builder) {
        String keysString = StringUtils.join(keys, " ")
        builder.topicset(navtitle: navtitle, id: id, href: href, toc: (toc?"yes":"no"), linking: (linking?"normal":"none"), keys: keysString, chunk: chunk) {
            refs.each {
                it.writeAsXml(builder)
            }
        }
    }

}
