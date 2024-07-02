package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd


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

    void "should build a full graph for a Mauro versioned folder"() {
        given: "a user is logged in"
        loginUser('admin@maurodatamapper.com', 'password')

        and: "a description is set"
        String description = """<a href=\"$dataModel1.path\">Model</a>,
<a href=\"$dataClass1.path\">Class</a>,
<a href=\"$dataElement1.path\">Element</a>,
<a href=\"https://www.google.com\">Website</a>"""

        PUT("dataModels/$dataModel2.id", [description: description], MAP_ARG, true)
        verifyResponse(OK, response)

        and: "a description is set"
        description = """<a href=\"$dataClass1.path\">Class</a>,
<a href=\"$dataElement1.path\">Element</a>,
<a href=\"https://www.google.com\">Website</a>"""

        PUT("terminologies/$terminology1.id/terms/$term1.id", [description: description], MAP_ARG, true)
        verifyResponse(OK, response)

        when: "the full graph is built"
        PUT("nhsdd/$dictionaryBranch.id/graph", [:], MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        when: "the graph node is fetched for data model 1"
        GET("nhsdd/$dictionaryBranch.id/graph/dataModels/$dataModel1.id", MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        and: "the response contains the expected graph node"
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [this.dataModel2.path.toString()]
        }

        when: "the graph node is fetched for data class 1"
        GET("nhsdd/$dictionaryBranch.id/graph/dataClasses/$dataClass1.id", MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        and: "the response contains the expected graph node"
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [this.dataModel2.path.toString(), this.term1.path.toString()]
        }

        when: "the graph node is fetched for data element 1"
        GET("nhsdd/$dictionaryBranch.id/graph/dataElements/$dataElement1.id", MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        and: "the response contains the expected graph node"
        verifyAll(responseBody()) {
            successors ==~ []
            predecessors ==~ [this.dataModel2.path.toString(), this.term1.path.toString()]
        }

        when: "the graph node is fetched for data model 2"
        GET("nhsdd/$dictionaryBranch.id/graph/dataModels/$dataModel2.id", MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        and: "the response contains the expected graph node"
        verifyAll(responseBody()) {
            successors ==~ [this.dataModel1.path.toString(), this.dataClass1.path.toString(), this.dataElement1.path.toString()]
            predecessors ==~ []
        }

        when: "the graph node is fetched for term 1"
        GET("nhsdd/$dictionaryBranch.id/graph/terms/$term1.id", MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        and: "the response contains the expected graph node"
        verifyAll(responseBody()) {
            successors ==~ [this.dataClass1.path.toString(), this.dataElement1.path.toString()]
            predecessors ==~ []
        }

        //cleanup:
        // TODO: endpoint - clear all

        // TODO: where - async yes/no
    }
}
