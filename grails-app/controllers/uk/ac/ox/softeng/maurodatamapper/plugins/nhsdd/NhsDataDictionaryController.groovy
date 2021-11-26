package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.transactions.Transactional
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.importer.DataDictionaryLoader
import uk.nhs.digital.maurodatamapper.datadictionary.importer.PluginImporter


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
        nhsDataDictionaryService.ingest(currentUser, xml, params.releaseDate, params.finalise, params.folderVersionNo, params.prevVersion)

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

        String branchName = params.branchName?:'main'

        def result = nhsDataDictionaryService.statistics(branchName)

        respond(result)


    }


    def integrityChecks() {

        String branchName = params.branchName?:'main'

        def result = nhsDataDictionaryService.integrityChecks(branchName)

        respond(result)

    }

    def allItemsIndex() {
        respond nhsDataDictionaryService.allItemsIndex()
    }

    def publishDita() {

        System.err.println(params.branchName)

        File file = nhsDataDictionaryService.publishDita(params.branchName)

        render(file: file, fileName: "DataDictionaryDita.zip", contentType: "application/zip")
    }

}
