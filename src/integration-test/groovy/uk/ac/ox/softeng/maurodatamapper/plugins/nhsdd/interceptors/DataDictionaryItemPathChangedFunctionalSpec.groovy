package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.interceptors

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.BaseDataDictionaryFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import spock.lang.Shared
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary

import static io.micronaut.http.HttpStatus.OK

@Integration
@Slf4j
@Rollback
class DataDictionaryItemPathChangedFunctionalSpec extends BaseDataDictionaryFunctionalSpec {
    // Trace the item layout of a test data dictionary
    @Shared
    VersionedFolder dictionaryBranch
    @Shared
    Folder dataSetsFolder
    @Shared
    Folder dataSetsSubFolder
    @Shared
    DataModel dataSetModel
    @Shared
    PrimitiveType dataSetType
    @Shared
    DataClass dataSetClass
    @Shared
    DataElement dataSetElement
    @Shared
    DataModel nhsClassesAndAttributes
    @Shared
    PrimitiveType nhsClassesAndAttributesType
    @Shared
    DataClass nhsClass
    @Shared
    DataElement nhsAttribute
    @Shared
    Terminology nhsBusinessDefinitions
    @Shared
    Term nhsBusinessTerm

    @Shared
    String nhsClassDescription
    @Shared
    String nhsAttributeDescription
    @Shared
    String nhsBusinessTermDescription

    @RunOnce
    @Transactional
    def setup() {
        dictionaryBranch = given."there is a versioned folder"("NHS Data Dictionary")

        // Data Sets
        dataSetsFolder = given."there is a folder"(NhsDataDictionary.DATA_SETS_FOLDER_NAME, "", dictionaryBranch)
        dataSetsSubFolder = given."there is a folder"("Sample Data Sets", "", dataSetsFolder)
        dataSetModel = given."there is a data model"("Critical Care Data Set", dataSetsSubFolder)
        dataSetType = given."there is a primitive data type"("dataSetType", dataSetModel)
        dataSetClass = given."there is a data class"("Data Set Table", dataSetModel)
        dataSetElement = given."there is a data element"("Data Set Element", dataSetModel, dataSetClass, dataSetType)

        // Classes and Attributes
        nhsClassesAndAttributes = given."there is a data model"(NhsDataDictionary.CLASSES_MODEL_NAME, dictionaryBranch)
        nhsClassesAndAttributesType = given."there is a primitive data type"("classesAndAttributesType", nhsClassesAndAttributes)
        nhsClass = given."there is a data class"("APPOINTMENT", nhsClassesAndAttributes)
        nhsAttribute = given."there is a data element"("APPOINTMENT DATE", nhsClassesAndAttributes, nhsClass, nhsClassesAndAttributesType)

        // NHS Business Definitions
        nhsBusinessDefinitions = given."there is a terminology"(NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME, dictionaryBranch)
        nhsBusinessTerm = given."there is a term"("ABO System", "ABO System", nhsBusinessDefinitions)

        // Setup up a triangular graph:
        // - NHS Class --> NHS Business Term
        // - NHS Business Term --> NHS Attribute
        // - NHS Attribute --> NHS Class
        String nhsClassPath = getPathStringWithoutBranchName(nhsClass.path, dictionaryBranch.branchName)
        String nhsAttributePath = getPathStringWithoutBranchName(nhsAttribute.path, dictionaryBranch.branchName)
        String nhsBusinessTermPath = getPathStringWithoutBranchName(nhsBusinessTerm.path, dictionaryBranch.branchName)

        nhsClassDescription = "Refers to <a href=\"$nhsBusinessTermPath\">something</a>"
        nhsAttributeDescription = "Refers to <a href=\"$nhsClassPath\">something</a>"
        nhsBusinessTermDescription = "Refers to <a href=\"$nhsAttributePath\">something</a>"

        nhsClass.description = nhsClassDescription
        nhsClass.save([flush: true])

        nhsAttribute.description = nhsAttributeDescription
        nhsAttribute.save([flush: true])

        nhsBusinessTerm.description = nhsBusinessTermDescription
        nhsBusinessTerm.save([flush: true])

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug("Cleanup specification")
        cleanUpResources(DataModel, PrimitiveType, DataClass, DataElement, Terminology, Term, Folder, VersionedFolder)
    }

    private <T extends MdmDomain & InformationAware> T getItemToTrack(String domainType) {
        if (domainType == "dataClasses") {
            return nhsClass as T
        }

        if (domainType == "dataElements") {
            return nhsAttribute as T
        }

        if (domainType == "terms") {
            return nhsBusinessTerm as T
        }

        throw new Exception("Unknown domain type: $domainType")
    }

    String getOriginalDescription(String domainType) {
        if (domainType == "dataClasses") {
            return nhsClassDescription
        }

        if (domainType == "dataElements") {
            return nhsAttributeDescription
        }

        if (domainType == "terms") {
            return nhsBusinessTermDescription
        }

        throw new Exception("Unknown domain type: $domainType")
    }

