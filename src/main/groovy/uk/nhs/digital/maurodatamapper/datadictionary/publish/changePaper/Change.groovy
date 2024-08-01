/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper

import uk.ac.ox.softeng.maurodatamapper.dita.meta.DitaElement

import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent

class Change {
    static final String NEW_TYPE = "New"
    static final String RETIRED_TYPE = "Retired"
    static final String UPDATED_DESCRIPTION_TYPE = "Updated description"
    static final String CHANGED_ATTRIBUTES_TYPE = "Changed attributes"

    String changeType
    String stereotype

    NhsDataDictionaryComponent oldItem
    NhsDataDictionaryComponent newItem

    String htmlDetail
    DitaElement ditaDetail

    Change() { }

    String changeText(String stereotype) {
        if(changeType == NEW_TYPE) {
            return "New " + stereotype
        }
        if(changeType == RETIRED_TYPE) {
            return "Changed status to retired"
        }
        if(changeType == "Description") {
            return "Changed description"
        }
        if(changeType == "Unchanged Item") {
            return "Unchanged item"
        }
    }

}
