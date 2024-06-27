package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.services

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.IntegrationTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.ReferencedItemsService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Integration
@Slf4j
@Rollback
class ReferencedItemsServiceSpec extends BaseIntegrationSpec {
    private ReferencedItemsService sut

    IntegrationTestGivens given

    User testUser
    VersionedFolder dictionaryBranch

    @Autowired
    PathService pathService

    def setup() {
        given = new IntegrationTestGivens(messageSource)

        sut = new ReferencedItemsService()
        sut.pathService = pathService
    }

    @Override
    void setupDomainData() {
        testUser = given."there is a user"()
        folder = given."there is a folder"('test folder')
        dictionaryBranch = given."there is a versioned folder"("NHS Data Dictionary")
    }

    void "should update the referenced item maps for a data class"() {
        given: "there is initial test data"
        setupData()

        and: "there is a target data model with target items"
        DataModel targetDataModel = given."there is a data model"("Target Data Model", dictionaryBranch)
        PrimitiveType targetDataType = given."there is a primitive data type"("target_string", targetDataModel)
        DataClass targetDataClass = given."there is a data class"("Target Data Class", targetDataModel)
        DataElement targetDataElement = given."there is a data element"("Target Data Element", targetDataModel, targetDataClass, targetDataType)

        and: "there is a source item with a description referencing target items"
        DataModel sourceDataModel = given."there is a data model"("Source Data Model", dictionaryBranch)
        String description = "<a href=\"$targetDataModel.path\">Model</a>, <a href=\"$targetDataClass.path\">Class</a>, <a href=\"$targetDataElement.path\">Element</a>, <a href=\"https://www.google.com\">Website</a>"
        DataClass sourceDataClass = given."there is a data class"("Source Data Class", sourceDataModel, null, description)

        when: "when building the reference maps"
        sut.buildReferenceMapsForSourceAndTargets(dictionaryBranch, sourceDataClass)

        then: "the source item has a target referenced item map"
        Metadata sourceDataClassMetadata = sourceDataClass.findMetadataByNamespaceAndKey(
            ReferencedItemsService.METADATA_NAMESPACE,
            ReferencedItemsService.METADATA_REFERENCES_KEY)

        verifyAll {
            sourceDataClassMetadata
        }

        and: "the source item reference map is correct"
        // TODO

        and: "the target items each have a referenced item map back to the source item"
        Metadata targetDataModelMetadata = targetDataModel.findMetadataByNamespaceAndKey(
            ReferencedItemsService.METADATA_NAMESPACE,
            ReferencedItemsService.METADATA_REFERENCED_IN_KEY)

        Metadata targetDataClassMetadata = targetDataClass.findMetadataByNamespaceAndKey(
            ReferencedItemsService.METADATA_NAMESPACE,
            ReferencedItemsService.METADATA_REFERENCED_IN_KEY)

        Metadata targetDataElementMetadata = targetDataElement.findMetadataByNamespaceAndKey(
            ReferencedItemsService.METADATA_NAMESPACE,
            ReferencedItemsService.METADATA_REFERENCED_IN_KEY)

        verifyAll {
            targetDataModelMetadata
            targetDataClassMetadata
            targetDataElementMetadata
        }

        and: "the target item reference maps are correct"
        // TODO
    }
}
