package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder
import groovy.xml.MarkupBuilderHelper

abstract class DitaElement {

    static XmlParser xmlParser = XmlParser.newInstance()

    String outputClass
    abstract def writeAsXml(MarkupBuilder builder)

    String toXmlString() {
        StringWriter stringWriter = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(stringWriter)
        def helper = new MarkupBuilderHelper(builder)
        helper.xmlDeclaration([version:'1.0', encoding:'UTF-8', standalone:'no'])
        builder.setOmitNullAttributes(true)
        builder.setOmitEmptyAttributes(true)

        writeAsXml(builder)

        return stringWriter.toString()
    }

    Node toXmlNode() {
        String xmlStr = toXmlString()
        xmlParser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)

        return xmlParser.parseText(xmlStr)

    }

}