    void "should automatically update the descriptions of predecessor items when a path has changed for #domainType"(
        String domainType,
        String referencedDomainType,
        Closure getUpdateEndpoint,
        Closure getReferencedEndpoint) {
        given: "a user is logged in"
        loginUser('admin@maurodatamapper.com', 'password')

        when: "the full graph is rebuilt"
        PUT("nhsdd/$dictionaryBranch.id/graph", [:], MAP_ARG, true)

        then: "the response status is correct"
        verifyResponse(OK, response)

        and: "the predecessors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/dataClasses/$nhsClass.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ [this.nhsBusinessTerm.path.toString()]
            predecessors ==~ [this.nhsAttribute.path.toString()]
        }

        and: "the predecessors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/dataElements/$nhsAttribute.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ [this.nhsClass.path.toString()]
            predecessors ==~ [this.nhsBusinessTerm.path.toString()]
        }

        and: "the predecessors are correct"
        GET("nhsdd/$dictionaryBranch.id/graph/terms/$nhsBusinessTerm.id", MAP_ARG, true)
        verifyResponse(OK, response)
        verifyAll(responseBody()) {
            successors ==~ [this.nhsAttribute.path.toString()]
            predecessors ==~ [this.nhsClass.path.toString()]
        }

        when: "an item is renamed"
        def itemToRename = getItemToTrack(domainType)
        def referencedItem = getItemToTrack(referencedDomainType)

        String originalLabel = itemToRename.label
        String updatedLabel = "$originalLabel MODIFIED"

        String updateEndpoint = getUpdateEndpoint(itemToRename.id)
        Map updateRequestBody = buildUpdateItemRequestBody(domainType, updatedLabel)
        PUT(updateEndpoint, updateRequestBody, MAP_ARG, true)

        then: "the response is correct"
        verifyResponse(OK, response)

        and: "the response has object values"
        def updatedResponseBody = responseBody()
        String updatedItemId = updatedResponseBody.id
        String updatedItemPath = updatedResponseBody.path
        verifyAll {
            updatedItemId
            updatedItemPath
            updatedItemId == itemToRename.id.toString()
            updatedItemPath != itemToRename.path.toString()
        }

        and: "wait for the async job to finish"
        waitForLastAsyncJobToComplete()

        when: "the referenced item is fetched"
        String referencedItemEndpoint = getReferencedEndpoint(referencedItem.id)
        GET(referencedItemEndpoint, MAP_ARG, true)

        then: "the response is correct"
        verifyResponse(OK, response)

        and: "the links in the referenced item have been updated"
        String expectedUpdatedPath = getPathStringWithoutBranchName(itemToRename.path, dictionaryBranch.branchName).replace(originalLabel, updatedLabel)
        String expectedUpdatedDescription = "Refers to <a href=\"$expectedUpdatedPath\">something</a>"

        def referencedItemResponseBody = responseBody()
        verifyAll(referencedItemResponseBody) {
            description == expectedUpdatedDescription
        }

        cleanup:
        // Put the label back to how it was
        log.info("Cleanup of label and description")
        String cleanupUpdateEndpoint = getUpdateEndpoint(itemToRename.id)
        Map cleanupUpdateRequestBody = buildUpdateItemRequestBodyWithDescription(
            domainType,
            originalLabel,
            getOriginalDescription(domainType))
        PUT(cleanupUpdateEndpoint, cleanupUpdateRequestBody, MAP_ARG, true)
        verifyResponse(OK, response)
        waitForLastAsyncJobToComplete()

        where:
        domainType      | referencedDomainType  | getUpdateEndpoint                                                                             | getReferencedEndpoint
        "dataClasses"   | "dataElements"        | { id -> "dataModels/$nhsClassesAndAttributes.id/dataClasses/$id" }                            | { id -> "dataModels/$nhsClassesAndAttributes.id/dataClasses/$nhsClass.id/dataElements/$id" }
        "dataElements"  | "terms"               | { id -> "dataModels/$nhsClassesAndAttributes.id/dataClasses/$nhsClass.id/dataElements/$id" }  | { id -> "terminologies/$nhsBusinessDefinitions.id/terms/$id" }
        "terms"         | "dataClasses"         | { id -> "terminologies/$nhsBusinessDefinitions.id/terms/$id" }                                | { id -> "dataModels/$nhsClassesAndAttributes.id/dataClasses/$id" }
    }

    Map buildUpdateItemRequestBody(
        String domainType,
        String label) {
        if (domainType == "terms") {
            return [
                code: label,
                definition: label
            ]
        }

        return [
            label: label
        ]
    }

    Map buildUpdateItemRequestBodyWithDescription(
        String domainType,
        String label,
        String description) {
        if (domainType == "terms") {
            return [
                code: label,
                definition: label,
                description: description
            ]
        }

        return [
            label: label,
            description: description
        ]
    }
}
