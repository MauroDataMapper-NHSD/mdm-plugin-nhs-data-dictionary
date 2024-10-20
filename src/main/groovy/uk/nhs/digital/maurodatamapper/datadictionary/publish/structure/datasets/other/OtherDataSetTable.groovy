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
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Table
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Align

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishHelper
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishTarget
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DitaAware
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.HtmlBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.HtmlConstants
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.StandardColumn
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.datasets.DataSetTable

class OtherDataSetTable implements DataSetTable {
    static final OtherDataSetColumn MANDATION_COLUMN = new OtherDataSetColumn(
        "col1",
        "Mandation",
        "2*",
        "20%",
        Align.CENTER)

    static final OtherDataSetColumn DATA_ELEMENTS_COLUMN = new OtherDataSetColumn(
        "col2",
        "Data Elements",
        "8*",
        "80%")

    final OtherDataSetHeader header
    final List<OtherDataSetGroup> groups

    OtherDataSetTable(OtherDataSetHeader header, List<OtherDataSetGroup> groups) {
        this.header = header
        this.groups = groups
    }

    boolean hasSingleGroup() {
        groups.size() <= 1
    }

    List<OtherDataSetColumn> getColumnList() {
        [MANDATION_COLUMN, DATA_ELEMENTS_COLUMN]
    }

    @Override
    Table generateDita(PublishContext context) {
        String tableCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_DATA_SET_TABLE : ""
        String headCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE_HEAD : ""

        Table.build(outputClass: tableCssClass) {
            tgroup(cols: columnList.size()) {
                columnList.eachWithIndex { column, index ->
                    colspec(colnum: (index + 1), colName: column.colId, colwidth: column.relColWidth, align: column.alignment)
                }
                tHead {
                    row(outputClass: headCssClass) {
                        entry this.header.generateDita(context)
                    }

                    if (hasSingleGroup()) {
                        row {
                            columnList.each { column ->
                                entry {
                                    p column.name
                                }
                            }
                        }
                    }
                }
                tBody {
                    this.groups.each { group ->
                        List<Row> rows = group.generateDita(context)
                        rows.each { innerRow ->
                            row innerRow
                        }
                    }
                }
            }
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String tableCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE_HTML_TOPIC_TABLE : ""
        String headCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_TOPIC_HEAD}" : null
        String headerRowCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_ROW} ${HtmlConstants.CSS_TABLE_HEAD}" : null
        String rowCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_ROW}" : null
        String entryCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_ENTRY}" : ""
        String bodyCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_TOPIC_BODY}" : null

        builder.table (class: tableCssClass) {
            colgroup {
                columnList.each { column ->
                    builder.col(style: "width: ${column.cssColWidth}")
                }
            }
            builder.thead(class: headCssClass) {
                builder.tr(class: headerRowCssClass) {
                    this.header.buildHtml(context, builder)
                }

                if (hasSingleGroup()) {
                    builder.tr(class: rowCssClass) {
                        builder.th(class: "${entryCssClass} ${HtmlConstants.CSS_HTML_ALIGN_CENTER}") {
                            PublishHelper.buildHtmlParagraph(context, builder, MANDATION_COLUMN.name)
                        }

                        builder.th(class: entryCssClass) {
                            PublishHelper.buildHtmlParagraph(context, builder, DATA_ELEMENTS_COLUMN.name)
                        }
                    }
                }
            }
            builder.tbody(class: bodyCssClass) {
                this.groups.each { group ->
                    group.buildHtml(context, builder)
                }
            }
        }
    }
}

class OtherDataSetColumn extends StandardColumn {
    final String colId
    final Align alignment

    OtherDataSetColumn(
        String colId,
        String name,
        String relColWidth,
        String cssColWidth) {
        this(colId, name, relColWidth, cssColWidth, Align.LEFT)
    }

    OtherDataSetColumn(
        String colId,
        String name,
        String relColWidth,
        String cssColWidth,
        Align alignment) {
        super(name, relColWidth, cssColWidth)

        this.colId = colId
        this.alignment = alignment
    }
}