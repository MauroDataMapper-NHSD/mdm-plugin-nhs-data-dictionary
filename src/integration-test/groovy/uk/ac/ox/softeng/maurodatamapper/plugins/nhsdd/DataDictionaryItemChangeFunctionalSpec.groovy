package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Shared

import java.util.concurrent.CancellationException
import java.util.concurrent.Future

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

@Integration
@Slf4j
class DataDictionaryItemChangeFunctionalSpec extends BaseFunctionalSpec {
    AsyncJobService asyncJobService

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
        loginUser('admin@maurodatamapper.com', 'password')
        cleanUpFolder(rootFolder.id.toString())
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

    AsyncJob getAsyncJob() {
        List<AsyncJob> asyncJobs = asyncJobService.list()
        if (!asyncJobs || asyncJobs.size() == 0) {
            return null
        }

        asyncJobs.first()
    }

    void waitForAsyncJobToComplete(AsyncJob job) {
        log.info("Waiting to complete $job.id")
        Future p = asyncJobService.getAsyncJobFuture(job.id)
        try {
            p.get()
        } catch (CancellationException ignored) {
        }
        log.debug("Completed")
    }

    void "NHS classes: should update nothing when item is updated but not inside an NHS data dictionary branch"() {
        given: "there is a data model not in an NHS data dictionary"
        loginUser('admin@maurodatamapper.com', 'password')

        def dataModel = new DataModelModel("Test Data Model")
        dataModel.classes = [
            new DataClassModel("Test Data Class", "", [new DataElementModel("Test Data Element", "")])
        ]

        createDataModel(rootFolder.id.toString(), dataModel)
        createDataClasses(dataModel)

        when: "the label is modified"
        def item = dataModel.classes.first()
        PUT("dataModels/$dataModel.id/dataClasses/$item.id", [
            label: "$item.label MODIFIED"
        ], MAP_ARG, true)

        then: "the response should be OK"
        verifyResponse(OK, response)

        and: "there were no background jobs started"
        AsyncJob asyncJob = getAsyncJob()
        verifyAll {
            asyncJob == null
        }
    }

    void "NHS classes: should update nothing when item is updated but HTTP response status was not OK"() {
        given: "there is an initial data dictionary"
        loginUser('admin@maurodatamapper.com', 'password')
        def dataDictionary =  createNhsDataDictionaryStructure()

        when: "the item is modified"
        def item = dataDictionary.classesAndAttributes.classes.find { it.label == "APPOINTMENT" }
        def otherItem = dataDictionary.classesAndAttributes.classes.find { it.label == "CLINICAL TRIAL" }
        PUT("dataModels/$dataDictionary.classesAndAttributes.id/dataClasses/$item.id", [
            label: otherItem.label  // Cannot be the same label as another item
        ], MAP_ARG, true)

        then: "the response should not be OK"
        verifyResponse(UNPROCESSABLE_ENTITY, response)

        and: "there were no background jobs started"
        AsyncJob asyncJob = getAsyncJob()
        verifyAll {
            asyncJob == null
        }

        and: "the links to the original item have not been updated"
        // TODO

        cleanup:
        cleanUpVersionedFolder(dataDictionary.id)
    }

    void "NHS classes: should update nothing when item has changed but not the label"() {
        given: "there is an initial data dictionary"
        loginUser('admin@maurodatamapper.com', 'password')
        def dataDictionary =  createNhsDataDictionaryStructure()

        when: "the item is modified but not the label"
        def item = dataDictionary.classesAndAttributes.classes.find { it.label == "APPOINTMENT" }
        PUT("dataModels/$dataDictionary.classesAndAttributes.id/dataClasses/$item.id", [
            aliases: ["test"]
        ], MAP_ARG, true)

        then: "the response should be OK"
        verifyResponse(OK, response)

        and: "the response contains the expected changes"
        verifyAll(responseBody()) {
            aliases == ["test"]
        }

        and: "there were no background jobs started"
        AsyncJob asyncJob = getAsyncJob()
        verifyAll {
            asyncJob == null
        }

        and: "the links to the original item have not been updated"
        // TODO

        cleanup:
        cleanUpVersionedFolder(dataDictionary.id)
    }

