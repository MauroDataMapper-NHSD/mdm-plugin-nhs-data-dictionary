/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
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
class DataDictionaryItemUpdatedFunctionalSpec extends BaseDataDictionaryItemTrackerFunctionalSpec {
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
        waitForLastAsyncJobToComplete()

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
}
