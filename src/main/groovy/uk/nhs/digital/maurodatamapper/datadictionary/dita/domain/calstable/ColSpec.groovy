package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.DitaElement

class ColSpec extends DitaElement {

    String colName
    String colNum
    String colWidth
    String align
    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.colspec (align: align, colname: colName, colnum: colNum, colwidth: colWidth)
    }
}
