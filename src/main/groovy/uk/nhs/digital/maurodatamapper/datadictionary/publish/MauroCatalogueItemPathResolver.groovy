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
package uk.nhs.digital.maurodatamapper.datadictionary.publish

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary

class MauroCatalogueItemPathResolver implements PathResolver<UUID> {
    final VersionedFolder versionedFolder

    final DataModelService dataModelService
    final DataClassService dataClassService
    final DataElementService dataElementService
    final TerminologyService terminologyService

    MauroCatalogueItemPathResolver(
        VersionedFolder versionedFolder,
        DataModelService dataModelService,
        DataClassService dataClassService,
        DataElementService dataElementService,
        TerminologyService terminologyService) {
        this.versionedFolder = versionedFolder
        this.dataModelService = dataModelService
        this.dataClassService = dataClassService
        this.dataElementService = dataElementService
        this.terminologyService = terminologyService
    }

    @Override
    UUID get(String path) {
        String[] pathParts = path.split("\\|")
        CatalogueItem catalogueItem = getByPath(versionedFolder, pathParts)
        if (!catalogueItem) {
            return null
        }

        catalogueItem.id
    }

    private CatalogueItem getByPath(VersionedFolder versionedFolder, String[] path) {
        if (path[0] == "te:${NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME}") {
            Terminology terminology = terminologyService.findByFolderIdAndLabel(versionedFolder.id, NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME)
            String termLabel = path[1].replace("tm:", "")
            Term t = terminology.findTermByCode(termLabel)
            if (t) {
                return t
            }
        }
        if (path[0] == "te:${NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}") {
            Terminology terminology = terminologyService.findByFolderIdAndLabel(versionedFolder.id, NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME)
            String termLabel = path[1].replace("tm:", "")
            Term t = terminology.findTermByCode(termLabel)
            if (t) {
                return t
            }
        }
        if (path[0] == "te:${NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME}") {
            Terminology terminology = terminologyService.findByFolderIdAndLabel(versionedFolder.id, NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME)
            String termLabel = path[1].replace("tm:", "")
            Term t = terminology.findTermByCode(termLabel)
            if (t) {
                return t
            }
        }
        if (path[0] == "dm:${NhsDataDictionary.CLASSES_MODEL_NAME}".toString()) {
            DataModel dm = dataModelService.findByFolderIdAndLabel(versionedFolder.id, NhsDataDictionary.CLASSES_MODEL_NAME)
            if (path[1] == "dc:Retired") {
                DataClass dc1 = dataClassService.findByDataModelIdAndLabel(dm.id, "Retired")
                DataClass dc2 = dataClassService.findByParentAndLabel(dc1, path[2].replace("dc:", ""))
                return dc2
            } else {
                DataClass dc1 = dataClassService.findByDataModelIdAndLabel(dm.id, path[1].replace("dc:", ""))
                return dc1
            }
        } else if (path[0] == "dm:${NhsDataDictionary.ELEMENTS_MODEL_NAME}") {
            DataModel dm = dataModelService.findByFolderIdAndLabel(versionedFolder.id, NhsDataDictionary.ELEMENTS_MODEL_NAME)
            if (path[1] == "dc:Retired") {
                DataClass dc1 = dataClassService.findByDataModelIdAndLabel(dm.id, "Retired")
                DataElement de = dataElementService.findByParentAndLabel(dc1, path[2].replace("de:", ""))
                return de
            } else {
                DataClass dc = dataClassService.findByDataModelIdAndLabel(dm.id, path[1].replace("dc:", ""))
                DataElement de = dataElementService.findByParentAndLabel(dc, path[2].replace("de:", ""))
                return de
            }
        }

        if (path.length == 1 && path[0].startsWith("dm:")) {
            DataModel dm = dataModelService.findByLabel(path[0].replace("dm:", ""))
            return dm
        }

        return null
    }
}
