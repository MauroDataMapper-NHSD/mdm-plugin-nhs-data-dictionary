package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController

import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.importer.DataDictionaryLoader
import uk.nhs.digital.maurodatamapper.datadictionary.importer.PluginImporter

class NhsDataDictionaryController implements ResourcelessMdmController {


    NhsDataDictionaryService nhsDataDictionaryService

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
