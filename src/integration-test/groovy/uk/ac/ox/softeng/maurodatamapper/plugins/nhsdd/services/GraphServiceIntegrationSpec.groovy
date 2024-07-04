package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.services

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.GraphNode
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.GraphService
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.IntegrationTestGivens
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.springframework.beans.factory.annotation.Autowired

@Integration
@Slf4j
@Rollback
class GraphServiceIntegrationSpec extends BaseIntegrationSpec {
    private GraphService sut

    IntegrationTestGivens given

    User testUser

    // Trace the item layout of a test data dictionary
    VersionedFolder dictionaryBranch
    DataModel dataModel1
    PrimitiveType dataType1
    DataClass dataClass1
    DataElement dataElement1
    DataModel dataModel2
    PrimitiveType dataType2
    DataClass dataClass2
    DataElement dataElement2
    Terminology terminology1
    Term term1

    @Autowired
    PathService pathService

    def setup() {
        given = new IntegrationTestGivens(messageSource)

        sut = new GraphService()
        sut.pathService = pathService
    }

    @Override
    void setupDomainData() {
        testUser = given."there is a user"()

        dictionaryBranch = given."there is a versioned folder"("NHS Data Dictionary")

        folder = given."there is a folder"('test folder', "", dictionaryBranch)

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
    }

    <T extends MdmDomain & InformationAware & MetadataAware & GormEntity> T createNewItem(
        String domainType,
        String description) {
        if (domainType == "dataModel") {
            return given."there is a data model"("New Data Model", dictionaryBranch, description) as T
        }

        if (domainType == "dataClass") {
            return given."there is a data class"(
                "New Data Class",
                dataModel2,
                dataClass2,
                description) as T
        }

        if (domainType == "dataElement") {
            return given."there is a data element"(
                "New Data Element",
                dataModel2,
                dataClass2,
                dataType2,
                description) as T
        }

        if (domainType == "term") {
            return given."there is a term"(
                "New Term",
                "New Term",
                terminology1,
                description) as T
        }

        if (domainType == "folder") {
            return given."there is a folder"("New Folder", description, dictionaryBranch) as T
        }

        throw new Exception("Unrecognised domain type '$domainType'")
    }

    <T extends MdmDomain & InformationAware & MetadataAware & GormEntity> T getItemToUpdateOrDelete(String domainType) {
        if (domainType == "dataModel") {
            return dataModel2 as T
        }

        if (domainType == "dataClass") {
            return dataClass2 as T
        }

        if (domainType == "dataElement") {
            return dataElement2 as T
        }

        if (domainType == "term") {
            return term1 as T
        }

        if (domainType == "folder") {
            return folder as T
        }

        throw new Exception("Unrecognised domain type '$domainType'")
    }

    void "should not build the graph node of a #domainType when there is no description"(String domainType, String description) {
        given: "there is initial test data"
        setupData()

        and: "a new item is created"
        def newItem = createNewItem(domainType, description)

        when: "building the graph"
        sut.buildGraphNode(dictionaryBranch, newItem)

        then: "the graph node is empty"
        GraphNode newItemGraphNode = sut.getGraphNode(newItem)
        verifyAll {
            newItemGraphNode
            newItemGraphNode.successors.empty
            newItemGraphNode.predecessors.empty
        }

        where:
        domainType      | description
        "dataModel"     | null
        "dataClass"     | null
        "dataElement"   | null
        "term"          | null
        "folder"        | null
        "dataModel"     | ""
        "dataClass"     | ""
        "dataElement"   | ""
        "term"          | ""
        "folder"        | ""
    }

