package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.BodyElement
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

class Table extends BodyElement {

    List<TGroup> tGroups = []

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.table (outputclass: "table table-striped table-bordered table-sm"){
            tGroups.each { tGroup ->
                tGroup.writeAsXml(builder)
            }
        }
    }

    Table() {}

    Table(List<String> colWidths, List<String> headers = null) {
        TGroup tGroup = new TGroup(cols: colWidths.size())
        tGroups.add(tGroup)

        colWidths.eachWithIndex{ String colWidth, int i ->
            ColSpec columni = new ColSpec(colNum: i+1, colName: "col${i+1}", colWidth: colWidth)
            tGroup.colSpecs.add(columni)
        }
        tGroup.tHead = new THead()
        if(headers) {
            Row headerRow = new Row(outputClass: "thead-light")
            tGroup.tHead.rows = [headerRow]
            headers.eachWithIndex { String header, int i ->
                Entry nameEntry = new Entry(nameSt: "col${i + 1}", nameEnd: "col${i + 1}")
                nameEntry.contents.add(new Html(content: "<b>${header}</b>"))
                headerRow.entries.add(nameEntry)

            }
        }
        tGroup.tBody = new TBody()
    }


    void addRow(List<Object> contents) {

        List<Entry> entries = contents.collect { content ->
            new Entry(content)
        }

        tGroups[0].tBody.rows.add(new Row(entries: entries))
    }

}
