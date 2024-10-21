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
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishHelper
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DiffStatus
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.HtmlConstants

class OtherDataSetGroupSeparator extends OtherDataSetGroup {
    final String text

    OtherDataSetGroupSeparator() {
        this("")
    }

    OtherDataSetGroupSeparator(String text) {
        this.text = text
    }

    @Override
    List<Row> generateDita(PublishContext context) {
        Row row = Row.build {
            entry (namest: "col1", nameend: "col2") {
                if (!this.text.empty) {
                    b this.text
                }
                else {
                    p ""
                }
            }
        }

        [row]
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String diffOutputClass = PublishHelper.getDiffCssClass(DiffStatus.NONE)
        String rowCssClass = context.rowCssClass
        String entryCssClass = context.getEntryCssClass(diffOutputClass) ?: ""
        String finalEntryCssClass = "${entryCssClass} ${HtmlConstants.CSS_HTML_ALIGN_CENTER}"

        builder.tr(class: rowCssClass) {
            builder.td(class: finalEntryCssClass, colspan: 2) {
                builder.b() {
                    mkp.yield(this.text)
                }
            }
        }
    }
}
