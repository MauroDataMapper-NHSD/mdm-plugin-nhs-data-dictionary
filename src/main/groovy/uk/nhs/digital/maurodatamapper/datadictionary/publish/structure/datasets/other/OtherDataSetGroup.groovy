/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.datasets.other

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Row

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishTarget
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.ChangeAware
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DiffAware
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DiffObjectAware
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DiffStatus
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DitaAware
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.HtmlBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.HtmlConstants

class OtherDataSetGroup implements
    DitaAware<List<Row>>,
    HtmlBuilder,
    ChangeAware,
    DiffAware<OtherDataSetGroup, OtherDataSetGroup>,
    DiffObjectAware<OtherDataSetGroup> {
    final List<OtherDataSetRow> rows
    final OtherDataSetHeader header

    final DiffStatus diffStatus

    OtherDataSetGroup(List<OtherDataSetRow> rows, OtherDataSetHeader header) {
        this(rows, header, DiffStatus.NONE)
    }

    OtherDataSetGroup(List<OtherDataSetRow> rows, OtherDataSetHeader header, DiffStatus diffStatus) {
        this.rows = rows
        this.header = header
        this.diffStatus = diffStatus
    }

    @Override
    String getDiscriminator() {
        String headerDiscriminator = this.header?.discriminator ?: "header:none"
        String rowsDiscriminator = this.rows.collect { it.discriminator }.join("#")

        "rowgroup:${headerDiscriminator}_rows:${rowsDiscriminator}"
    }

    @Override
    OtherDataSetGroup cloneWithDiffStatus(DiffStatus diffStatus) {
        new OtherDataSetGroup(this.rows, this.header, diffStatus)
    }

    @Override
    OtherDataSetGroup produceDiff(OtherDataSetGroup previous) {
        return null
    }

    @Override
    List<Row> generateDita(PublishContext context) {
        List<Row> rowList = []

        if (this.header) {
            rowList.add(Row.build(outputClass: "table-primary") {
                entry(namest: OtherDataSetTable.MANDATION_COLUMN.colId, nameend: OtherDataSetTable.MANDATION_COLUMN.colId) {
                    p {
                        b OtherDataSetTable.MANDATION_COLUMN.name
                    }
                }
                entry this.header.generateDita(context)
            })
        }

        rowList.addAll(this.rows.collect { row -> row.generateDita(context) })

        rowList
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String rowCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_ROW} ${HtmlConstants.CSS_TABLE_PRIMARY}" : null
        String entryCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_ENTRY}" : ""

        if (this.header) {
            builder.tr(class: rowCssClass) {
                builder.td(class: "${entryCssClass} ${HtmlConstants.CSS_HTML_ALIGN_CENTER}") {
                    b OtherDataSetTable.MANDATION_COLUMN.name
                }
                this.header.buildHtml(context, builder)
            }
        }

        this.rows.each { row ->
            row.buildHtml(context, builder)
        }
    }
}