    void "NHS classes: should update all links when the label has changed"() {
        given: "there is an initial data dictionary"
        loginUser('admin@maurodatamapper.com', 'password')
        def dataDictionary =  createNhsDataDictionaryStructure()

        and: "there is an item to modify"
        DataClassModel dataClassItemToModify = dataDictionary.classesAndAttributes.classes.find { it.label == "APPOINTMENT" }

        // This data class is referenced in these other items description fields. These should all have the same description as
        // set in createNhsDataDictionaryStructure()
        DataElementModel relatedDataElement = dataClassItemToModify.elements.find { it.label == "APPOINTMENT DATE" }
        DataClassModel relatedDataClass = dataDictionary.classesAndAttributes.classes.find { it.label == "CLINICAL TRIAL" }
        TermModel relatedTerm = dataDictionary.nhsBusinessDefinitions.terms.find { it.definition == "NHS Business Definition 1" }

        when: "the label is modified"
        String originalLabel = dataClassItemToModify.label
        String newLabel = "$originalLabel MODIFIED"
        String expectedNewDescription = "Refers to dm:Classes and Attributes|dc:APPOINTMENT MODIFIED"

        PUT("dataModels/$dataDictionary.classesAndAttributes.id/dataClasses/$dataClassItemToModify.id", [
            label: newLabel
        ], MAP_ARG, true)

        then: "the response should be OK"
        verifyResponse(OK, response)

        and: "the label should be modified"
        verifyAll(responseBody()) {
            label == newLabel
            path != dataClassItemToModify.path
        }

        when: "waiting for the background job to complete"
        AsyncJob asyncJob = getAsyncJob()
        verifyAll {
            asyncJob != null
        }
        waitForAsyncJobToComplete(asyncJob)

        then: "the links to the original item have been updated"
        GET(
            "dataModels/$dataDictionary.classesAndAttributes.id/dataClasses/$dataClassItemToModify.id/dataElements/$relatedDataElement.id",
            MAP_ARG,
            true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            description == expectedNewDescription
        }

        then: "the links to the original item have been updated"
        GET(
            "dataModels/$dataDictionary.classesAndAttributes.id/dataClasses/$relatedDataClass.id",
            MAP_ARG,
            true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            description == expectedNewDescription
        }

        then: "the links to the original item have been updated"
        GET(
            "terminologies/$dataDictionary.nhsBusinessDefinitions.id/terms/$relatedTerm.id",
            MAP_ARG,
            true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            description == expectedNewDescription
        }

        cleanup:
        cleanUpVersionedFolder(dataDictionary.id)
    }

