package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.interceptors

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.BaseDataDictionaryFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import spock.lang.Shared
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

@Integration
@Slf4j
@Rollback
class DataDictionaryItemUpdatedFunctionalSpec extends BaseDataDictionaryFunctionalSpec {
    // Trace the item layout of a test data dictionary
    @Shared
    VersionedFolder dictionaryBranch
    @Shared
    Folder dataSetsFolder
    @Shared
    Folder dataSetsSubFolder
    @Shared
    DataModel dataSetModel
    @Shared
    PrimitiveType dataSetType
    @Shared
    DataClass dataSetClass
    @Shared
    DataElement dataSetElement
    @Shared
    DataModel nhsClassesAndAttributes
    @Shared
    PrimitiveType nhsClassesAndAttributesType
    @Shared
    DataClass nhsClass
    @Shared
    DataElement nhsAttribute
    @Shared
    Terminology nhsBusinessDefinitions
    @Shared
    Term nhsBusinessTerm

    @RunOnce
    @Transactional
    def setup() {
        dictionaryBranch = given."there is a versioned folder"("NHS Data Dictionary")

        // Data Sets
        dataSetsFolder = given."there is a folder"(NhsDataDictionary.DATA_SETS_FOLDER_NAME, "", dictionaryBranch)
        dataSetsSubFolder = given."there is a folder"("Sample Data Sets", "", dataSetsFolder)
        dataSetModel = given."there is a data model"("Critical Care Data Set", dataSetsSubFolder)
        dataSetType = given."there is a primitive data type"("dataSetType", dataSetModel)
        dataSetClass = given."there is a data class"("Data Set Table", dataSetModel)
        dataSetElement = given."there is a data element"("Data Set Element", dataSetModel, dataSetClass, dataSetType)

        // Classes and Attributes
        nhsClassesAndAttributes = given."there is a data model"(NhsDataDictionary.CLASSES_MODEL_NAME, dictionaryBranch)
        nhsClassesAndAttributesType = given."there is a primitive data type"("classesAndAttributesType", nhsClassesAndAttributes)
        nhsClass = given."there is a data class"("APPOINTMENT", nhsClassesAndAttributes)
        nhsAttribute = given."there is a data element"("APPOINTMENT DATE", nhsClassesAndAttributes, nhsClass, nhsClassesAndAttributesType)

        // NHS Business Definitions
        nhsBusinessDefinitions = given."there is a terminology"(NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME, dictionaryBranch)
        nhsBusinessTerm = given."there is a term"("ABO System", "ABO System", nhsBusinessDefinitions)

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug("Cleanup specification")
        cleanUpResources(DataModel, PrimitiveType, DataClass, DataElement, Terminology, Term, Folder, VersionedFolder)
    }

    private static String getPathStringWithoutBranchName(Path path, String branchName) {
        path.toString().replace("\$$branchName", "")
    }

