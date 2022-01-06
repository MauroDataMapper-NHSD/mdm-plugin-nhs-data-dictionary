package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.IntegrityCheck

@Slf4j
class NhsDataDictionaryController implements ResourcelessMdmController {

    // For ingest
    static XmlSlurper xmlSlurper = new XmlSlurper()

    NhsDataDictionaryService nhsDataDictionaryService

    @Transactional
    def ingest() {
        if(!params.ingestFile) {
            throw new ApiBadRequestException("NHSDD-01","No ingestFile parameter supplied")
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
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(versionedFolderId)
        //render(view:"statistics", model: [nhsDataDictionary: dataDictionary])
        respond dataDictionary
    }


    def integrityChecks() {

        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)

        List<IntegrityCheck> result = nhsDataDictionaryService.integrityChecks(versionedFolderId)

        respond result, model: [integrityChecks: result]

    }

    def allItemsIndex() {
        Map<CatalogueItem, String> allItemsMap = nhsDataDictionaryService.allItemsIndex(
            UUID.fromString(params.versionedFolderId), params.boolean('includeRetired')?:true)
        render(view: "allItemsIndex", model: [catalogueItemMap: allItemsMap])
    }

    def publishDita() {

        log.debug(params.branchName)

        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)


        File file = nhsDataDictionaryService.publishDita(versionedFolderId)

        render(file: file, fileName: "DataDictionaryDita.zip", contentType: "application/zip")
    }

}
