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
package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDDataSetFolder
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary

@Slf4j
@Transactional
class DataSetFolderService extends DataDictionaryComponentService<Folder, NhsDDDataSetFolder> {

    @Override
    NhsDDDataSetFolder show(UUID versionedFolderId, String id) {
        Folder folderFolder = folderService.get(id)
        NhsDDDataSetFolder dataSetFolder = getNhsDataDictionaryComponentFromCatalogueItem(folderFolder, null)
        dataSetFolder.definition = convertLinksInDescription(versionedFolderId, dataSetFolder.getDescription())
        return dataSetFolder
    }

    @Override
    Set<Folder> getAll(UUID versionedFolderId, boolean includeRetired = false) {
        Folder dataSetsFolder = nhsDataDictionaryService.getDataSetsFolder(versionedFolderId)

        List<Folder> allFolders = getAllFolders(dataSetsFolder, includeRetired)

        allFolders.findAll {folder ->
            folder.label != "Retired" && (
                includeRetired || !containerIsRetired(folder))
        }
    }

    Set<Folder> getAllFolders(Folder dataSetsFolder, boolean includeRetired = false) {
        Set<Folder> returnFolders = []
        returnFolders.addAll(folderService.findAllByParentId(dataSetsFolder.id))
        dataSetsFolder.childFolders.each {childFolder ->
            returnFolders.addAll(getAllFolders(childFolder, includeRetired))
        }
        return returnFolders
    }


    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".data set folder"
    }

    @Override
    NhsDDDataSetFolder getNhsDataDictionaryComponentFromCatalogueItem(Folder catalogueItem, NhsDataDictionary dataDictionary) {
        NhsDDDataSetFolder folder = new NhsDDDataSetFolder()
        nhsDataDictionaryComponentFromItem(catalogueItem, folder)
        folder.dataDictionary = dataDictionary
        return folder
    }

    void persistDataSetFolders(NhsDataDictionary dataDictionary,
                        VersionedFolder dictionaryFolder, DataModel coreDataModel, String currentUserEmailAddress) {

        Folder dataSetsFolder = new Folder(label: "Data Sets", createdBy: currentUserEmailAddress)
        dictionaryFolder.addToChildFolders(dataSetsFolder)
        if (!folderService.validate(dataSetsFolder)) {
            throw new ApiInvalidModelException('NHSDD', 'Invalid model', dataSetsFolder.errors)
        }
        folderService.save(dataSetsFolder)


        dataDictionary.dataSetFolders.each {name, dataSetFolder ->
            System.err.println("Persisting: ${dataSetFolder.name}")
            List<String> folderPath = []
            folderPath.addAll(dataSetFolder.folderPath)
            folderPath.add(name)
            Folder newFolder = getFolderAtPath(dataSetsFolder, folderPath, currentUserEmailAddress)
            if(dataSetFolder.definition) {
                newFolder.description = dataSetFolder.definition
            }
            addMetadataFromComponent(newFolder, dataSetFolder, currentUserEmailAddress)

            if (!folderService.validate(newFolder)) {
                throw new ApiInvalidModelException('NHSDD', 'Invalid model', newFolder.errors)
            }
            folderService.save(newFolder)
        }
    }



    NhsDDDataSetFolder getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        nhsDataDictionary.dataSetFolders.values().find {
            it.catalogueItem.id == catalogueItemId
        }
    }

}