    void "should automatically update a graph node after updating the description of #domainType"(
        String domainType,
        Closure getCreateEndpoint,
        Closure getUpdateEndpoint) {
        given: "a user is logged in"
        loginUser('admin@maurodatamapper.com', 'password')

        and: "a description is prepared"
        // *Don't* include the branch name in paths to match what MDM UI would do when
        // saving the description
        String dataSetModelPath = getPathStringWithoutBranchName(dataSetModel.path, dictionaryBranch.branchName)
        String nhsBusinessTermPath = getPathStringWithoutBranchName(nhsBusinessTerm.path, dictionaryBranch.branchName)
        String nhsAttributePath = getPathStringWithoutBranchName(nhsAttribute.path, dictionaryBranch.branchName)
        verifyAll {
            dataSetModelPath == "dm:Critical Care Data Set"
            nhsBusinessTermPath == "te:NHS Business Definitions|tm:ABO System"
            nhsAttributePath == "dm:Classes and Attributes|dc:APPOINTMENT|de:APPOINTMENT DATE"
        }
        String originalDescription = """<a href=\"$dataSetModelPath\">Model</a>, 
<a href=\"$nhsBusinessTermPath\">Term</a>, 
<a href=\"$nhsAttributePath\">Element</a>, 
<a href=\"https://www.google.com\">Website</a>"""

        when: "a new domain item is created"
        String createEndpoint = getCreateEndpoint()
        Map requestBody = buildNewItemRequestBody(domainType, "Item to Update", originalDescription)
        POST(createEndpoint, requestBody, MAP_ARG, true)

        then: "the response is correct"
        verifyResponse(CREATED, response)

        and: "the response has object values"
        def body = responseBody()
        String newItemId = body.id
        String newItemPath = body.path
        verifyAll {
            newItemId
            newItemPath
        }

        and: "wait for the async job to finish"
        // Why do we need to sleep for a period of time to make sure that async jobs exist? This is the only way I could
        // these tests to pass, annoyingly. I think multiple threads are in play between the test, the backend controller and
        // the interceptor, all confusing things
        log.warn("Wait 2 seconds to catchup...")
        Thread.sleep(2000)
        AsyncJob asyncJob = getLastAsyncJob()
        waitForAsyncJobToComplete(asyncJob)

        and: "the successors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/$domainType/$newItemId", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ [this.dataSetModel.path.toString(), this.nhsBusinessTerm.path.toString(), this.nhsAttribute.path.toString()]
            predecessors ==~ []
        }

