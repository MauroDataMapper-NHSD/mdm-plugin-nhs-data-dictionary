/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 */
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
