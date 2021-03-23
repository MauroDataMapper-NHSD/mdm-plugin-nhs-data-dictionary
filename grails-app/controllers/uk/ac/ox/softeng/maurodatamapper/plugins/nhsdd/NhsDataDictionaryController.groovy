package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController

import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.importer.DataDictionaryLoader
import uk.nhs.digital.maurodatamapper.datadictionary.importer.PluginImporter

class NhsDataDictionaryController implements ResourcelessMdmController {


    NhsDataDictionaryService nhsDataDictionaryService


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


}