        and: "the predecessors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/dataModels/$dataSetModel.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [newItemPath]
        }

        and: "the predecessors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/terms/$nhsBusinessTerm.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [newItemPath]
        }

        and: "the predecessors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/dataElements/$nhsAttribute.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [newItemPath]
        }

        and: "the predecessors were updated"
        GET("nhsdd/$dictionaryBranch.id/graph/dataClasses/$nhsClass.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ []
        }

        when: "an updated description is prepared"
        // *Don't* include the branch name in paths to match what MDM UI would do when
        // saving the description
        String nhsClassPath = getPathStringWithoutBranchName(nhsClass.path, dictionaryBranch.branchName)
        verifyAll {
            nhsClassPath == "dm:Classes and Attributes|dc:APPOINTMENT"
        }
        String updatedDescription = """<a href=\"$nhsClassPath\">Class</a>, 
<a href=\"$nhsBusinessTermPath\">Term</a>, 
<a href=\"https://www.google.com\">Website</a>"""

        and: "the item is updated"
        String updateEndpoint = getUpdateEndpoint(newItemId)
        PUT(updateEndpoint, [
            description: updatedDescription
        ],MAP_ARG, true)

        then: "the response is correct"
        verifyResponse(OK, response)

        and: "wait for the async job to finish"
        // Why do we need to sleep for a period of time to make sure that async jobs exist? This is the only way I could
        // these tests to pass, annoyingly. I think multiple threads are in play between the test, the backend controller and
        // the interceptor, all confusing things
        log.warn("Wait 2 seconds to catchup...")
        Thread.sleep(2000)
        AsyncJob asyncJob2 = getLastAsyncJob()
        waitForAsyncJobToComplete(asyncJob2)

        and: "the successors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/$domainType/$newItemId", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ [this.nhsClass.path.toString(), this.nhsBusinessTerm.path.toString()]
            predecessors ==~ []
        }

        and: "the predecessors were updated"
        GET("nhsdd/$dictionaryBranch.id/graph/dataModels/$dataSetModel.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ []
        }

        and: "the predecessors were updated"
        GET("nhsdd/$dictionaryBranch.id/graph/terms/$nhsBusinessTerm.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [newItemPath]
        }

        and: "the predecessors were updated"
        GET("nhsdd/$dictionaryBranch.id/graph/dataElements/$nhsAttribute.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ []
        }

        and: "the predecessors were updated"
        GET("nhsdd/$dictionaryBranch.id/graph/dataClasses/$nhsClass.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [newItemPath]
        }

        cleanup:
        // Remove existing graph nodes
        DELETE("nhsdd/$dictionaryBranch.id/graph/$domainType/$newItemId", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)

        DELETE("nhsdd/$dictionaryBranch.id/graph/dataModels/$dataSetModel.id", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)

        DELETE("nhsdd/$dictionaryBranch.id/graph/terms/$nhsBusinessTerm.id", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)

        DELETE("nhsdd/$dictionaryBranch.id/graph/dataElements/$nhsAttribute.id", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)

        DELETE("nhsdd/$dictionaryBranch.id/graph/dataClasses/$nhsClass.id", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)

        where:
        domainType          | getCreateEndpoint                                                             | getUpdateEndpoint
        "dataModels"        | { "folders/$dataSetsSubFolder.id/dataModels" }                                | { id -> "dataModels/$id" }
        "dataClasses"       | { "dataModels/$dataSetModel.id/dataClasses" }                                 | { id -> "dataModels/$dataSetModel.id/dataClasses/$id" }
        "dataElements"      | { "dataModels/$dataSetModel.id/dataClasses/$dataSetClass.id/dataElements" }   | { id -> "dataModels/$dataSetModel.id/dataClasses/$dataSetClass.id/dataElements/$id" }
        "terms"             | { "terminologies/$nhsBusinessDefinitions.id/terms" }                          | { id -> "terminologies/$nhsBusinessDefinitions.id/terms/$id" }
        "folders"           | { "folders/$dataSetsSubFolder.id/folders" }                                   | { id ->  "folders/$id" }
    }

    void "should automatically update a graph node after changing the path of #domainType"(
        String domainType,
        Closure getCreateEndpoint,
        Closure getUpdateEndpoint) {
        given: "a user is logged in"
        loginUser('admin@maurodatamapper.com', 'password')

        and: "a description is prepared"
        // *Don't* include the branch name in paths to match what MDM UI would do when
        // saving the description
        String dataSetModelPath = getPathStringWithoutBranchName(dataSetModel.path, dictionaryBranch.branchName)
        String nhsBusinessTermPath = getPathStringWithoutBranchName(nhsBusinessTerm.path, dictionaryBranch.branchName)
        String nhsAttributePath = getPathStringWithoutBranchName(nhsAttribute.path, dictionaryBranch.branchName)
        verifyAll {
            dataSetModelPath == "dm:Critical Care Data Set"
            nhsBusinessTermPath == "te:NHS Business Definitions|tm:ABO System"
            nhsAttributePath == "dm:Classes and Attributes|dc:APPOINTMENT|de:APPOINTMENT DATE"
        }
        String originalDescription = """<a href=\"$dataSetModelPath\">Model</a>, 
<a href=\"$nhsBusinessTermPath\">Term</a>, 
<a href=\"$nhsAttributePath\">Element</a>, 
<a href=\"https://www.google.com\">Website</a>"""

        when: "a new domain item is created"
        String createEndpoint = getCreateEndpoint()
        Map requestBody = buildNewItemRequestBody(domainType, "Item to Update", originalDescription)
        POST(createEndpoint, requestBody, MAP_ARG, true)

        then: "the response is correct"
        verifyResponse(CREATED, response)

        and: "the response has object values"
        def createdResponseBody = responseBody()
        String newItemId = createdResponseBody.id
        String newItemPath = createdResponseBody.path
        verifyAll {
            newItemId
            newItemPath
        }

        and: "wait for the async job to finish"
        // Why do we need to sleep for a period of time to make sure that async jobs exist? This is the only way I could
        // these tests to pass, annoyingly. I think multiple threads are in play between the test, the backend controller and
        // the interceptor, all confusing things
        log.warn("Wait 2 seconds to catchup...")
        Thread.sleep(2000)
        AsyncJob asyncJob = getLastAsyncJob()
        waitForAsyncJobToComplete(asyncJob)

        and: "the successors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/$domainType/$newItemId", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ [this.dataSetModel.path.toString(), this.nhsBusinessTerm.path.toString(), this.nhsAttribute.path.toString()]
            predecessors ==~ []
        }

        and: "the predecessors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/dataModels/$dataSetModel.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [newItemPath]
        }

        and: "the predecessors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/terms/$nhsBusinessTerm.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [newItemPath]
        }

        and: "the predecessors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/dataElements/$nhsAttribute.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [newItemPath]
        }

        when: "the item is updated to change its path"
        String updateEndpoint = getUpdateEndpoint(newItemId)
        Map updateRequestBody = buildUpdateItemRequestBody(domainType, "Changed Item to Update")
        PUT(updateEndpoint, updateRequestBody,MAP_ARG, true)

        then: "the response is correct"
        verifyResponse(OK, response)

        and: "the response has object values"
        def updatedResponseBody = responseBody()
        String updatedItemId = updatedResponseBody.id
        String updatedItemPath = updatedResponseBody.path
        verifyAll {
            updatedItemId
            updatedItemPath
            updatedItemId == newItemId
            updatedItemPath != newItemPath
        }

        and: "wait for the async job to finish"
        // Why do we need to sleep for a period of time to make sure that async jobs exist? This is the only way I could
        // these tests to pass, annoyingly. I think multiple threads are in play between the test, the backend controller and
        // the interceptor, all confusing things
        log.warn("Wait 2 seconds to catchup...")
        Thread.sleep(2000)
        AsyncJob asyncJob2 = getLastAsyncJob()
        waitForAsyncJobToComplete(asyncJob2)

        and: "the successors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/$domainType/$newItemId", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ [this.dataSetModel.path.toString(), this.nhsBusinessTerm.path.toString(), this.nhsAttribute.path.toString()]
            predecessors ==~ []
        }

        and: "the predecessors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/dataModels/$dataSetModel.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [updatedItemPath]
        }

        and: "the predecessors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/terms/$nhsBusinessTerm.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [updatedItemPath]
        }

        and: "the predecessors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/dataElements/$nhsAttribute.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [updatedItemPath]
        }

        cleanup:
        // Remove existing graph nodes
        DELETE("nhsdd/$dictionaryBranch.id/graph/$domainType/$newItemId", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)

        DELETE("nhsdd/$dictionaryBranch.id/graph/dataModels/$dataSetModel.id", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)

        DELETE("nhsdd/$dictionaryBranch.id/graph/terms/$nhsBusinessTerm.id", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)

        DELETE("nhsdd/$dictionaryBranch.id/graph/dataElements/$nhsAttribute.id", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)

        where:
        domainType          | getCreateEndpoint                                                             | getUpdateEndpoint
        "dataModels"        | { "folders/$dataSetsSubFolder.id/dataModels" }                                | { id -> "dataModels/$id" }
        "dataClasses"       | { "dataModels/$dataSetModel.id/dataClasses" }                                 | { id -> "dataModels/$dataSetModel.id/dataClasses/$id" }
        "dataElements"      | { "dataModels/$dataSetModel.id/dataClasses/$dataSetClass.id/dataElements" }   | { id -> "dataModels/$dataSetModel.id/dataClasses/$dataSetClass.id/dataElements/$id" }
        "terms"             | { "terminologies/$nhsBusinessDefinitions.id/terms" }                          | { id -> "terminologies/$nhsBusinessDefinitions.id/terms/$id" }
        "folders"           | { "folders/$dataSetsSubFolder.id/folders" }                                   | { id ->  "folders/$id" }
    }

    private Map buildNewItemRequestBody(
        String domainType,
        String label,
        String description) {
        if (domainType == "terms") {
            return [
                code: label,
                definition: label,
                description: description
            ]
        }

        if (domainType == "dataElements") {
            return [
                label: label,
                dataType: dataSetType.id,   // Assume that the data element is being created under the Critical Care Data Set
                description: description
            ]
        }

        return [
            label: label,
            description: description
        ]
    }

    private Map buildUpdateItemRequestBody(
        String domainType,
        String label) {
        if (domainType == "terms") {
            return [
                code: label,
                definition: label
            ]
        }

        return [
            label: label
        ]
    }
}
