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

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.DataSetParser

class NhsDDDataSetClass {

    String name
    String description

    String mandation
    String minMultiplicity
    String maxMultiplicity
    String constraints
    Boolean isChoice

    List<NhsDDDataSetClass> dataSetClasses = []
    List<NhsDDDataSetElement> dataSetElements = []

    NhsDataDictionary dataDictionary

    NhsDDDataSetClass(DataClass dataClass, NhsDataDictionary dataDictionary) {
        this.name = dataClass.label
        this.description = dataClass.description
        isChoice = DataSetParser.isChoice(dataClass)
        mandation = DataSetParser.getMRO(dataClass)
        constraints = DataSetParser.getRules(dataClass)

        this.dataDictionary = dataDictionary

        dataClass.dataClasses.sort {childDataClass ->
            DataSetParser.getOrder(childDataClass)
        }.each {childDataClass ->
            dataSetClasses.add(new NhsDDDataSetClass(childDataClass, dataDictionary))
        }
        dataClass.getImportedDataElements().each {dataElement ->
            dataSetElements.add(new NhsDDDataSetElement(dataElement, dataDictionary))

        }

    }

    Set<NhsDDDataSetElement> getAllElements() {
        Set<NhsDDDataSetElement> returnElements = [] as Set<NhsDDDataSetElement>
        dataSetClasses.each {dataSetClass ->
            returnElements.addAll(dataSetClass.getAllElements())
        }
        returnElements.addAll(dataSetElements)
        return returnElements
    }


}
