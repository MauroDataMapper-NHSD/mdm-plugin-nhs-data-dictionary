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
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic

import groovy.xml.MarkupBuilder

abstract class Section implements DitaAware<Topic>, HtmlAware, HtmlBuilder {
    final DictionaryItem parent

    final String type
    final String title

    Section(DictionaryItem parent, String type, String title) {
        this.parent = parent
        this.type = type
        this.title = title
    }

    @Override
    Topic generateDita() {
        String xrefId = "${parent.xrefId}_${type}"

        Topic.build(id: xrefId) {
            title title
            body generateBodyDita()
        }
    }

    protected abstract Body generateBodyDita()

    @Override
    String generateHtml() {
        StringWriter writer = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(writer)

        builder.div(class: HtmlConstants.CSS_TOPIC_BODY) {
            buildHtml(builder)
        }

        writer.toString()
    }
}
