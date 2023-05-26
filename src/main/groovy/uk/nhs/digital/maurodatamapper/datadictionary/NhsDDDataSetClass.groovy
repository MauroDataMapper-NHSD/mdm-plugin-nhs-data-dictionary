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
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.P
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Row
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.TBody
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.XRef
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Align

class NhsDDDataSetClass {

    String name
    String description

    String mandation
    String minMultiplicity
    String maxMultiplicity
    String constraints
    Boolean isChoice
    Boolean isAnd
    Boolean isInclusiveOr
    Integer webOrder
    Boolean isAddress

    List<NhsDDDataSetClass> dataSetClasses = []
    List<NhsDDDataSetElement> dataSetElements = []

    NhsDataDictionary dataDictionary

    NhsDDDataSetClass(DataClass dataClass) {
        this.name = dataClass.label
        this.description = dataClass.description

    }


    NhsDDDataSetClass(DataClass dataClass, NhsDataDictionary dataDictionary) {
        this(dataClass)
        this.dataDictionary = dataDictionary

        List<Metadata> thisClassMetadata = dataDictionary.dataSetsMetadata[dataClass.id]

        isChoice = thisClassMetadata.find {
            (it.namespace == NhsDataDictionary.METADATA_NAMESPACE
                    && it.key == "Choice"
                    && it.value == "true")
        }
        isAnd = thisClassMetadata.find {
            (it.namespace == NhsDataDictionary.METADATA_NAMESPACE
                    && it.key == "And"
                    && it.value == "true")
        }
        isInclusiveOr = thisClassMetadata.find {
            (it.namespace == NhsDataDictionary.METADATA_NAMESPACE
                    && it.key == "InclusiveOr"
                    && it.value == "true")
        }
        isAddress = thisClassMetadata.find {
            (it.namespace == NhsDataDictionary.METADATA_NAMESPACE
                    && it.key == "Address Choice"
                    && it.value == "true")
        }
        mandation = thisClassMetadata.find { it.key == "MRO" }?.value
        constraints = thisClassMetadata.find { it.key == "Rules" }?.value

        Metadata md = thisClassMetadata.find {it.key == "Web Order"}
        if(md) {
            webOrder = Integer.parseInt(md.value)
        } else {
            webOrder = dataClass.idx
        }

        dataClass.dataClasses.each {childDataClass ->
            dataSetClasses.add(new NhsDDDataSetClass(childDataClass, dataDictionary))
        }
        dataSetClasses = dataSetClasses.sort { it.webOrder }
        /*
        dataClass.getImportedDataElements().each {dataElement ->
            dataSetElements.add(new NhsDDDataSetElement(dataElement, dataDictionary))
        }
         */
        dataClass.getDataElements().each { dataElement ->
            dataSetElements.add(new NhsDDDataSetElement(dataElement, dataDictionary))
        }

        dataSetElements = dataSetElements.sort {it.webOrder}

    }

    List<Object> getSortedChildren() {
        List<Object> children = []
        children.addAll(dataSetClasses)
        children.addAll(dataSetElements)
        children = children.sort { it.webOrder }
        return children
    }

    Set<NhsDDDataSetElement> getAllElements() {
        Set<NhsDDDataSetElement> returnElements = [] as Set<NhsDDDataSetElement>
        dataSetClasses.each {dataSetClass ->
            returnElements.addAll(dataSetClass.getAllElements())
        }
        returnElements.addAll(dataSetElements)
        return returnElements
    }

    Div outputClassAsDita(NhsDataDictionary dataDictionary) {
        Div.build {
            if (isChoice && name.startsWith("Choice")) {
                b "One of the following options must be used:"
                dataSetClasses.eachWithIndex { NhsDDDataSetClass childDataClass, int i ->
                    if (idx != 0) {
                        b "Or"
                    }
                    div childDataClass.outputClassAsDita(dataDictionary)
                }
            } else {
                table(outputClass: "table table-striped table-bordered table-sm") {
                    tgroup(cols: 2) {
                        colspec(colnum: 1, colName: "col1", colwidth: "2*", align: Align.CENTER)
                        colspec(colnum: 2, colName: "col2", colwidth: "8*")
                        tHead {
                            row(outputClass: "thead-light") {
                                entry(namest: "col1", nameend: "col2") {
                                    b name
                                    if (description) {
                                        p description
                                    }
                                }
                            }
                            if (dataSetElements && dataSetElements.size() > 0) {
                                row {
                                    entry {
                                        p "Mandation"
                                    }
                                    entry {
                                        p "Data Elements"
                                    }
                                }
                            }

                        }
                        tBody {
                            addAllChildRows().each {childRow ->
                                row childRow
                            }
                        }
                    }
                }
            }
        }

    }

