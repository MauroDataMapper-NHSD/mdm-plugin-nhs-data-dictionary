package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder

class Metadata extends DitaElement{

    List<OtherMeta> otherMeta = []
    List<String> keywords = []
    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.metadata {
            if(keywords) {
                builder.keywords {
                    keywords.each {it
                        builder.keyword (it)
                    }
                }
            }
            otherMeta.each { om ->
                om.writeAsXml(builder)
            }
        }
    }



}
