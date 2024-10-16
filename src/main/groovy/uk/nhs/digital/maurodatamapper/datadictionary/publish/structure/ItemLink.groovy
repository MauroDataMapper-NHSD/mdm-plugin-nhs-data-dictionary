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

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.XRef

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishHelper
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.ChangeAware

class ItemLink implements DitaAware<XRef>, HtmlBuilder, ChangeAware {
    final UUID itemId
    final UUID branchId

    final String stereotype
    final DictionaryItemState state
    final String name

    final String outputClass

    final DiffStatus diffStatus

    ItemLink(
        UUID itemId,
        UUID branchId,
        String stereotype,
        DictionaryItemState state,
        String name,
        String outputClass) {
        this(itemId, branchId, stereotype, state, name, outputClass, DiffStatus.NONE)
    }

    ItemLink(
        UUID itemId,
        UUID branchId,
        String stereotype,
        DictionaryItemState state,
        String name,
        String outputClass,
        DiffStatus diffStatus) {
        this.itemId = itemId
        this.branchId = branchId
        this.stereotype = stereotype
        this.state = state
        this.name = name
        this.outputClass = outputClass
        this.diffStatus = diffStatus
    }

    static ItemLink create(NhsDataDictionaryComponent component) {
        new ItemLink(
            component.catalogueItemId,
            component.branchId,
            component.stereotype,
            component.itemState,
            component.name,
            component.outputClass)
    }

    @Override
    XRef generateDita(PublishContext context) {
        String linkOutputClass = outputClass
        String xrefId = PublishHelper.createXrefId(stereotype, name, state)

        XRef.build(format: "html", keyRef: xrefId, outputClass: linkOutputClass) {
            txt name
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String href = "#/preview/${branchId}/${outputClass}/${itemId}"

        builder.a(class: outputClass, title: name, href: href) {
            mkp.yield(name)
        }
    }

    @Override
    String getDiscriminator() {
        itemId.toString()
    }

    ItemLink cloneWithDiff(DiffStatus diffStatus) {
        new ItemLink(this.itemId, this.branchId, this.stereotype, this.state, this.name, this.outputClass, diffStatus)
    }
}
