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

class AliasesTable extends StandardTable<AliasesRow> {
    static final List<StandardColumn> COLUMNS = [
        new StandardColumn("Context", "1*", "34%"),
        new StandardColumn("Alias", "2*", "66%"),
    ]

    AliasesTable(List<AliasesRow> rows) {
        super(rows)
    }

    @Override
    protected List<StandardColumn> getColumnDefinitions() {
        COLUMNS
    }
}

class AliasesRow extends StandardRow {
    final String context
    final String alias

    AliasesRow(String context, String alias) {
        this.context = context
        this.alias = alias
    }

    @Override
    Strow generateDita() {
        Strow.build() {
            stentry context
            stentry alias
        }
    }

    @Override
    void buildHtml(MarkupBuilder builder) {
        builder.tr(class: HtmlConstants.CSS_TABLE_ROW) {
            builder.td(class: HtmlConstants.CSS_TABLE_ENTRY) {
                mkp.yield(context)
            }
            builder.td(class: HtmlConstants.CSS_TABLE_ENTRY) {
                mkp.yield(alias)
            }
        }
    }
}