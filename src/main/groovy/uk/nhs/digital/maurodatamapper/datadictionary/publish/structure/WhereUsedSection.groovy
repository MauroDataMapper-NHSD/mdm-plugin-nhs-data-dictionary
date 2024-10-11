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

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Body
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext

class WhereUsedSection extends Section {
    final WhereUsedTable table

    WhereUsedSection(DictionaryItem parent, WhereUsedTable table) {
        super(parent, "whereUsed", "Where Used")

        this.table = table
    }

    WhereUsedSection(DictionaryItem parent, List<WhereUsedRow> rows) {
        this(parent, new WhereUsedTable(rows))
    }

    @Override
    Section produceDiff(Section previous) {
        // Do not compare "Where Used" section
        return null
    }

    @Override
    protected Body generateBodyDita(PublishContext context) {
        Body.build() {
            simpletable table.generateDita(context)
        }
    }

    @Override
    protected Div generateDivDita(PublishContext context) {
        Div.build() {
            simpletable table.generateDita(context)
        }
    }

    @Override
    void buildHtml(MarkupBuilder builder) {
        table.buildHtml(builder)
    }
}
