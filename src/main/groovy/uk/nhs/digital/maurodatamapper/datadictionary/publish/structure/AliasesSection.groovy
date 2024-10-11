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
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishTarget
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change

class AliasesSection extends Section {
    final AliasesTable table

    AliasesSection(DictionaryItem parent, AliasesTable table) {
        this(parent, "Also Known As", table)
    }

    AliasesSection(DictionaryItem parent, List<AliasesRow> rows) {
        this(parent, new AliasesTable(rows))
    }

    AliasesSection(DictionaryItem parent, String title, AliasesTable table) {
        super(parent, "aliases", title)

        this.table = table
    }

    @Override
    Section produceDiff(Section previous) {
        AliasesSection previousSection = previous as AliasesSection
        AliasesTable diffTable = this.table.produceDiff(previousSection?.table)
        if (!diffTable) {
            return null
        }

        new AliasesSection(this.parent, Change.ALIASES_TYPE, diffTable)
    }

    @Override
    protected Body generateBodyDita(PublishContext context) {
        Body.build() {
            p headerText
            simpletable table.generateDita(context)
        }
    }

    @Override
    protected Div generateDivDita(PublishContext context) {
        Div.build() {
            p headerText
            simpletable table.generateDita(context)
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String paragraphOutputClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_PARAGRAPH : ""

        builder.p(class: paragraphOutputClass) {
            mkp.yield(headerText)
        }

        table.buildHtml(context, builder)
    }

    String getHeaderText() {
        "This ${parent.stereotype} is also known by these names:"
    }
}
