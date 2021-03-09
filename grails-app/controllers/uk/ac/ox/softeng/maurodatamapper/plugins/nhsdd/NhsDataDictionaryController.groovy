package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController

class NhsDataDictionaryController implements ResourcelessMdmController {


    NhsDataDictionaryService nhsDataDictionaryService


    def statistics() {

        String branchName = params.branchName?:'main'

        def result = nhsDataDictionaryService.statistics(branchName)

        respond(result)


    }


}
