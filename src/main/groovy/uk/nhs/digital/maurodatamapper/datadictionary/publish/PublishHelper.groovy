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
package uk.nhs.digital.maurodatamapper.datadictionary.publish

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.XRef
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Scope

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItemState
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DiffStatus
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.HtmlConstants

class PublishHelper {
    static MarkupBuilder createMarkupBuilder(StringWriter writer, boolean prettyPrint = true) {
        MarkupBuilder builder = prettyPrint
            ? new MarkupBuilder(writer)
            : new MarkupBuilder(new IndentPrinter(writer, "", false))

        builder.omitEmptyAttributes = true
        builder.omitNullAttributes = true
        builder.doubleQuotes = true
        builder
    }

    static String createOfficialName(String name, DictionaryItemState state) {
        if (state == DictionaryItemState.RETIRED) {
            return "$name (Retired)"
        }

        name
    }

    static String createXrefId(String stereotype, String name, DictionaryItemState state) {
        String encodedName = replaceNonAlphaNumerics(name)
        String retiredSuffix = state == DictionaryItemState.RETIRED ? "_retired" : ""
        String key = "${stereotype}_${encodedName}${retiredSuffix}".replace(" ", "_").toLowerCase()
        key
    }

    static String getDiffCssClass(DiffStatus status) {
        switch (status) {
            case DiffStatus.NEW:
                return HtmlConstants.CSS_DIFF_NEW
            case DiffStatus.REMOVED:
                return HtmlConstants.CSS_DIFF_DELETED
            default:
                return ""
        }
    }

    static XRef buildExternalXRef(String url, String text) {
        XRef.build(
            scope: Scope.EXTERNAL,
            format: "html",
            href: url
        ) {
            txt text
        }
    }

    private static String replaceNonAlphaNumerics(String value) {
        if (!value) {
            return ""
        }

        value.replaceAll("[^A-Za-z0-9- ]", "")
    }
}
