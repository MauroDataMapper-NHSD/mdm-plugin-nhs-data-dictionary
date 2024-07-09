package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.interceptors

import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.GraphService
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.ACCEPTED
import static io.micronaut.http.HttpStatus.OK

@Integration
@Slf4j
@Rollback
class DataDictionaryItemPathChangedFunctionalSpec extends BaseDataDictionaryItemTrackerFunctionalSpec {
    @Override
    @Transactional
    void postSetup() {
        // Setup up a triangular graph:
        // - NHS Class --> NHS Business Term
        // - NHS Business Term --> NHS Attribute
        // - NHS Attribute --> NHS Class
        String nhsClassPath = getPathStringWithoutBranchName(nhsClass.path, dictionaryBranch.branchName)
        String nhsAttributePath = getPathStringWithoutBranchName(nhsAttribute.path, dictionaryBranch.branchName)
        String nhsBusinessTermPath = getPathStringWithoutBranchName(nhsBusinessTerm.path, dictionaryBranch.branchName)

        String nhsClassDescription = "Refers to <\"$nhsBusinessTermPath\"></a>"
        nhsClass.description = nhsClassDescription
        nhsClass.save([flush: true])

        String nhsAttributeDescription = "Refers to <\"$nhsClassPath\"></a>"
        nhsAttribute.description = nhsAttributeDescription
        nhsAttribute.save([flush: true])

        String nhsBusinessTermDescription = "Refers to <\"$nhsAttributePath\"></a>"
        nhsBusinessTerm.description = nhsBusinessTermDescription
        nhsBusinessTerm.save([flush: true])
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

    void "should automatically update the descriptions of predecessor items when a path has changed for #domainType"(
        String domainType,
        String referencedDomainType,
        Closure getUpdateEndpoint,
        Closure getReferencedEndpoint) {
        given: "a user is logged in"
        loginUser('admin@maurodatamapper.com', 'password')

        when: "the full graph is rebuilt"
        PUT("nhsdd/$dictionaryBranch.id/graph?asynchronous=true", [:], MAP_ARG, true)

        then: "the response status is correct"
        verifyResponse(ACCEPTED, response)

        and: "wait for the async job to finish"
        waitForLastAsyncJobToComplete()

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

        // TODO: wait for a second async job to complete??

        when: "the referenced item is fetched"
        String referencedItemEndpoint = getReferencedEndpoint(referencedItem.id)
        GET(referencedItemEndpoint, MAP_ARG, true)

        then: "the response is correct"
        verifyResponse(OK, response)

        and: "the links in the referenced item have been updated"
        String expectedUpdatedPath = getPathStringWithoutBranchName(itemToRename.path, dictionaryBranch.branchName).replace(originalLabel, updatedLabel)
        String expectedUpdatedDescription = "Refers to <\"$expectedUpdatedPath\"></a>"

        def referencedItemResponseBody = responseBody()
        verifyAll(referencedItemResponseBody) {
            description == expectedUpdatedDescription
        }

        where:
        domainType      | referencedDomainType  | getUpdateEndpoint                                                                             | getReferencedEndpoint
        "dataClasses"   | "dataElements"        | { id -> "dataModels/$nhsClassesAndAttributes.id/dataClasses/$id" }                            | { id -> "dataModels/$nhsClassesAndAttributes.id/dataClasses/$nhsClass.id/dataElements/$id" }
        "dataElements"  | "terms"               | { id -> "dataModels/$nhsClassesAndAttributes.id/dataClasses/$nhsClass.id/dataElements/$id" }  | { id -> "terminologies/$nhsBusinessDefinitions.id/terms/$id" }
        "terms"         | "dataClasses"         | { id -> "terminologies/$nhsBusinessDefinitions.id/terms/$id" }                                | { id -> "dataModels/$nhsClassesAndAttributes.id/dataClasses/$id" }
    }
}
