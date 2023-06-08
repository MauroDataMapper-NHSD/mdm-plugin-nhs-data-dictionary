/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.nhs.digital.maurodatamapper.datadictionary.utils

import groovy.transform.Sortable
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent

/**
 * @since 06/01/2022
 */
@Sortable(includes = 'label')
class StereotypedCatalogueItem {
    MdmDomain catalogueItem
    String label
    String stereotype
    Boolean isRetired

    StereotypedCatalogueItem(MdmDomain catalogueItem, String stereotype) {
        this.catalogueItem = catalogueItem
        this.stereotype = stereotype
        this.isRetired = catalogueItem.metadata.find {
            it.namespace == NhsDataDictionary.METADATA_NAMESPACE &&
            it.key == "isRetired" &&
            it.value == "true"
        }
        this.label = catalogueItem.label
    }

    StereotypedCatalogueItem(NhsDataDictionaryComponent component) {
        this.catalogueItem = component.catalogueItem
        this.stereotype = component.stereotypeForPreview
        this.isRetired = component.isRetired()
        this.label = component.getNameWithRetired()
    }

    String getId() {
        catalogueItem.id.toString()
    }

}