    void "should build the graph node of a new #domainType"(String domainType) {
        given: "there is initial test data"
        setupData()

        and: "a description is prepared"
        String description = """<a href=\"$dataModel1.path\">Model</a>, 
<a href=\"$dataClass1.path\">Class</a>, 
<a href=\"$dataElement1.path\">Element</a>, 
<a href=\"https://www.google.com\">Website</a>"""

        and: "a new item is created"
        def newItem = createNewItem(domainType, description)

        when: "building the graph"
        sut.buildGraphNode(dictionaryBranch, newItem)

        then: "successors were updated"
        GraphNode newItemGraphNode = sut.getGraphNode(newItem)
        verifyAll {
            newItemGraphNode
            newItemGraphNode.hasSuccessor(dataModel1.path)
            newItemGraphNode.hasSuccessor(dataClass1.path)
            newItemGraphNode.hasSuccessor(dataElement1.path)
        }

        and: "predecessors were updated"
        GraphNode dataModel1GraphNode = sut.getGraphNode(dataModel1)
        GraphNode dataClass1GraphNode = sut.getGraphNode(dataClass1)
        GraphNode dataElement1GraphNode = sut.getGraphNode(dataElement1)
        verifyAll {
            dataModel1GraphNode
            dataClass1GraphNode
            dataElement1GraphNode
            dataModel1GraphNode.hasPredecessor(newItem.path)
            dataClass1GraphNode.hasPredecessor(newItem.path)
            dataElement1GraphNode.hasPredecessor(newItem.path)
        }

        where:
        domainType      | _
        "dataModel"     | _
        "dataClass"     | _
        "dataElement"   | _
        "term"          | _
        "folder"        | _
    }

    void "should build the graph node when updating a #domainType"(String domainType) {
        given: "there is initial test data"
        setupData()
        def updateItem = getItemToUpdateOrDelete(domainType)

        when: "a description is originally set"
        String originalDescription = """<a href=\"$dataModel1.path\">Model</a>, 
<a href=\"$dataClass1.path\">Class</a>"""
        updateItem.description = originalDescription
        updateItem.save([flush: true])

        and: "an existing graph node was built"
        sut.buildGraphNode(dictionaryBranch, updateItem)

        then: "successors are configured"
        GraphNode updateItemGraphNode = sut.getGraphNode(updateItem)
        verifyAll {
            updateItemGraphNode
            updateItemGraphNode.hasSuccessor(dataModel1.path)
            updateItemGraphNode.hasSuccessor(dataClass1.path)
            !updateItemGraphNode.hasSuccessor(dataElement1.path)
        }

        and: "predecessors are configured"
        GraphNode dataModel1GraphNode = sut.getGraphNode(dataModel1)
        GraphNode dataClass1GraphNode = sut.getGraphNode(dataClass1)
        GraphNode dataElement1GraphNode = sut.getGraphNode(dataElement1)
        verifyAll {
            dataModel1GraphNode
            dataClass1GraphNode
            dataElement1GraphNode
            dataModel1GraphNode.hasPredecessor(updateItem.path)
            dataClass1GraphNode.hasPredecessor(updateItem.path)
            !dataElement1GraphNode.hasPredecessor(updateItem.path)
        }

        when: "the description is changed"
        // Keep one path (model), add a new one (element), remove another (class)
        String updatedDescription = """<a href=\"$dataModel1.path\">Model</a>, 
<a href=\"$dataElement1.path\">Element</a>"""
        updateItem.description = updatedDescription
        updateItem.save([flush: true])

        and: "the graph node is rebuilt"
        sut.buildGraphNode(dictionaryBranch, updateItem)

        then: "successors were updated"
        GraphNode updateItemGraphNodeUpdated = sut.getGraphNode(updateItem)
        verifyAll {
            updateItemGraphNodeUpdated
            updateItemGraphNodeUpdated.hasSuccessor(dataModel1.path)
            !updateItemGraphNodeUpdated.hasSuccessor(dataClass1.path)
            updateItemGraphNodeUpdated.hasSuccessor(dataElement1.path)
        }

        and: "predecessors were updated"
        GraphNode dataModel1GraphNodeUpdated = sut.getGraphNode(dataModel1)
        GraphNode dataClass1GraphNodeUpdated = sut.getGraphNode(dataClass1)
        GraphNode dataElement1GraphNodeUpdated = sut.getGraphNode(dataElement1)
        verifyAll {
            dataModel1GraphNodeUpdated
            dataClass1GraphNodeUpdated
            dataElement1GraphNodeUpdated
            dataModel1GraphNodeUpdated.hasPredecessor(updateItem.path)
            !dataClass1GraphNodeUpdated.hasPredecessor(updateItem.path)
            dataElement1GraphNodeUpdated.hasPredecessor(updateItem.path)
        }

        where:
        domainType      | _
        "dataModel"     | _
        "dataClass"     | _
        "dataElement"   | _
        "term"          | _
        "folder"        | _
    }

