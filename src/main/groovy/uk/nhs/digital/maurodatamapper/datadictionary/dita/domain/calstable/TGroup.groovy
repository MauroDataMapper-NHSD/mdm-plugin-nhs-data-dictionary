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
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.DitaElement

class TGroup extends DitaElement {

    int cols
    List<ColSpec> colSpecs = []
    THead tHead
    TBody tBody = new TBody()

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.tgroup (outputclass: outputClass, cols: "" + cols){
            colSpecs.each { colSpec ->
                colSpec.writeAsXml(builder)
            }
            if(tHead) {
                tHead.writeAsXml(builder)
            }
            tBody.writeAsXml(builder)
        }
    }
}
