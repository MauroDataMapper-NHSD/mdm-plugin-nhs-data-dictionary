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

import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.MergeDiff
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish.PublishOptions
import groovy.xml.XmlParser

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @since 17/02/2022
 */
@Transactional
@Slf4j
class TestingService {

    NhsDataDictionaryService nhsDataDictionaryService
    VersionedFolderService versionedFolderService
    FolderService folderService

    @Autowired
    SessionFactory sessionFactory

    Map<String, String> buildTestData(User user) {
        // Ingest, finalise september
        UUID releaseId = cleanIngest(user, 'september2021.xml', 'September 2021', 'main')
        // Ingest november
        UUID novBranchId = cleanIngest(user, 'november2021.xml', 'November 2021', 'main', false)
        // Change the folder name and link to the sept release
        VersionedFolder release = versionedFolderService.get(releaseId)
        VersionedFolder novBranch = versionedFolderService.get(novBranchId)
        novBranch.label = 'NHS Data Dictionary (September 2021)'
        versionedFolderService.setFolderIsNewBranchModelVersionOfFolder(novBranch, release, UnloggedUser.instance)
        versionedFolderService.save(novBranch, validate: false, flush: true)
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        [releaseId  : releaseId.toString(),
         novBranchId: novBranchId.toString()]
    }

    Map<String, String> branchTestData(User user, String releaseId) {
        // Create a september branch
        VersionedFolder release = versionedFolderService.get(releaseId)
        UUID septBranchId = createBranch(user, release, 'september_2021')
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        [releaseId   : releaseId.toString(),
         septBranchId: septBranchId.toString()]

    }

    UUID cleanIngest(User user, String name, String releaseDate, String branchName, boolean finalised = true, boolean deletePrevious = false) {
        log.info('---------- Ingesting {} ----------', name)
        def xml = loadXml(name)
        assert xml
        VersionedFolder dd = nhsDataDictionaryService.ingest(user, xml, releaseDate, finalised, null, null, branchName, new PublishOptions(), deletePrevious)
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        log.info('---------- Finished Ingesting {} ----------', name)
        dd.id
    }

    def loadXml(String filename) {
        Path resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources')
        Path testFilePath = resourcesPath.resolve(filename).toAbsolutePath()
        assert Files.exists(testFilePath)
        def xml = new XmlParser().parse(Files.newBufferedReader(testFilePath))
        xml
    }


    UUID createBranch(User user, VersionedFolder release, String branchName) {
        log.info('---------- Starting {} branch ----------', branchName)
        VersionedFolder branch = versionedFolderService.createNewBranchModelVersion(branchName,
                                                                                    release, user, true,
                                                                                    PublicAccessSecurityPolicyManager.instance as UserSecurityPolicyManager)
        VersionedFolder validated = folderService.validate(branch) as VersionedFolder
        assert !validated.hasErrors()
        VersionedFolder saved = versionedFolderService.save(validated, validate: false, flush: true)
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        log.info('---------- Finished {} branch ----------', branchName)
        saved.id
    }


    def diff(UUID versionedFolderId) {
        VersionedFolder thisDictionary = versionedFolderService.get(versionedFolderId)

        VersionedFolder previousVersion = versionedFolderService.getFinalisedParent(thisDictionary)

        // Load the things into memory
        //NhsDataDictionary thisDataDictionary = buildDataDictionary(versionedFolderId)
        //NhsDataDictionary previousDataDictionary = buildDataDictionary(versionedFolderId)
        //ObjectDiff objectDiff = versionedFolderService.getDiffForVersionedFolders(thisDictionary, previousVersion)

        log.info('---------- Starting merge diff ----------')
        long start = System.currentTimeMillis()
        MergeDiff<VersionedFolder> objectDiff = versionedFolderService.getMergeDiffForVersionedFolders(thisDictionary, previousVersion)
        log.info('Merge Diff took {}', Utils.timeTaken(start))

        return objectDiff
    }
}
