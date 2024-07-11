package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Shared

import java.util.concurrent.CancellationException
import java.util.concurrent.Future

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * Base specification for any tests that need to verify data dictionary items.
 */
@Slf4j
class BaseDataDictionaryFunctionalSpec extends BaseFunctionalSpec {
    AsyncJobService asyncJobService

    @Shared
    Folder rootFolder

    @Shared
    IntegrationTestGivens given

    @Override
    String getResourcePath() {
        ''
    }

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

    def cleanup() {
        cleanUpAsyncJobs()
    }

    AsyncJob getLastAsyncJob() {
        List<AsyncJob> asyncJobs = asyncJobService.list()
        if (!asyncJobs || asyncJobs.size() == 0) {
            return null
        }

        asyncJobs.last()
    }

    void waitForAsyncJobToComplete(AsyncJob job) {
        if (!job) {
            log.debug("No async job to wait for - continue")
            return
        }

        log.info("Waiting to complete $job.id")
        Future p = asyncJobService.getAsyncJobFuture(job.id)
        try {
            p.get()
        } catch (CancellationException ignored) {
        }
        log.debug("Completed")
    }

    void waitForLastAsyncJobToComplete() {
        // Why do we need to sleep for a period of time to make sure that async jobs exist? This is the only way I could
        // these tests to pass, annoyingly. I think multiple threads are in play between the test, the backend controller and
        // the interceptor, all confusing things
        log.warn("Wait 2 seconds to catchup...")
        Thread.sleep(2000)
        AsyncJob asyncJob = getLastAsyncJob()
        waitForAsyncJobToComplete(asyncJob)
    }

    static String getPathStringWithoutBranchName(Path path, String branchName) {
        path.toString().replace("\$$branchName", "")
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

    void cleanUpFolder(String id) {
        if (!id) {
            return
        }

        DELETE("folders/$id?permanent=true", MAP_ARG, true)
        assert response.status() == NO_CONTENT
        log.info("Deleted folder [$id]")
    }

    void cleanUpVersionedFolder(String id) {
        if (!id) {
            return
        }

        DELETE("folders/$rootFolder.id/versionedFolders/$id?permanent=true", MAP_ARG, true)
        assert response.status() == NO_CONTENT
        log.info("Deleted versioned folder [$id]")
    }

    void cleanUpDataModel(String id) {
        if (!id) {
            return
        }

        DELETE("dataModels/$id?permanent=true", MAP_ARG, true)
        assert response.status() == NO_CONTENT
        log.info("Deleted data model [$id]")
    }

    void cleanUpTerminology(String id) {
        if (!id) {
            return
        }

        DELETE("terminologies/$id?permanent=true", MAP_ARG, true)
        assert response.status() == NO_CONTENT
        log.info("Deleted terminology [$id]")
    }

    void cleanUpAsyncJobs() {
        List<AsyncJob> asyncJobs = asyncJobService.list()
        asyncJobs.each { asyncJobService.delete(it) }
        log.info("Deleted all async jobs")
    }

    ResponseModel "given there is a data dictionary branch"(String label = "Data Dictionary") {
        POST("folders/$rootFolder.id/versionedFolders", [
            label: label
        ], MAP_ARG, true)

        verifyResponse(CREATED, response)
        createResponseModelFromResponse()
    }

    ResponseModel "given there is a folder"(String parentId, String label) {
        POST("folders/$parentId/folders", [
            label: label
        ], MAP_ARG, true)

        verifyResponse(CREATED, response)
        createResponseModelFromResponse()
    }

    ResponseModel "given there is a data model"(String folderId, String label, String description = "") {
        POST("folders/$folderId/dataModels", [
            label: label,
            description: description
        ], MAP_ARG, true)

        verifyResponse(CREATED, response)
        createResponseModelFromResponse()
    }

    ResponseModel "given there is a primitive data type"(String dataModelId, String label) {
        POST("dataModels/$dataModelId/dataTypes", [
            domainType: "PrimitiveType",
            label: label
        ], MAP_ARG, true)

        verifyResponse(CREATED, response)
        createResponseModelFromResponse()
    }

    ResponseModel "given there is a parent data class"(String dataModelId, String label, String description = "") {
        POST("dataModels/$dataModelId/dataClasses", [
            label: label,
            description: description
        ], MAP_ARG, true)

        verifyResponse(CREATED, response)
        createResponseModelFromResponse()
    }

    ResponseModel "given there is a data element"(String dataModelId, String dataClassId, String label, String dataTypeId, String description = "") {
        POST("dataModels/$dataModelId/dataClasses/$dataClassId/dataElements", [
            label: label,
            dataType: dataTypeId,
            description: description
        ], MAP_ARG, true)

        verifyResponse(CREATED, response)
        createResponseModelFromResponse()
    }

    ResponseModel "given there is a terminology"(String folderId, String label) {
        POST("folders/$folderId/terminologies", [
            label: label
        ], MAP_ARG, true)

        verifyResponse(CREATED, response)
        createResponseModelFromResponse()
    }

    ResponseModel "given there is a term"(String terminologyId, String code, String definition, String description = "") {
        POST("terminologies/$terminologyId/terms", [
            code: code,
            definition: definition,
            description: description
        ], MAP_ARG, true)

        verifyResponse(CREATED, response)
        createResponseModelFromResponse()
    }

    private ResponseModel createResponseModelFromResponse() {
        def responseModel = new ResponseModel()
        responseModel.id = responseBody().id
        responseModel.domainType = responseBody().domainType
        responseModel.label = responseBody().label
        responseModel.path = responseBody().path
        log.info("Created $responseModel.domainType '$responseModel.label' [$responseModel.id] - $responseModel.path")
        responseModel
    }
}
