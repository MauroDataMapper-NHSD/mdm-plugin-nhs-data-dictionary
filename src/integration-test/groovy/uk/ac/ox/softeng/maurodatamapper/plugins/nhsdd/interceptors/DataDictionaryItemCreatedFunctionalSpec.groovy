package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.interceptors

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT

@Integration
@Slf4j
@Rollback
class DataDictionaryItemCreatedFunctionalSpec extends BaseDataDictionaryItemTrackerFunctionalSpec {
    void "should automatically build a graph node after creating #domainType"(
        String domainType,
        Closure getCreateEndpoint) {
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
        String description = """<a href=\"$dataSetModelPath\">Model</a>, 
<a href=\"$nhsBusinessTermPath\">Term</a>, 
<a href=\"$nhsAttributePath\">Element</a>, 
<a href=\"https://www.google.com\">Website</a>"""

        when: "a new domain item is created"
        String createEndpoint = getCreateEndpoint()
        Map requestBody = buildNewItemRequestBody(domainType, "New Item", description)
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
        waitForLastAsyncJobToComplete()

        and: "there is a graph node for the new item"
        GET("nhsdd/$dictionaryBranch.id/graph/$domainType/$newItemId", MAP_ARG, true)
        verifyResponse(HttpStatus.OK, response)

        and: "the successors are correct"
        verifyAll(responseBody()) {
            successors ==~ [this.dataSetModel.path.toString(), this.nhsBusinessTerm.path.toString(), this.nhsAttribute.path.toString()]
            predecessors ==~ []
        }

        and: "the predecessors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/dataModels/$dataSetModel.id", MAP_ARG, true)
        verifyResponse(HttpStatus.OK, response)
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [newItemPath]
        }

        and: "the predecessors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/terms/$nhsBusinessTerm.id", MAP_ARG, true)
        verifyResponse(HttpStatus.OK, response)
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [newItemPath]
        }

        and: "the predecessors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/dataElements/$nhsAttribute.id", MAP_ARG, true)
        verifyResponse(HttpStatus.OK, response)
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

        where:
        domainType          | getCreateEndpoint
        "dataModels"        | { "folders/$dataSetsSubFolder.id/dataModels" }
        "dataClasses"       | { "dataModels/$dataSetModel.id/dataClasses" }
        "dataElements"      | { "dataModels/$dataSetModel.id/dataClasses/$dataSetClass.id/dataElements" }
        "terms"             | { "terminologies/$nhsBusinessDefinitions.id/terms" }
        "folders"           | { "folders/$dataSetsSubFolder.id/folders" }
    }
}