    List<Row> addAllChildRows() {
        List<Row> rows = []
        if(dataSetElements && dataSetElements.size() > 0) {
            getSortedChildren().each {classOrElement ->
                rows.add(addChildRow(classOrElement))
            }
        } else {
            dataSetClasses.eachWithIndex { dataSetClass, idx ->
                if(isChoice && idx != 0) {
                    rows.add(Row.build {
                        entry (namest: "col1", nameend: "col2") {
                            b 'Or'
                        }
                    })

                } else if(idx != 0) {
                    rows.add(Row.build {
                        entry (namest: "col1", nameend: "col2") {
                            p ''
                        }

                    })
                }
                rows.addAll(dataSetClass.addAsSubClass())
            }
        }
        return rows
    }

    static Row addChildRow(def classOrElement) {
        String mro = classOrElement.mandation
        Row.build {
            entry {
                p mro
            }
            entry {
                getXRefStringFromItem(classOrElement).each {paragraph ->
                    p paragraph
                }
            }
        }
    }

    static List<P> getXRefStringFromItem(def classOrElement) {
        if (classOrElement instanceof NhsDDDataSetElement) {
            return ((NhsDDDataSetElement) classOrElement).createEntryParagraphs()
        } else { //if (catalogueItem instanceof NhsDDDataSetClass)
            return ((NhsDDDataSetClass) classOrElement).createEntryParagraphs()
        }
    }

    List<P> createEntryParagraphs() {
        if(isAddress) { // See COSD Pathology for an example of this
            // We store "Address 1" and "Address 2" as values in the metadata, but given that all occurrences of this
            // are currently the same, we'll hard-code the links in for now

            List<P> paragraphs = dataSetElements[0].createEntryParagraphs()
            XRef firstLink = paragraphs[0].getXRefs()[0]
            paragraphs.add(1, P.build {text "Or"})
            paragraphs[0].text " - "
            paragraphs[0].xRef XRef.build(outputClass: "class", keyRef: "class_address_structured")
            paragraphs.add(P.build {
                xRef firstLink
                text " - "
                xRef XRef.build(outputClass: "class", keyRef: "class_address_unstructured")
            })
            return paragraphs
        } else if (isChoice && name.startsWith("Choice")) {
            List<P> paragraphs = []
            getSortedChildren().eachWithIndex { childItem, idx ->
                if (idx != 0) {
                    paragraphs.add(P.build { text "Or"})
                }
                paragraphs.addAll(childItem.createEntryParagraphs())
            }
            return paragraphs
        } else if (isAnd && (name.startsWith("Choice") || name.startsWith("And"))) {
            List<P> paragraphs = []
            getSortedChildren().eachWithIndex { childItem, idx ->
                if (idx != 0) {
                    paragraphs.add(P.build { text "And"})
                }
                paragraphs.addAll(childItem.createEntryParagraphs())
            }
            return paragraphs
        } else if (isInclusiveOr && name.startsWith("Choice")) {
            List<P> paragraphs = []
            getSortedChildren().eachWithIndex { childItem, idx ->
                if (idx != 0) {
                    paragraphs.add(P.build { text "And/Or"})
                }
                paragraphs.addAll(childItem.createEntryParagraphs())
            }
            return paragraphs
        }
        return []
    }

    List<Row> addAsSubClass() {
        List<Row> rows = []
        if(name != "Choice") {
            rows.add(Row.build(outputClass: "table-primary") {
                entry(namest: "col1", nameend: "col1") {
                    p {
                        b "Mandation"
                    }
                }
                entry(namest: "col2", nameend: "col2") {
                    b name
                    if(description) {
                        p description
                    }
                }
            })
        }
        rows.addAll(addAllChildRows())
        return rows
    }
}
