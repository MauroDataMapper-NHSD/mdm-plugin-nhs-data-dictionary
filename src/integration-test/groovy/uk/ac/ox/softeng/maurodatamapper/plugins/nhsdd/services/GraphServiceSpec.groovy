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
class GraphServiceSpec extends BaseIntegrationSpec {
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
        GraphNode newDataClassGraphNode = sut.getGraphNode(newItem)
        verifyAll {
            newDataClassGraphNode
            newDataClassGraphNode.hasSuccessor(dataModel1.path)
            newDataClassGraphNode.hasSuccessor(dataClass1.path)
            newDataClassGraphNode.hasSuccessor(dataElement1.path)
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
}
