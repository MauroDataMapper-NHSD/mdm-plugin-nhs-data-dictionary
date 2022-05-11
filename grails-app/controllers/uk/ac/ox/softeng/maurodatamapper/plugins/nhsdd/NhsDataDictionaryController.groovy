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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish.PublishOptions

@Slf4j
class NhsDataDictionaryController implements ResourcelessMdmController {

    // For ingest
    static XmlSlurper xmlSlurper = new XmlSlurper()

    NhsDataDictionaryService nhsDataDictionaryService

    @Transactional
    def ingest() {
        if (!params.ingestFile) {
            throw new ApiBadRequestException("NHSDD-01", "No ingestFile parameter supplied")
        }
        def xml = xmlSlurper.parse(params.ingestFile.getInputStream())
        User currentUser = getCurrentUser()
        VersionedFolder newVersionedFolder = nhsDataDictionaryService.ingest(currentUser, xml, params.releaseDate, params.boolean('finalise'), params
            .folderVersionNo, params.prevVersion, params.branchName)

        respond([newVersionedFolder.id.toString()])

    }

    def previewChangePaper() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        respond nhsDataDictionaryService.previewChangePaper(versionedFolderId)
    }

    def branches() {
        respond nhsDataDictionaryService.branches(currentUserSecurityPolicyManager)
    }

    def statistics() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        respond nhsDataDictionaryService.buildDataDictionary(versionedFolderId)
    }


    def integrityChecks() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        respond integrityChecks: nhsDataDictionaryService.integrityChecks(versionedFolderId)
    }

    def allItemsIndex() {
        respond nhsDataDictionaryService.allItemsIndex(UUID.fromString(params.versionedFolderId))
    }


    // Publication endpoints - generate documents

    def generateWebsite() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)

        PublishOptions publishOptions = PublishOptions.fromParameters(params)
        File file = nhsDataDictionaryService.generateWebsite(versionedFolderId, publishOptions)
        header 'Access-Control-Expose-Headers', 'Content-Disposition'
        render(file: file, fileName: file.name, contentType: "application/zip")
    }


    def generateChangePaper() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        File file = nhsDataDictionaryService.generateChangePaper(versionedFolderId, params.boolean('test')?: false)

        header 'Access-Control-Expose-Headers', 'Content-Disposition'
        render(file: file, fileName: file.name, contentType: "application/zip")
    }

    def codeSystemValidateBundle() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        respond nhsDataDictionaryService.codeSystemValidationBundle(versionedFolderId)
    }

    def valueSetValidateBundle() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        respond nhsDataDictionaryService.valueSetValidationBundle(versionedFolderId)
    }


}