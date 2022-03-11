package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper

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
            .folderVersionNo, params.prevVersion)

        respond([newVersionedFolder.id.toString()])

    }

    def changePaper() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        nhsDataDictionaryService.changePaper(versionedFolderId)
        respond([])
    }

    def codeSystemValidateBundle() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        respond nhsDataDictionaryService.codeSystemValidationBundle(versionedFolderId)
    }

    def valueSetValidateBundle() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        respond nhsDataDictionaryService.valueSetValidationBundle(versionedFolderId)
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

    def publishDita() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)

        nhsDataDictionaryService.publishDita(versionedFolderId)

        //        File file = nhsDataDictionaryService.publishDita(versionedFolderId)
        //        render(file: file, fileName: "DataDictionaryDita.zip", contentType: "application/zip")

        respond([])
    }
}