/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Strow
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.helpers.HtmlHelper
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

class NhsDDCode {

    String code
    String definition
    String publishDate
    String webOrder
    String webPresentation
    Boolean isDefault

    NhsDDAttribute owningAttribute
    List<NhsDDElement> usedByElements = []

    Term catalogueItem

    boolean isRetired
    String retiredDate

    Map<String, String> propertiesAsMap() {
        return [
            "publishDate"       : publishDate,
            "isRetired"         : isRetired.toString(),
            "retiredDate"       : retiredDate,
            "webOrder"          : webOrder,
            "webPresentation"   : webPresentation,
            "isDefault"         : isDefault.toString()
        ]
    }

    NhsDDCode(NhsDDAttribute owningAttribute) {
        this.owningAttribute = owningAttribute
    }

    NhsDDCode(NhsDDElement owningElement) {
        this.usedByElements.add(owningElement)
    }

    NhsDDCode() {

    }

    Strow toDitaTableRow() {
        Strow.build(outputClass: isRetired ? "retired" : "") {
            stentry code
            stentry {
                if(webPresentation) {
                    div HtmlHelper.replaceHtmlWithDita(webPresentation)
                } else {
                    txt definition
                }
            }
        }
    }

    static getCodesTopic(String topicId, String topicTitle, List<NhsDDCode> orderedCodes) {
        Topic.build (id: topicId) {
            title topicTitle
            body {
                simpletable(relColWidth: new SpaceSeparatedStringList (["1*", "4*"]), outputClass: "table table-sm") {
                    stHead (outputClass: "thead-light") {
                        stentry "Code"
                        stentry "Description"
                    }
                    orderedCodes.each {code ->
                        strow code.toDitaTableRow()
                    }
                }
            }
        }
    }

}
