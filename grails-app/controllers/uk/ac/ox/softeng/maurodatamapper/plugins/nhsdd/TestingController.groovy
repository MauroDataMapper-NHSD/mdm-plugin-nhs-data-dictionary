package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

/**
 * @since 17/02/2022
 */
@Slf4j
@Transactional
class TestingController implements ResourcelessMdmController {
    TestingService testingService


    def buildTestData() {
        def ids = testingService.buildTestData(currentUser)
        respond(ids)
    }

    def branchTestData() {
        def ids = testingService.branchTestData(currentUser, params.releaseId)
        respond(ids)
    }

}
