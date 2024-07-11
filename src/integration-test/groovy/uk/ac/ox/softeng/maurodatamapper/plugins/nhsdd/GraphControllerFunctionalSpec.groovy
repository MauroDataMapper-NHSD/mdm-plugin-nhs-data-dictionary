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
package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import spock.lang.Shared
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary

import static io.micronaut.http.HttpStatus.ACCEPTED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

@Integration
@Slf4j
@Rollback
class GraphControllerFunctionalSpec extends BaseDataDictionaryFunctionalSpec {
    // Trace the item layout of a test data dictionary
    @Shared
    VersionedFolder dictionaryBranch
    @Shared
    DataModel dataModel1
    @Shared
    PrimitiveType dataType1
    @Shared
    DataClass dataClass1
    @Shared
    DataElement dataElement1
    @Shared
    DataModel dataModel2
    @Shared
    PrimitiveType dataType2
    @Shared
    DataClass dataClass2
    @Shared
    DataElement dataElement2
    @Shared
    Terminology terminology1
    @Shared
    Term term1

    @RunOnce
    @Transactional
    def setup() {
        dictionaryBranch = given."there is a versioned folder"("NHS Data Dictionary")

        dataModel1 = given."there is a data model"("Data Model 1", dictionaryBranch)
        dataType1 = given."there is a primitive data type"("dataType1", dataModel1)
        dataClass1 = given."there is a data class"("Data Class 1", dataModel1)
        dataElement1 = given."there is a data element"("Data Element 1", dataModel1, dataClass1, dataType1)

        dataModel2 = given."there is a data model"("Data Model 2", dictionaryBranch)
        dataType2 = given."there is a primitive data type"("dataType2", dataModel2)
        dataClass2 = given."there is a data class"("Data Class 2", dataModel2)
        dataElement2 = given."there is a data element"("Data Element 2", dataModel2, dataClass2, dataType2)

        terminology1 = given."there is a terminology"("Terminology 1", dictionaryBranch)
        term1 = given."there is a term"("Term 1", "Term 1", terminology1)

        sessionFactory.currentSession.flush()
    }