    void "NHS data set constraint: should update all links when the label has changed"() {
        given: "there is an initial data dictionary"
        loginUser('admin@maurodatamapper.com', 'password')
        def dataDictionary =  createNhsDataDictionaryStructure()

        and: "there is an item to modify"
        TermModel termToModify = dataDictionary.dataSetConstraints.terms.find { it.code == "Data Set Constraint 1" }

        // This term is referenced in these other items description fields. These should all have the same description as
        // set in createNhsDataDictionaryStructure()
        DataClassModel relatedDataClass = dataDictionary.classesAndAttributes.classes.find { it.label == "APPOINTMENT" }
        TermModel relatedTerm = dataDictionary.supportingInformation.terms.find { it.code == "Supporting Information 1" }

        when: "the label is modified"
        String originalCode = termToModify.code
        String newCode = "$originalCode MODIFIED"
        String expectedNewDescription = "Refers to te:Data Set Constraints|tm:Data Set Constraint 1 MODIFIED"

        PUT("terminologies/$dataDictionary.dataSetConstraints.id/terms/$termToModify.id", [
            code: newCode,
            definition: newCode
        ], MAP_ARG, true)

        then: "the response should be OK"
        verifyResponse(OK, response)

        and: "the label should be modified"
        verifyAll(responseBody()) {
            code == newCode
            definition == newCode
            path != termToModify.path
        }

        when: "waiting for the background job to complete"
        AsyncJob asyncJob = getAsyncJob()
        verifyAll {
            asyncJob != null
        }
        waitForAsyncJobToComplete(asyncJob)

        then: "the links to the original item have been updated"
        GET(
            "dataModels/$dataDictionary.classesAndAttributes.id/dataClasses/$relatedDataClass.id",
            MAP_ARG,
            true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            description == expectedNewDescription
        }

        then: "the links to the original item have been updated"
        GET(
            "terminologies/$dataDictionary.supportingInformation.id/terms/$relatedTerm.id",
            MAP_ARG,
            true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            description == expectedNewDescription
        }

        cleanup:
        cleanUpVersionedFolder(dataDictionary.id)
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
        dataDictionaryModel.classesAndAttributes.classes = [
            new DataClassModel(
                "APPOINTMENT",
                "Refers to te:Data Set Constraints|tm:Data Set Constraint 1",
                [new DataElementModel("APPOINTMENT DATE", "Refers to dm:Classes and Attributes|dc:APPOINTMENT")]),
            new DataClassModel(
                "CLINICAL TRIAL",
                "Refers to dm:Classes and Attributes|dc:APPOINTMENT",
                [new DataElementModel("CLINICAL TRIAL NAME", "")])
        ]

        createDataModel(dataDictionaryModel.id, dataDictionaryModel.classesAndAttributes)
        createDataClasses(dataDictionaryModel.classesAndAttributes)

        // -- Data Elements --------------------
        dataDictionaryModel.dataElements.classes = [
            new DataClassModel("B", "", [new DataElementModel("BIRTH ORDER", "")]),
            new DataClassModel("D", "", [new DataElementModel("DATA SET VERSION NUMBER", "")])
        ]

        createDataModel(dataDictionaryModel.id, dataDictionaryModel.dataElements)
        createDataClasses(dataDictionaryModel.dataElements)

        // -- Data Set Constraints --------------------
        dataDictionaryModel.dataSetConstraints.terms = [
            new TermModel("Data Set Constraint 1", "Data Set Constraint 1", "")
        ]

        createTerminology(dataDictionaryModel.id, dataDictionaryModel.dataSetConstraints)
        createTerms(dataDictionaryModel.dataSetConstraints)

        // -- NHS Business Definitions --------------------
        dataDictionaryModel.nhsBusinessDefinitions.terms = [
            new TermModel(
                "NHS Business Definition 1",
                "NHS Business Definition 1",
                "Refers to dm:Classes and Attributes|dc:APPOINTMENT")
        ]

        createTerminology(dataDictionaryModel.id, dataDictionaryModel.nhsBusinessDefinitions)
        createTerms(dataDictionaryModel.nhsBusinessDefinitions)

        // -- Supporting Information --------------------
        dataDictionaryModel.supportingInformation.terms = [
            new TermModel(
                "Supporting Information 1",
                "Supporting Information 1",
                "Refers to te:Data Set Constraints|tm:Data Set Constraint 1")
        ]

        createTerminology(dataDictionaryModel.id, dataDictionaryModel.supportingInformation)
        createTerms(dataDictionaryModel.supportingInformation)

        // Return all the details the test needs
        dataDictionaryModel
    }

    void createDataModel(String dataDictionaryId, DataModelModel model) {
        POST("folders/$dataDictionaryId/dataModels", [
            label: model.label
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        model.id = responseBody().id
        model.path = responseBody().path
        log.info("Created '$model.label' data model [$model.id]")

        POST("dataModels/$model.id/dataTypes", [
            domainType: "PrimitiveType",
            label: "String"
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        model.dataTypeId = responseBody().id
        log.info("Added data type [$model.dataTypeId]")
    }

    void createDataClasses(DataModelModel model) {
        model.classes.forEach { dataClass ->
            POST("dataModels/$model.id/dataClasses", [
                label: dataClass.label,
                description: dataClass.description
            ], MAP_ARG, true)
            verifyResponse(CREATED, response)
            dataClass.id = responseBody().id
            dataClass.path = responseBody().path
            log.info("Added data class '$dataClass.label' [$dataClass.id]")

            dataClass.elements.forEach { dataElement ->
                POST("dataModels/$model.id/dataClasses/$dataClass.id/dataElements", [
                    label: dataElement.label,
                    dataType: model.dataTypeId,
                    description: dataElement.description
                ], MAP_ARG, true)
                verifyResponse(CREATED, response)
                dataElement.id = responseBody().id
                dataElement.path = responseBody().path
                log.info("Added data element '$dataElement.label' [$dataElement.id]")
            }
        }
    }

    void createTerminology(String dataDictionaryId, TerminologyModel model) {
        POST("folders/$dataDictionaryId/terminologies", [
            label: model.label
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        model.id = responseBody().id
        model.path = responseBody().path
        log.info("Created '$model.label' terminology [$model.id]")
    }

    void createTerms(TerminologyModel model) {
        model.terms.forEach { term ->
            POST("terminologies/$model.id/terms", [
                code: term.code,
                definition: term.definition,
                description: term.description
            ], MAP_ARG, true)
            verifyResponse(CREATED, response)
            term.id = responseBody().id
            term.path = responseBody().path
            log.info("Added term '$term.code: $term.definition' [$term.id]")
        }
    }
}
