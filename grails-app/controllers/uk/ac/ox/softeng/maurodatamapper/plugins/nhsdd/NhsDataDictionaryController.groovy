package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

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
        nhsDataDictionaryService.ingest(currentUser, xml, params.releaseDate, params.boolean('finalise'), params.folderVersionNo, params.prevVersion)

        respond([])

    }

    def changePaper() {
        User currentUser = getCurrentUser()
        nhsDataDictionaryService.changePaper(currentUser, params.sourceId, params.targetId)
        respond([])
    }

    def codeSystemValidateBundle() {
        User currentUser = getCurrentUser()
        respond nhsDataDictionaryService.codeSystemValidationBundle(currentUser, params.releaseDate)
    }

    def valueSetValidateBundle() {
        User currentUser = getCurrentUser()
        respond nhsDataDictionaryService.valueSetValidationBundle(currentUser, params.releaseDate)
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
        respond nhsDataDictionaryService.allItemsIndex(
            UUID.fromString(params.versionedFolderId), params.boolean('includeRetired') ?: true)
    }

    def publishDita() {

        log.debug(params.branchName)

        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)


        File file = nhsDataDictionaryService.publishDita(versionedFolderId)

        render(file: file, fileName: "DataDictionaryDita.zip", contentType: "application/zip")
    }

}
