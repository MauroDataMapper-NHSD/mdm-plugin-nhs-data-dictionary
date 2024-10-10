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
package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Simpletable
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Strow
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList

import groovy.xml.MarkupBuilder

abstract class StandardTable<R extends StandardRow> implements DitaAware<Simpletable>, HtmlBuilder {
    final List<R> rows

    StandardTable(List<R> rows) {
        this.rows = rows
    }

    @Override
    Simpletable generateDita() {
        List<StandardColumn> columns = getColumnDefinitions()
        List<String> relColWidths = columns.collect {column -> column.relColWidth }

        Simpletable.build(relColWidth: new SpaceSeparatedStringList (relColWidths), outputClass: HtmlConstants.CSS_TABLE) {
            stHead (outputClass: HtmlConstants.CSS_TABLE_HEAD) {
                columns.each { column ->
                    stentry column.name
                }
            }
            rows.each { row ->
                strow row.generateDita()
            }
        }
    }

    @Override
    void buildHtml(MarkupBuilder builder) {
        List<StandardColumn> columns = getColumnDefinitions()

        builder.div(class: HtmlConstants.CSS_TABLE_HTML_CONTAINER) {
            builder.table(class: "${HtmlConstants.CSS_TABLE_HTML_TOPIC_SIMPLETABLE} $HtmlConstants.CSS_TABLE") {
                colgroup {
                    columns.each { column ->
                        builder.col(style: "width: ${column.cssColWidth}")
                    }
                }
                thead {
                    builder.tr(class: "${HtmlConstants.CSS_TABLE_TOPIC_HEAD} ${HtmlConstants.CSS_TABLE_HEAD}") {
                        columns.each { column ->
                            builder.th(class: HtmlConstants.CSS_TABLE_ENTRY) {
                                mkp.yield(column.name)
                            }
                        }
                    }
                }
                tbody {
                    rows.each { row ->
                        row.buildHtml(builder)
                    }
                }
            }
        }
    }

    protected abstract List<StandardColumn> getColumnDefinitions()
}

class StandardColumn {
    final String name
    final String relColWidth
    final String cssColWidth

    StandardColumn(String name, String relColWidth, String cssColWidth) {
        this.name = name
        this.relColWidth = relColWidth
        this.cssColWidth = cssColWidth
    }
}

abstract class StandardRow implements DitaAware<Strow>, HtmlBuilder {
}


