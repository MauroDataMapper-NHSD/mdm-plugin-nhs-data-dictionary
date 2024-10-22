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
package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.datasets

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Body
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.ChangeFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishTarget
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItem
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DiffStatus
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.HtmlConstants
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.Section

class DataSetSection extends Section {
    final List<DataSetTable> tables

    DataSetSection(DictionaryItem parent, List<DataSetTable> tables) {
        super(parent, "specification", "Specification")

        this.tables = tables ?: []
    }

    @Override
    Section produceDiff(Section previous) {
        DataSetSection previousSection = previous as DataSetSection

        List<DataSetTable> currentList = this.tables
        List<DataSetTable> previousList = previousSection ? previousSection.tables : []

        List<DataSetTable> diffList = ChangeFunctions.buildDifferencesList(
            currentList,
            previousList,
            { DataSetTable currentItem, DataSetTable previousItem ->
                currentItem.produceDiff(previousItem)
            })

        if (diffList.every {it.hierarchicalDiffStatus == DiffStatus.NONE }) {
            // No differences found in these sections
            return null
        }

        // Otherwise, return all the calculated differences
        new DataSetSection(this.parent, diffList)
    }

    @Override
    protected Body generateBodyDita(PublishContext context) {
        Body.build() {
            // Use different closure name than "table", otherwise dita dsl thinks its a method call
            this.tables.each { tableStruct ->
                div {
                    table tableStruct.generateDita(context)
                }
            }
        }
    }

    @Override
    protected Div generateDivDita(PublishContext context) {
        Div.build() {
            // Use different closure name than "table", otherwise dita dsl thinks its a method call
            this.tables.each { tableStruct ->
                div {
                    table tableStruct.generateDita(context)
                }
            }
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String containerCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE_HTML_CONTAINER : null

        this.tables.each { table ->
            builder.div(class: containerCssClass) {
                table.buildHtml(context, builder)
            }
        }
    }
}
