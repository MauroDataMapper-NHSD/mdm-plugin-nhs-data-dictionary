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
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishHelper
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishTarget
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.ChangeAware
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.ChangeFunctions

class AliasesTable extends StandardTable<AliasesRow> {
    static final List<StandardColumn> COLUMNS = [
        new StandardColumn("Context", "1*", "34%"),
        new StandardColumn("Alias", "2*", "66%"),
    ]

    AliasesTable(List<AliasesRow> rows) {
        super(rows)
    }

    AliasesTable produceDiff(AliasesTable previous) {
        List<AliasesRow> currentRows = this.rows
        List<AliasesRow> previousRows = previous ? previous.rows : []

        if (currentRows.empty && previousRows.empty) {
            return null
        }

        if (ChangeFunctions.areEqual(currentRows, previousRows)) {
            return null
        }

        List<AliasesRow> newRows = ChangeFunctions.getDifferences(currentRows, previousRows)
        List<AliasesRow> removedRows = ChangeFunctions.getDifferences(previousRows, currentRows)

        if (newRows.empty && removedRows.empty) {
            return null
        }

        List<AliasesRow> diffRows = []
        currentRows.each { row ->
            if (newRows.any { it.discriminator == row.discriminator }) {
                diffRows.add(row.cloneWithDiff(DiffStatus.NEW))
            }
            else {
                diffRows.add(row)
            }
        }
        removedRows.each { row ->
            diffRows.add(row.cloneWithDiff(DiffStatus.REMOVED))
        }

        new AliasesTable(diffRows)
    }

    @Override
    protected List<StandardColumn> getColumnDefinitions() {
        COLUMNS
    }
}

class AliasesRow extends StandardRow implements ChangeAware {
    final String context
    final String alias

    final DiffStatus diffStatus

    AliasesRow(String context, String alias) {
        this(context, alias, DiffStatus.NONE)
    }

    AliasesRow(String context, String alias, DiffStatus diffStatus) {
        this.context = context
        this.alias = alias
        this.diffStatus = diffStatus
    }

    AliasesRow cloneWithDiff(DiffStatus diffStatus) {
        new AliasesRow(this.context, this.alias, diffStatus)
    }

    @Override
    String getDiscriminator() {
        "context:${context}-alias:${alias}"
    }

    @Override
    Strow generateDita(PublishContext context) {
        String outputClass = PublishHelper.getDiffCssClass(diffStatus)

        Strow.build() {
            stentry(outputClass: outputClass) {
                txt this.context
            }
            stentry(outputClass: outputClass) {
                txt this.alias
            }
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String diffOutputClass = PublishHelper.getDiffCssClass(diffStatus)

        String rowCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE_ROW : null
        String entryCssClass = context.target == PublishTarget.WEBSITE
            ? HtmlConstants.CSS_TABLE_ENTRY
            : !diffOutputClass.empty
            ? diffOutputClass
            : null

        builder.tr(class: rowCssClass) {
            builder.td(class: entryCssClass) {
                mkp.yield(this.context)
            }
            builder.td(class: entryCssClass) {
                mkp.yield(this.alias)
            }
        }
    }
}