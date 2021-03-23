package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder

class Map extends TopLevelDitaElement {

    String title
    String chunk
    TopicMeta topicMeta
    List<Ref> refs = []

    String doctypeDecl = """<!DOCTYPE map PUBLIC \"-//OASIS//DTD DITA Map//EN\" \"map.dtd\">"""

    def writeAsXml(MarkupBuilder builder) {
        builder.map(chunk: chunk) {
            if(title) {
                title title
            }
            if(topicMeta) {
                topicMeta.writeAsXml(builder)
            }
            refs.each { ref ->
                ref.writeAsXml(builder)
            }
        }

    }


}
