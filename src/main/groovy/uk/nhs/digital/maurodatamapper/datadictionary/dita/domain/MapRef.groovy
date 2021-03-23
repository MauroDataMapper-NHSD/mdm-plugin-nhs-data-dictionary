package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder
import org.apache.commons.lang3.StringUtils

class MapRef extends Ref{

    String href=""
    Boolean toc = true
    List<String> keys = []

    @Override
    def writeAsXml(MarkupBuilder builder) {
        String keysString = StringUtils.join(keys, " ")
        builder.mapref(href: href, toc: (toc?"yes":"no"), keys: keysString) {

        }
    }

}
