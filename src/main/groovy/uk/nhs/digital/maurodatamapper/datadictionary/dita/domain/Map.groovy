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
package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.xml.MarkupBuilder

class Map extends TopLevelDitaElement {

    String title
    String chunk
    TopicMeta topicMeta
    List<Ref> refs = []

    String doctypeDecl = """<!DOCTYPE map PUBLIC \"-//OASIS//DTD DITA Map//EN\" \"map.dtd\">"""

    def writeAsXml(MarkupBuilder builder) {
        builder.map(chunk: chunk) {
            if(title) {
                title title
            }
            if(topicMeta) {
                topicMeta.writeAsXml(builder)
            }
            refs.each { ref ->
                ref.writeAsXml(builder)
            }
        }

    }


}
