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
package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware

import grails.artefact.Controller
import grails.artefact.controller.RestResponder
import uk.nhs.digital.maurodatamapper.datadictionary.utils.StereotypedCatalogueItem

abstract class DataDictionaryComponentController<T extends MultiFacetAware> implements Controller, RestResponder {
	static responseFormats = ['json', 'xml']

    DataSetService dataSetService
    ElementService elementService
    ClassService classService
    AttributeService attributeService
    BusinessDefinitionService businessDefinitionService
    SupportingInformationService supportingInformationService
    DataSetConstraintService dataSetConstraintService
    DataSetFolderService dataSetFolderService

    NhsDataDictionaryService nhsDataDictionaryService

    abstract DataDictionaryComponentService getService()

    abstract String getParameterIdKey()


    def index() {
        respond getService().index(UUID.fromString(params.versionedFolderId), params.boolean('includeRetired')?:false)
    }

    def show() {
        respond getService().show(UUID.fromString(params.versionedFolderId), params.id)
    }

    def whereUsed() {
        List<StereotypedCatalogueItem> whereUsed =  getService().getWhereUsed(UUID.fromString(params.versionedFolderId), params[getParameterIdKey()])
        respond whereUsed
    }

}
