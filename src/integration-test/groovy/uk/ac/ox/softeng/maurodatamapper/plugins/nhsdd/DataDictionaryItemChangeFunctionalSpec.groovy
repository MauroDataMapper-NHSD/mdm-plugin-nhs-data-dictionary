package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Shared

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

@Integration
@Slf4j
class DataDictionaryItemChangeFunctionalSpec extends BaseFunctionalSpec {
    @Shared
    IntegrationTestGivens given

    @Shared
    Folder rootFolder

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        given = new IntegrationTestGivens(messageSource)
        // Setup any domain entities here before flushing the database session...
        loginUser('admin@maurodatamapper.com', 'password')
        rootFolder = given."there is a folder"("test-folder")

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataDictionaryItemChangeFunctionalSpec')
        cleanUpData(rootFolder.id.toString())
    }

    @Override
    String getResourcePath() {
        ''
    }

    void logout() {
        log.trace('Logging out')
        GET('authentication/logout', MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)
        currentCookie = null
    }

    HttpResponse<Map> loginUser(String emailAddress, String userPassword) {
        logout()
        log.trace('Logging in as {}', emailAddress)
        POST('authentication/login', [
            username: emailAddress,
            password: userPassword
        ], MAP_ARG, true)
        verifyResponse(OK, response)
        response
    }

    void "should update all links when an NHS data set constraint label has changed"() {
        given: "there is an initial data dictionary"
        loginUser('admin@maurodatamapper.com', 'password')
        def dataDictionary =  createNhsDataDictionaryStructure()

        when: "the label is modified"
        def item = dataDictionary.dataSetConstraints.terms.first()
        PUT("terminologies/$dataDictionary.dataSetConstraints.id/terms/$item.id", [
            definition: "$item.definition MODIFIED"
        ], MAP_ARG, true)

        then: "the response should be OK"
        verifyResponse(OK, response)

        when: "waiting for the background job to complete"

        then: "the response should be OK"

        and: "the links to the original item have been updated"

        cleanup:
        cleanUpData(dataDictionary.id)
    }

    NhsDataDictionaryModel createNhsDataDictionaryStructure() {
        def dataDictionaryModel = new NhsDataDictionaryModel()

        // -- Versioned Folder --------------------
        POST("folders/$rootFolder.id/versionedFolders", [
            label: dataDictionaryModel.label
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        dataDictionaryModel.id = responseBody().id
        log.info("Created '$dataDictionaryModel.label' versioned folder [$dataDictionaryModel.id]")

        // -- Data Sets --------------------
        // TODO

        // -- Classes and Attributes --------------------
        // TODO

        // -- Data Elements --------------------
        // TODO

        // -- Data Set Constraints --------------------
        dataDictionaryModel.dataSetConstraints.terms = [
            new TermModel("DSC01", "Data Set Constraint 1")
        ]

        createTerminology(dataDictionaryModel.id, dataDictionaryModel.dataSetConstraints)
        createTerms(dataDictionaryModel.dataSetConstraints)

        // -- NHS Business Definitions --------------------
        // TODO

        // -- Supporting Information --------------------
        // TODO

        // Return all the details the test needs
        dataDictionaryModel
    }

    void createTerminology(String dataDictionaryId, TerminologyModel model) {
        POST("folders/$dataDictionaryId/terminologies", [
            label: model.label
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        model.id = responseBody().id
        log.info("Created '$model.label' terminology [$model.id]")
    }

    void createTerms(TerminologyModel model) {
        model.terms.forEach { term ->
            POST("terminologies/$model.id/terms", [
                code: term.code,
                definition: term.definition,
                description: "" // TODO
            ], MAP_ARG, true)
            verifyResponse(CREATED, response)
            term.id = responseBody().id
            log.info("Adding term '$term.code: $term.definition' [$term.id]")
        }
    }
}
