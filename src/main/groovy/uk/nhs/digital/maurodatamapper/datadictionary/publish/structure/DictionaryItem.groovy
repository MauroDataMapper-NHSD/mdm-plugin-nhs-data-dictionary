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

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishHelper

enum DictionaryItemState {
    ACTIVE,
    RETIRED,
    PREPARATORY
}

class DictionaryItem implements DitaAware<Topic>, HtmlAware {
    final UUID id
    final UUID branchId
    final String stereotype
    final String name
    final DictionaryItemState state
    final String outputClass
    final String description

    final List<Section> sections = []

    DictionaryItem(
        UUID id,
        UUID branchId,
        String stereotype,
        String name,
        DictionaryItemState state,
        String outputClass,
        String description) {
        this.id = id
        this.branchId = branchId
        this.stereotype = stereotype
        this.name = name
        this.state = state
        this.outputClass = outputClass
        this.description = description
    }

    DictionaryItem addSection(Section section) {
        if (section) {
            sections.add(section)
        }
        this
    }

    String getOfficialName() {
        PublishHelper.createOfficialName(name, state)
    }

    String getXrefId() {
        PublishHelper.createXrefId(stereotype, name, state)
    }

    @Override
    Topic generateDita() {
        String titleOutputClass = this.outputClass

        Topic.build(id: xrefId) {
            title (outputClass: titleOutputClass) {
                text getOfficialName()
            }
            shortdesc description
            sections.each { section ->
                topic section.generateDita()
            }
        }
    }

    @Override
    String generateHtml() {
        StringWriter writer = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(writer)

        String titleCssClass = "${HtmlConstants.CSS_TOPIC_TITLE} ${outputClass}"

        builder.h1(class: titleCssClass) {
            mkp.yield(getOfficialName())
        }

        if (description) {
            builder.div(class: HtmlConstants.CSS_TOPIC_BODY) {
                builder.p(class: HtmlConstants.CSS_TOPIC_SHORTDESC) {
                    mkp.yield(description)
                }
            }
        }

        writer.toString()
    }
}