    void "should update the successor graph nodes when deleting a #domainType"(String domainType) {
        given: "there is initial test data"
        setupData()
        def deleteItem = getItemToUpdateOrDelete(domainType)

        when: "a description is originally set"
        String description = """<a href=\"$dataModel1.path\">Model</a>, 
<a href=\"$dataClass1.path\">Class</a>, 
<a href=\"$dataElement1.path\">Element</a>, 
<a href=\"https://www.google.com\">Website</a>"""
        deleteItem.description = description
        deleteItem.save([flush: true])

        and: "an existing graph node was built"
        sut.buildGraphNode(dictionaryBranch, deleteItem)

        then: "successors are configured"
        GraphNode updateItemGraphNode = sut.getGraphNode(deleteItem)
        verifyAll {
            updateItemGraphNode
            updateItemGraphNode.hasSuccessor(dataModel1.path)
            updateItemGraphNode.hasSuccessor(dataClass1.path)
            updateItemGraphNode.hasSuccessor(dataElement1.path)
        }

        and: "predecessors are configured"
        GraphNode dataModel1GraphNode = sut.getGraphNode(dataModel1)
        GraphNode dataClass1GraphNode = sut.getGraphNode(dataClass1)
        GraphNode dataElement1GraphNode = sut.getGraphNode(dataElement1)
        verifyAll {
            dataModel1GraphNode
            dataClass1GraphNode
            dataElement1GraphNode
            dataModel1GraphNode.hasPredecessor(deleteItem.path)
            dataClass1GraphNode.hasPredecessor(deleteItem.path)
            dataElement1GraphNode.hasPredecessor(deleteItem.path)
        }

        when: "the item is removed"
        // Need to simulate a "deletion" of a Mauro item without actually deleting it.
        // Before the item is deleted, the graph nodes of the successors must be updated, which
        // is what this test proves. If properly deleting the item, then the graph node pointing to
        // the successors will be lost, so this must happen first
        String originalPath = deleteItem.path.toString()
        GraphNode deleteItemGraphNodeBeforeDelete = sut.getGraphNode(deleteItem)
        sut.removePredecessorFromSuccessorGraphNodes(dictionaryBranch, originalPath, deleteItemGraphNodeBeforeDelete)
        // Technically in real life the Mauro item would have been deleted after this so the graph node would
        // also have been deleted - but we need it still for this test to track the end state for assertions
        sut.saveGraphNode(deleteItem, deleteItemGraphNodeBeforeDelete)

        then: "successors are updated"
        GraphNode deleteItemGraphNodeAfterDelete = sut.getGraphNode(deleteItem)
        verifyAll {
            deleteItemGraphNodeAfterDelete
            deleteItemGraphNodeAfterDelete.successors.empty
        }

        and: "predecessors are updated"
        GraphNode dataModel1GraphNodeUpdated = sut.getGraphNode(dataModel1)
        GraphNode dataClass1GraphNodeUpdated = sut.getGraphNode(dataClass1)
        GraphNode dataElement1GraphNodeUpdated = sut.getGraphNode(dataElement1)
        verifyAll {
            dataModel1GraphNodeUpdated
            dataClass1GraphNodeUpdated
            dataElement1GraphNodeUpdated
            !dataModel1GraphNodeUpdated.hasPredecessor(deleteItem.path)
            !dataClass1GraphNodeUpdated.hasPredecessor(deleteItem.path)
            !dataElement1GraphNodeUpdated.hasPredecessor(deleteItem.path)
        }

        where:
        domainType      | _
        "dataModel"     | _
        "dataClass"     | _
        "dataElement"   | _
        "term"          | _
        "folder"        | _
    }

