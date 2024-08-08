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
package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Strow
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.helpers.HtmlHelper
import uk.ac.ox.softeng.maurodatamapper.dita.meta.DitaElement
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import groovy.xml.MarkupBuilder

class NhsDDCode {

    String code
    String definition
    String publishDate
    Integer webOrder
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
            "webOrder"          : "${webOrder}",
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
                simpletable(relColWidth: new SpaceSeparatedStringList (["1*", "4*"]), outputClass: "table table-sm table-striped") {
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

    static String createCodesTableChangeHtml(List<NhsDDCode> currentCodes, List<NhsDDCode> previousCodes) {
        StringWriter stringWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(stringWriter)

        markupBuilder.div {
            table(class: "codes-table") {
                thead {
                    th "Code"
                    th "Description"
                }
                tbody {
                    currentCodes.each { code ->
                        NhsDDCode existing = previousCodes.find { previous -> previous.code == code.code }
                        if (!existing || (existing && existing.definition != code.definition)) {
                            markupBuilder.tr {
                                markupBuilder.td(class: "new") {
                                    mkp.yield(code.code)
                                }
                                markupBuilder.td(class: "new") {
                                    code.webPresentation ? mkp.yieldUnescaped(code.webPresentation) : mkp.yield(code.definition)
                                }
                            }
                        }
                        else {
                            markupBuilder.tr {
                                td code.code
                                td {
                                    code.webPresentation ? mkp.yieldUnescaped(code.webPresentation) : mkp.yield(code.definition)
                                }
                            }
                        }
                    }
                    previousCodes.each { code ->
                        NhsDDCode existing = currentCodes.find { current -> current.code == code.code }
                        if (!existing || (existing && existing.definition != code.definition)) {
                            markupBuilder.tr {
                                markupBuilder.td(class: "deleted") {
                                    mkp.yield(code.code)
                                }
                                markupBuilder.td(class: "deleted") {
                                    code.webPresentation ? mkp.yieldUnescaped(code.webPresentation) : mkp.yield(code.definition)
                                }
                            }
                        }
                    }
                }
            }
        }

        return stringWriter.toString()
    }

    static DitaElement createCodesTableChangeDita(List<NhsDDCode> currentCodes, List<NhsDDCode> previousCodes) {
        Div.build {
            simpletable(relColWidth: new SpaceSeparatedStringList (["1*", "4*"])) {
                stHead {
                    stentry "Code"
                    stentry "Description"
                }
                currentCodes.each { code ->
                    NhsDDCode existing = previousCodes.find { previous -> previous.code == code.code }
                    if (!existing || (existing && existing.definition != code.definition)) {
                        strow {
                            stentry(outputClass: "new") {
                                ph code.code
                            }
                            stentry(outputClass: "new") {
                                if (code.webPresentation) {
                                    div HtmlHelper.replaceHtmlWithDita(code.webPresentation)
                                }
                                else {
                                    ph code.definition
                                }
                            }
                        }
                    }
                    else {
                        strow {
                            stentry {
                                ph code.code
                            }
                            stentry {
                                if (code.webPresentation) {
                                    div HtmlHelper.replaceHtmlWithDita(code.webPresentation)
                                }
                                else {
                                    ph code.definition
                                }
                            }
                        }
                    }
                }
                previousCodes.each { code ->
                    NhsDDCode existing = currentCodes.find { current -> current.code == code.code }
                    if (!existing || (existing && existing.definition != code.definition)) {
                        strow {
                            stentry(outputClass: "deleted") {
                                ph code.code
                            }
                            stentry(outputClass: "deleted") {
                                if (code.webPresentation) {
                                    div HtmlHelper.replaceHtmlWithDita(code.webPresentation)
                                }
                                else {
                                    ph code.definition
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
