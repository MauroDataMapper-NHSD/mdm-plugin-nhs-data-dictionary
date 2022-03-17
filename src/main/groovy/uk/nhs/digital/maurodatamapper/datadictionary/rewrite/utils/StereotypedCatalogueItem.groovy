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
package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.utils

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import groovy.transform.Sortable
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

/**
 * @since 06/01/2022
 */
@Sortable(includes = 'label')
class StereotypedCatalogueItem {
    CatalogueItem catalogueItem
    String label
    String stereotype
    Boolean isRetired

    StereotypedCatalogueItem(CatalogueItem catalogueItem, String stereotype) {
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
