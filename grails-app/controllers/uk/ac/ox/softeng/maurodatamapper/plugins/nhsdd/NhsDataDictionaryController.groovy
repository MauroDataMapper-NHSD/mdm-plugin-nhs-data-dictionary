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

import groovy.xml.XmlParser
import org.springframework.http.HttpStatus
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishOptions

@Slf4j
class NhsDataDictionaryController implements ResourcelessMdmController {

    // For ingest
    static XmlParser xmlParser = new XmlParser(false, false)

    NhsDataDictionaryService nhsDataDictionaryService
    AsyncJobService asyncJobService

    @Transactional
    def newVersion() {
        log.debug("Creating a new version...")
        User currentUser = getCurrentUser()
        long startTime = System.currentTimeMillis()
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        UUID newVersionedFolderId = nhsDataDictionaryService.newVersion(currentUser, versionedFolderId)
        log.debug(Utils.timeTaken(startTime))
        respond([newVersionedFolderId.toString()])
    }

    @Transactional
    def ingest() {
        if (!params.ingestFile) {
            throw new ApiBadRequestException("NHSDD-01", "No ingestFile parameter supplied")
        }
        def xml = xmlParser.parse(params.ingestFile.getInputStream())
        User currentUser = getCurrentUser()
        PublishOptions publishOptions = PublishOptions.fromParameters(params)

        Closure<VersionedFolder> ingestDD = { String releaseDate,
                                              Boolean finalise,
                                              String folderVersionNo,
                                              String prevVersion,
                                              String branchName,
                                              PublishOptions pubOptions,
                                              Boolean deletePrevious,
                                              UUID jobId ->
            nhsDataDictionaryService.ingest(currentUser, xml, releaseDate, finalise,
                    folderVersionNo, prevVersion, branchName, pubOptions, deletePrevious)
        }


        if(params.boolean('async')) {
            AsyncJob asyncJob = asyncJobService.createAndSaveAsyncJob(
                    "Ingest Data Dictionary ${params.releaseDate}",
                    currentUser.emailAddress,
                    ingestDD.curry(params.releaseDate, params.boolean('finalise'),
                        params.folderVersionNo, params.prevVersion, params.branchName, publishOptions, params.boolean('deletePrevious')?:false))


            return respond(asyncJob, view: '/asyncJob/show', status: HttpStatus.ACCEPTED)

        } else {
            // Doing this synchronously
            VersionedFolder versionedFolder = ingestDD.call(params.releaseDate, params.boolean('finalise'),
                    params.folderVersionNo, params.prevVersion, params.branchName, publishOptions, params.boolean('deletePrevious')?:false, null)
            return respond([versionedFolder: versionedFolder, userSecurityPolicyManager: currentUserSecurityPolicyManager], view: '/versionedFolder/show')
        }

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
        File file = nhsDataDictionaryService.generateChangePaper(versionedFolderId,
                                                                 params.boolean('dataSets')?: false,
                                                                 params.boolean('test')?: false)

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

    def shortDescriptions() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        respond nhsDataDictionaryService.shortDescriptions(versionedFolderId)
    }


    def diff() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        respond(nhsDataDictionaryService.diff(versionedFolderId))

    }

    def iso11179() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        String response = nhsDataDictionaryService.iso11179(versionedFolderId)
        render (text: response, contentType: "text/xml", encoding: "UTF-8")
    }

}