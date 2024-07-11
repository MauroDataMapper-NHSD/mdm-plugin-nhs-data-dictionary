package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.interceptors

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

@Integration
@Slf4j
@Rollback
class DataDictionaryItemMovedFunctionalSpec extends BaseDataDictionaryItemTrackerFunctionalSpec {
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
        waitForLastAsyncJobToComplete()

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
        waitForLastAsyncJobToComplete()

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
}
