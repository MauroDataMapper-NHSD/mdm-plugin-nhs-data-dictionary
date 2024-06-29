package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.services

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.GraphService
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.IntegrationTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.ReferencedItemsService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
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
    DataClass dataClass1
    DataElement dataElement1
    DataModel dataModel2
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
        PrimitiveType dataType1 = given."there is a primitive data type"("dataType1", dataModel1)
        dataClass1 = given."there is a data class"("Data Class 1", dataModel1)
        dataElement1 = given."there is a data element"("Data Element 1", dataModel1, dataClass1, dataType1)

        dataModel2 = given."there is a data model"("Data Model 2", dictionaryBranch)
        PrimitiveType dataType2 = given."there is a primitive data type"("dataType2", dataModel2)
        dataClass2 = given."there is a data class"("Data Class 2", dataModel2)
        dataElement2 = given."there is a data element"("Data Element 2", dataModel2, dataClass2, dataType2)

        terminology1 = given."there is a terminology"("Terminology 1", dictionaryBranch)
        term1 = given."there is a term"("Term 1", "Term 1", terminology1)
    }

    void "should build the graph node of a new data class"() {
        given: "there is initial test data"
        setupData()

        when: "adding a new data class"
        String description = """<a href=\"$dataModel1.path\">Model</a>, 
<a href=\"$dataClass1.path\">Class</a>, 
<a href=\"$dataElement1.path\">Element</a>, 
<a href=\"https://www.google.com\">Website</a>"""
        
        DataClass newDataClass = given."there is a data class"("New Data Class", dataModel2, dataClass2, description)

        then:
        with {
            newDataClass
        }
    }
}
