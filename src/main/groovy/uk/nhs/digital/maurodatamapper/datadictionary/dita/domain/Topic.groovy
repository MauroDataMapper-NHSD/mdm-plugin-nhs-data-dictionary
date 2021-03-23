package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder

class Topic extends TopLevelDitaElement {

    String doctypeDecl = """<!DOCTYPE topic PUBLIC "-//OASIS//DTD DITA Topic//EN" "topic.dtd">"""


    String id
    String title
    String titleClass = null
    String shortDesc
    String abstr

    String searchTitle
    String navTitle


    //List<RelatedLink> relatedLinks

    Prolog prolog
    Body body = new Body()

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.topic(id: id) {
            title(outputclass: titleClass, title)
            if(searchTitle || navTitle) {
                titlealts {
                    if(navTitle) {
                        navtitle navTitle
                    }
                    if(searchTitle) {
                        searchtitle searchTitle
                    }

                }
            }
            if(shortDesc) {
                shortdesc shortDesc
            }
            if(abstr) {
                "abstract" abstr
            }
            if(prolog) {
                prolog.writeAsXml(builder)
            }
            if(body) {
                body.writeAsXml(builder)
            }


        }
    }




}
