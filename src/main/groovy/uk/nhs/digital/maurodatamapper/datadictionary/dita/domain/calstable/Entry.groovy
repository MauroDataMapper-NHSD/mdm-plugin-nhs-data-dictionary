/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 */
package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.BodyElement
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.DitaElement
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

class Entry extends DitaElement {

    List<BodyElement> contents = []
    String nameSt
    String nameEnd
    Integer moreRows
    String scope
    String align = ""

    Entry() { }

    Entry(String text) {
        this.contents.add(new Html(content: text))
    }

    Entry(BodyElement bodyElement) {
        this.contents.add(bodyElement)
    }

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.entry (morerows: moreRows, namest: nameSt, nameend: nameEnd, outputclass: outputClass, scope: scope, align: align){
            contents.each { content ->
                content.writeAsXml(builder)
            }
        }
    }
}
