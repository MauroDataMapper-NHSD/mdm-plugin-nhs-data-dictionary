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

class Topic extends TopLevelDitaElement {

    String doctypeDecl = """<!DOCTYPE topic PUBLIC "-//OASIS//DTD DITA Topic//EN" "topic.dtd">"""


    String id
    String title
    String titleClass = null
    String shortDesc
    String abstr

    String searchTitle
    String navTitle


    //List<RelatedLink> relatedLinks

    Prolog prolog
    Body body = new Body()

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.topic(id: id) {
            title(outputclass: titleClass, title)
            if(searchTitle || navTitle) {
                titlealts {
                    if(navTitle) {
                        navtitle navTitle
                    }
                    if(searchTitle) {
                        searchtitle searchTitle
                    }

                }
            }
            if(shortDesc) {
                shortdesc shortDesc
            }
            if(abstr) {
                "abstract" abstr
            }
            if(prolog) {
                prolog.writeAsXml(builder)
            }
            if(body) {
                body.writeAsXml(builder)
            }


        }
    }




}
