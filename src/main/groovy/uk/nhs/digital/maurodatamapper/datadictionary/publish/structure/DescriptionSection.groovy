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
import uk.ac.ox.softeng.maurodatamapper.dita.helpers.HtmlHelper

import groovy.xml.MarkupBuilder

class DescriptionSection extends Section {
    final String text

    DescriptionSection(DictionaryItem parent, String text) {
        super(parent, "description", "Description")

        this.text = text
            ? text.replace('<table', '<table class=\"table-striped\"')
            : ""
    }

    @Override
    protected Body generateBodyDita() {
        Body.build() {
            div HtmlHelper.replaceHtmlWithDita(text)
        }
    }

    @Override
    void buildHtml(MarkupBuilder builder) {
        builder.div(class: HtmlConstants.CSS_TOPIC_DIV) {
            // TODO: prefix sentence - for Element page type (X is the same as Y)
            builder.p(class: HtmlConstants.CSS_TOPIC_PARAGRAPH) {
                mkp.yieldUnescaped(text)
            }
        }
    }
}
