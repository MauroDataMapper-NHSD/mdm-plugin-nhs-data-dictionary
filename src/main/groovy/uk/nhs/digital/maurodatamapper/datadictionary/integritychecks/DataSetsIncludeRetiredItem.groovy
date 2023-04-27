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
package uk.nhs.digital.maurodatamapper.datadictionary.integritychecks

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel

import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDElement
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent

class DataSetsIncludeRetiredItem implements IntegrityCheck {

    String name = "Datasets that include retired items"

    String description = "Check that a dataset doesn't include any retired data elements in its definition"

    @Override
    List<NhsDataDictionaryComponent> runCheck(NhsDataDictionary dataDictionary) {

        //List<NhsDataDictionaryComponent> prepItems = dataDictionary.elements.values().findAll {it.isPreparatory()}

        errors = dataDictionary.dataSets.values().findAll {component ->
            ((DataModel)component.catalogueItem).allDataElements.find {dataElement ->
                NhsDDElement foundElement = dataDictionary.elementsByCatalogueId[dataElement.id]
                if(foundElement) {
                    return foundElement.isRetired()
                } else {
                    // This must be one of those 'preview' elements, and so we'll assume it's not retired
                    return false
                }
            }
        }
        return errors
    }

}
