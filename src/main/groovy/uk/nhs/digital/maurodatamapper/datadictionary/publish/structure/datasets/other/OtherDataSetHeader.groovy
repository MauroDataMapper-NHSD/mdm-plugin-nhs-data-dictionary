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

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Entry

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishHelper
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishTarget
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.ChangeAware
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DiffObjectAware
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DiffStatus
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DitaAware
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.HtmlBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.HtmlConstants

enum OtherDataSetHeaderType {
    TABLE,
    GROUP
}

class OtherDataSetHeader
    implements
        DitaAware<Entry>,
        HtmlBuilder,
        ChangeAware,
        DiffObjectAware<OtherDataSetHeader> {
    final OtherDataSetHeaderType type
    final String name
    final String description    // Optional

    final DiffStatus diffStatus

    OtherDataSetHeader(OtherDataSetHeaderType type, String name, String description) {
        this(type, name, description, DiffStatus.NONE)
    }

    OtherDataSetHeader(OtherDataSetHeaderType type, String name, String description, DiffStatus diffStatus) {
        this.type = type
        this.name = name
        this.description = description
        this.diffStatus = diffStatus
    }

    @Override
    String getDiscriminator() {
        "${type}_${name}"
    }

    @Override
    OtherDataSetHeader cloneWithDiffStatus(DiffStatus diffStatus) {
        new OtherDataSetHeader(this.type, this.name, this.description, diffStatus)
    }

    @Override
    Entry generateDita(PublishContext context) {
        String nameStart = type == OtherDataSetHeaderType.TABLE
            ? OtherDataSetTable.MANDATION_COLUMN.colId
            : OtherDataSetTable.DATA_ELEMENTS_COLUMN.colId

        Entry.build(namest: nameStart, nameend: OtherDataSetTable.DATA_ELEMENTS_COLUMN.colId) {
            b this.name
            if (this.description) {
                p this.description
            }
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String entryCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_ENTRY}" : ""

        if (type == OtherDataSetHeaderType.TABLE) {
            builder.th(colspan: 2, class: "${entryCssClass} ${HtmlConstants.CSS_HTML_ALIGN_CENTER}") {
                b this.name
                if (this.description) {
                    PublishHelper.buildHtmlParagraph(context, builder, this.description)
                }
            }
        }
        else {
            builder.td(class: entryCssClass) {
                b this.name
                if (this.description) {
                    PublishHelper.buildHtmlParagraph(context, builder, this.description)
                }
            }
        }
    }
}
