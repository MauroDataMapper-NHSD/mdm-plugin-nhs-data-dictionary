/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish.changePaper

import uk.ac.ox.softeng.maurodatamapper.dita.meta.DitaElement

import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

class Change {
    String changeType
    String stereotype

    NhsDataDictionaryComponent oldItem
    NhsDataDictionaryComponent newItem

    String htmlDetail
    DitaElement ditaDetail

    Change() { }

    String changeText(String stereotype) {
        if(changeType == "New") {
            return "New " + stereotype
        }
        if(changeType == "Retired") {
            return "Changed status to retired"
        }
        if(changeType == "Description") {
            return "Changed description"
        }
        if(changeType == "Data Set Change") {
            return "Changed Data Set"
        }
    }

}