    void "should update the successor graph nodes when moving or renaming a #domainType"(String domainType) {
        given: "there is initial test data"
        setupData()
        def updateItem = getItemToUpdateOrDelete(domainType)

        when: "a description is originally set"
        String description = """<a href=\"$dataModel1.path\">Model</a>, 
<a href=\"$dataClass1.path\">Class</a>, 
<a href=\"$dataElement1.path\">Element</a>, 
<a href=\"https://www.google.com\">Website</a>"""
        updateItem.description = description
        updateItem.save([flush: true])

        and: "an existing graph node was built"
        sut.buildGraphNode(dictionaryBranch, updateItem)

        then: "successors are configured"
        GraphNode updateItemGraphNode = sut.getGraphNode(updateItem)
        verifyAll {
            updateItemGraphNode
            updateItemGraphNode.hasSuccessor(dataModel1.path)
            updateItemGraphNode.hasSuccessor(dataClass1.path)
            updateItemGraphNode.hasSuccessor(dataElement1.path)
        }

        and: "predecessors are configured"
        GraphNode dataModel1GraphNode = sut.getGraphNode(dataModel1)
        GraphNode dataClass1GraphNode = sut.getGraphNode(dataClass1)
        GraphNode dataElement1GraphNode = sut.getGraphNode(dataElement1)
        verifyAll {
            dataModel1GraphNode
            dataClass1GraphNode
            dataElement1GraphNode
            dataModel1GraphNode.hasPredecessor(updateItem.path)
            dataClass1GraphNode.hasPredecessor(updateItem.path)
            dataElement1GraphNode.hasPredecessor(updateItem.path)
        }

        when: "changing the path to an item"
        String originalPath = updateItem.path.toString()
        // Change the label (or code for Terms) of a Mauro item will change it's path
        if (domainType == "term") {
            updateItem.code = "Renamed code"
        }
        else {
            updateItem.label = "Renamed item"
        }
        updateItem.save([flush: true])
        verifyAll {
            originalPath != updateItem.path.toString()
        }

        and: "updating the graph nodes"
        GraphNode updateItemGraphNodeBeforeUpdate = sut.getGraphNode(updateItem)
        sut.updatePredecessorFromSuccessorGraphNodes(
            dictionaryBranch,
            originalPath,
            updateItem.path.toString(),
            updateItemGraphNodeBeforeUpdate)

        then: "successors are unchanged"
        GraphNode updateItemGraphNodeAfterUpdate = sut.getGraphNode(updateItem)
        verifyAll {
            updateItemGraphNodeAfterUpdate
            updateItemGraphNodeAfterUpdate.hasSuccessor(dataModel1.path)
            updateItemGraphNodeAfterUpdate.hasSuccessor(dataClass1.path)
            updateItemGraphNodeAfterUpdate.hasSuccessor(dataElement1.path)
        }

        and: "predecessors are updated"
        GraphNode dataModel1GraphNodeUpdated = sut.getGraphNode(dataModel1)
        GraphNode dataClass1GraphNodeUpdated = sut.getGraphNode(dataClass1)
        GraphNode dataElement1GraphNodeUpdated = sut.getGraphNode(dataElement1)
        verifyAll {
            dataModel1GraphNodeUpdated
            dataClass1GraphNodeUpdated
            dataElement1GraphNodeUpdated
            !dataModel1GraphNodeUpdated.hasPredecessor(originalPath)
            !dataClass1GraphNodeUpdated.hasPredecessor(originalPath)
            !dataElement1GraphNodeUpdated.hasPredecessor(originalPath)
            dataModel1GraphNodeUpdated.hasPredecessor(updateItem.path)
            dataClass1GraphNodeUpdated.hasPredecessor(updateItem.path)
            dataElement1GraphNodeUpdated.hasPredecessor(updateItem.path)
        }

        where:
        domainType      | _
        "dataModel"     | _
        "dataClass"     | _
        "dataElement"   | _
        "term"          | _
        "folder"        | _
    }
}
