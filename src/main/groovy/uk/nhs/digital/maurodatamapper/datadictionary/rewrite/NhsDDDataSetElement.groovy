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
package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

import groovy.xml.MarkupBuilder

class NhsDDDataSetElement{

    String name

    String mandation
    String minMultiplicity
    String maxMultiplicity
    String constraints
    UUID elementId

    NhsDDElement reuseElement
    NhsDataDictionary dataDictionary

    NhsDDDataSetElement(DataElement dataElement, NhsDataDictionary dataDictionary) {
        this.name = dataElement.label
        this.elementId = dataElement.id
        this.dataDictionary = dataDictionary
    }

    def createHtmlLink(MarkupBuilder markupBuilder) {
        String link = "#/preview/${dataDictionary.containingVersionedFolder.id.toString()}/element/${elementId.toString()}"
        markupBuilder.a (href: link, class: "element") {
            mkp.yield(name)
        }

    }

}
