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
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.TBody
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Align
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.DataSetParser

class NhsDDDataSetClass {

    String name
    String description

    String mandation
    String minMultiplicity
    String maxMultiplicity
    String constraints
    Boolean isChoice
    Integer webOrder

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
        mandation = thisClassMetadata.find { it.key == "MRO" }?.value
        constraints = thisClassMetadata.find { it.key == "Rules" }?.value

        Metadata md = thisClassMetadata.find {it.key == "Web Order"}
        if(md) {
            webOrder = Integer.parseInt(md.value())
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
                TBody body = addAllChildRows(dataDictionary)

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
                        tBody body
                    }
                }
            }
        }

    }

    TBody addAllChildRows(NhsDataDictionary dataDictionary) {
        TBody.build {
            if(dataSetElements && dataSetElements.size() > 0) {
                dataSetElements.each {dataSetElement ->
                    row dataSetElement.createDita(dataDictionary)
                }
            } else {
                dataSetClasses.eachWithIndex { dataSetClass, idx ->
                    if(isChoice && idx != 0) {
                        row {
                            entry (namest: "col1", nameend: "col2") {
                                b 'Or'
                            }
                        }

                    } else if(idx != 0) {
                        row {
                            entry (namest: "col1", nameend: "col2") {
                                p ''
                            }

                        }
                    }
                    //addSubClass((DataClass)childDataClass, tBody, dataDictionary)
                }
            }
        }
    }

}
