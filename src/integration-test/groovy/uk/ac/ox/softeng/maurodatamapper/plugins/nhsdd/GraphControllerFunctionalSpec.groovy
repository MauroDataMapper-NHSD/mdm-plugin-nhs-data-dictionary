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

import static io.micronaut.http.HttpStatus.OK

@Integration
@Slf4j
@Rollback
class GraphControllerFunctionalSpec extends BaseDataDictionaryFunctionalSpec {
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

//    @Transactional
//    def cleanupSpec() {
//        log.debug('CleanupSpec GraphControllerFunctionalSpec')
//        cleanUpResources(VersionedFolder)
//    }

    void "should build a graph node for a Mauro item"() {
        given: "an item has a description set"
        String domain = "dataModels"
        String description = """<a href=\"$dataModel1.path\">Model</a>, 
<a href=\"$dataClass1.path\">Class</a>, 
<a href=\"$dataElement1.path\">Element</a>, 
<a href=\"https://www.google.com\">Website</a>"""

        dataModel2.description = description
        dataModel2.save([flush: true])

        when: "the graph node is built"
        PUT("nhsdd/$dictionaryBranch.id/graph/$domain/$dataModel2.id", [:], MAP_ARG, true)

        then: "the response is OK"
        verifyResponse(OK, response)

        and: "the response contains the expected graph node"
        // TODO

        when: "the graph node is fetched"
        GET("nhsdd/$dictionaryBranch.id/graph/$domain/$dataModel2.id", MAP_ARG, true)

        then: "the response is OK"

        and: "the response contains the expected graph node"
        // TODO
    }
}
