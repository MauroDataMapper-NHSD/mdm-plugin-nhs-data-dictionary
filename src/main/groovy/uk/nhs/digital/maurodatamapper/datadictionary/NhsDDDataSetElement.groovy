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
package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

import groovy.xml.MarkupBuilder
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Row
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Scope
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.DataSetParser

class NhsDDDataSetElement{

    String name
    String description

    String mandation
    String minMultiplicity
    String maxMultiplicity
    String constraints
    UUID elementId
    Integer webOrder
    boolean retired

    NhsDDElement reuseElement
    NhsDataDictionary dataDictionary

    NhsDDDataSetElement(DataElement dataElement) {
        this.name = dataElement.label
        //this.elementId = dataElement.getSemanticLinks().first().multiFacetAwareItemId
        this.minMultiplicity = dataElement.minMultiplicity
        this.maxMultiplicity = dataElement.maxMultiplicity

    }
    NhsDDDataSetElement(DataElement dataElement, NhsDataDictionary dataDictionary) {
        this(dataElement)

        if(dataDictionary) {
            this.dataDictionary = dataDictionary
            this.reuseElement = dataDictionary.elements[dataElement.label]
        }

        List<Metadata> thisElementMetadata = dataDictionary.dataSetsMetadata[dataElement.id]

        mandation = thisElementMetadata.find { it.key == "MRO" }?.value
        constraints = thisElementMetadata.find { it.key == "Rules" }?.value

        Metadata md = thisElementMetadata.find {it.key == "Web Order"}
        if(md) {
            webOrder = Integer.parseInt(md.value())
        } else {
            webOrder = dataElement.idx
        }
    }

    def createHtmlLink(MarkupBuilder markupBuilder) {
        String link = "#/preview/${dataDictionary.containingVersionedFolder.id.toString()}/element/${elementId.toString()}"
        markupBuilder.a (href: link, class: "element") {
            mkp.yield(name)
        }

    }

    def createLink(MarkupBuilder markupBuilder) {
        String link

        if(isRetired()) {
            link = "dm:${NhsDataDictionary.CORE_MODEL_NAME}|dc:${NhsDataDictionary.DATA_ELEMENTS_CLASS_NAME}|dc:Retired|de:${name}"
        } else {
            link = "dm:${NhsDataDictionary.CORE_MODEL_NAME}|dc:${NhsDataDictionary.DATA_ELEMENTS_CLASS_NAME}|de:${name}"
        }


        markupBuilder.a (href: link) {
            mkp.yield(name)
        }

    }

    Row createDita(NhsDataDictionary dataDictionary) {
        Row.build {
            entry {
                p mandation
            }
            entry {
                if(reuseElement) {
                    p {
                        xRef (outputClass: "element", keyRef: reuseElement.getDitaKey(), scope: Scope.LOCAL)
                    }
                } else {
                    p name
                }
            }
        }
    }

}
