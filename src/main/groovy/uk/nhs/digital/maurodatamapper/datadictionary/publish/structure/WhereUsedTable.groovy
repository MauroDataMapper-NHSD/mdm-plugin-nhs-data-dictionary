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

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Strow

import groovy.xml.MarkupBuilder

class WhereUsedTable extends StandardTable<WhereUsedRow> {
    static final List<StandardColumn> COLUMNS = [
        new StandardColumn("Type", "1*", "15%"),
        new StandardColumn("Link", "3*", "45%"),
        new StandardColumn("How used", "2*", "40%"),
    ]

    WhereUsedTable(List<WhereUsedRow> rows) {
        super(rows)
    }

    @Override
    protected List<StandardColumn> getColumnDefinitions() {
        COLUMNS
    }
}

class WhereUsedRow extends StandardRow {
    final String type
    final ItemLink link
    final String usage

    WhereUsedRow(String type, ItemLink link, String usage) {
        this.type = type
        this.link = link
        this.usage = usage
    }

    @Override
    Strow generateDita() {
        Strow.build() {
            stentry type
            stentry {
                xRef link.generateDita()
            }
            stentry usage
        }
    }

    @Override
    void buildHtml(MarkupBuilder builder) {
        builder.tr(class: HtmlConstants.CSS_TABLE_ROW) {
            builder.td(class: HtmlConstants.CSS_TABLE_ENTRY) {
                mkp.yield(type)
            }
            builder.td(class: HtmlConstants.CSS_TABLE_ENTRY) {
                link.buildHtml(builder)
            }
            builder.td(class: HtmlConstants.CSS_TABLE_ENTRY) {
                mkp.yield(usage)
            }
        }
    }
}