    void "should build a graph node for a Mauro item: #predecessorDomainType"(
        String predecessorDomainType,
        Closure getPredecessorId,
        Closure getPredecessorPath,
        Closure getPredecessorUpdateEndpoint) {
        given: "a user is logged in"
        loginUser('admin@maurodatamapper.com', 'password')

        and: "an item has a description set"
        String description = """<a href=\"$dataModel1.path\">Model</a>, 
<a href=\"$dataClass1.path\">Class</a>, 
<a href=\"$dataElement1.path\">Element</a>, 
<a href=\"https://www.google.com\">Website</a>"""

        String predecessorUpdateEndpoint = getPredecessorUpdateEndpoint.call()
        PUT(predecessorUpdateEndpoint, [description: description], MAP_ARG, true)
        verifyResponse(OK, response)

        when: "the graph node is built"
        UUID predecessorId = getPredecessorId.call() as UUID
        String predecessorPath = getPredecessorPath()
        PUT("nhsdd/$dictionaryBranch.id/graph/$predecessorDomainType/$predecessorId", [:], MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        and: "the response contains the expected graph node"
        verifyAll(responseBody()) {
            successors ==~ [this.dataModel1.path.toString(), this.dataClass1.path.toString(), this.dataElement1.path.toString()]
            predecessors ==~ []
        }

        when: "the graph node is fetched for the predecessor item"
        GET("nhsdd/$dictionaryBranch.id/graph/$predecessorDomainType/$predecessorId", MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        and: "the response contains the expected graph node"
        verifyAll(responseBody()) {
            successors ==~ [this.dataModel1.path.toString(), this.dataClass1.path.toString(), this.dataElement1.path.toString()]
            predecessors ==~ []
        }

        when: "the graph node is fetched for a successor item"
        GET("nhsdd/$dictionaryBranch.id/graph/dataModels/$dataModel1.id", MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        and: "the response contains the expected graph node"
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [predecessorPath]
        }

        when: "the graph node is fetched for a successor item"
        GET("nhsdd/$dictionaryBranch.id/graph/dataClasses/$dataClass1.id", MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        and: "the response contains the expected graph node"
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [predecessorPath]
        }

        when: "the graph node is fetched for a successor item"
        GET("nhsdd/$dictionaryBranch.id/graph/dataElements/$dataElement1.id", MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        and: "the response contains the expected graph node"
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [predecessorPath]
        }

        cleanup:
        // Remove existing graph nodes
        DELETE("nhsdd/$dictionaryBranch.id/graph/$predecessorDomainType/$predecessorId", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)

        DELETE("nhsdd/$dictionaryBranch.id/graph/dataModels/$dataModel1.id", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)

        DELETE("nhsdd/$dictionaryBranch.id/graph/dataClasses/$dataClass1.id", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)

        DELETE("nhsdd/$dictionaryBranch.id/graph/dataElements/$dataElement1.id", MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)

        where:
        predecessorDomainType   | getPredecessorId      | getPredecessorPath                | getPredecessorUpdateEndpoint
        "dataModels"            | { dataModel2.id }     | { dataModel2.path.toString() }    | { "dataModels/$dataModel2.id" }
        "dataClasses"           | { dataClass2.id }     | { dataClass2.path.toString() }    | { "dataModels/$dataModel2.id/dataClasses/$dataClass2.id" }
        "dataElements"          | { dataElement2.id }   | { dataElement2.path.toString() }  | { "dataModels/$dataModel2.id/dataClasses/$dataClass2.id/dataElements/$dataElement2.id" }
        "terms"                 | { term1.id }          | { term1.path.toString() }         | { "terminologies/$terminology1.id/terms/$term1.id" }
    }

    void "should build a full graph for a data dictionary: async = #async"(boolean async) {
        given: "a user is logged in"
        loginUser('admin@maurodatamapper.com', 'password')

        and: "there is a data dictionary"
        // Build up a data dictionary and structure all the descriptions so they contain links to each other item
        ResponseModel dataDictionary = "given there is a data dictionary branch"()
        // Data Sets
        ResponseModel dataSetsFolder = "given there is a folder"(dataDictionary.id, NhsDataDictionary.DATA_SETS_FOLDER_NAME)
        ResponseModel dataSetsSubFolder = "given there is a folder"(dataSetsFolder.id, "Sample Data Sets")
        ResponseModel dataSetModel = "given there is a data model"(
            dataSetsSubFolder.id,
            "Critical Care Data Set",
            "Refers to <a href=\"dm:Classes and Attributes\$main|dc:APPOINTMENT\">item1</a> and <a href=\"te:NHS Business Definitions\$main|tm:ABO System\">item2</a>")
        ResponseModel dataSetType = "given there is a primitive data type"(dataSetModel.id, "dataSetType")
        ResponseModel dataSetClass = "given there is a parent data class"(dataSetModel.id, "Data Set Table")
        ResponseModel dataSetElement = "given there is a data element"(dataSetModel.id, dataSetClass.id, "Data Set Element", dataSetType.id)
        // Classes and Attributes
        ResponseModel nhsClassesAndAttributes = "given there is a data model"(dataDictionary.id, NhsDataDictionary.CLASSES_MODEL_NAME)
        ResponseModel nhsClassesAndAttributesType = "given there is a primitive data type"(nhsClassesAndAttributes.id, "classesAndAttributesType")
        ResponseModel nhsClass = "given there is a parent data class"(
            nhsClassesAndAttributes.id,
            "APPOINTMENT",
            "Refers to <a href=\"dm:Critical Care Data Set\$main|dc:Data Set Table|de:Data Set Element\">item</a>")
        ResponseModel nhsAttribute = "given there is a data element"(
            nhsClassesAndAttributes.id,
            nhsClass.id,
            "APPOINTMENT DATE",
            nhsClassesAndAttributesType.id,
            "Refers to <a href=\"dm:Classes and Attributes\$main|dc:APPOINTMENT\">item</a>")
        // NHS Business Definitions
        ResponseModel nhsBusinessDefinitions = "given there is a terminology"(dataDictionary.id, NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME)
        ResponseModel nhsBusinessTerm = "given there is a term"(
            nhsBusinessDefinitions.id,
            "ABO System",
            "ABO System",
            "Refers to <a href=\"dm:Classes and Attributes\$main|dc:APPOINTMENT\">item</a>")

        when: "the full graph is built"
        PUT("nhsdd/$dataDictionary.id/graph?asynchronous=$async", [:], MAP_ARG, true)

        then: "the response status is correct"
        verifyResponse(async ? ACCEPTED : OK, response)

        when: "the graph node is fetched for data set"
        if (async) {
            AsyncJob asyncJob = getLastAsyncJob()
            waitForAsyncJobToComplete(asyncJob)
        }

        GET("nhsdd/$dataDictionary.id/graph/dataModels/$dataSetModel.id", MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        and: "the response contains the expected graph node"
        verifyAll(responseBody()) {
            successors ==~ [nhsClass.path, nhsBusinessTerm.path]
            predecessors ==~ []
        }

        when: "the graph node is fetched for data set element"
        GET("nhsdd/$dataDictionary.id/graph/dataElements/$dataSetElement.id", MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        and: "the response contains the expected graph node"
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [nhsClass.path]
        }

        when: "the graph node is fetched for nhs class"
        GET("nhsdd/$dataDictionary.id/graph/dataClasses/$nhsClass.id", MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        and: "the response contains the expected graph node"
        verifyAll(responseBody()) {
            successors ==~ [dataSetElement.path]
            predecessors ==~ [dataSetModel.path, nhsBusinessTerm.path, nhsAttribute.path]
        }

        when: "the graph node is fetched for nhs attribute"
        GET("nhsdd/$dataDictionary.id/graph/dataElements/$nhsAttribute.id", MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        and: "the response contains the expected graph node"
        verifyAll(responseBody()) {
            successors ==~ [nhsClass.path]
            predecessors ==~ []
        }

        when: "the graph node is fetched for nhs business term"
        GET("nhsdd/$dataDictionary.id/graph/terms/$nhsBusinessTerm.id", MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        and: "the response contains the expected graph node"
        verifyAll(responseBody()) {
            successors ==~ [nhsClass.path]
            predecessors ==~ [dataSetModel.path]
        }

        cleanup:
        cleanUpVersionedFolder(dataDictionary.id)

        where:
        async | _
        false | _
        true  | _
    }
}
