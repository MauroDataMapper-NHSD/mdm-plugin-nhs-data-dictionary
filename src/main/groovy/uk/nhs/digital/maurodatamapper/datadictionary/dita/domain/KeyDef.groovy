package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder
import org.apache.commons.lang3.StringUtils

class KeyDef extends Ref{

    String href=""
    String format
    List<String> keys = []
    String scope

    @Override
    def writeAsXml(MarkupBuilder builder) {
        String keysString = StringUtils.join(keys, " ")
        builder.keydef( keys: keysString, href: href, format: format, scope: scope) {}
    }

}
