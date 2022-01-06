package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd


import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDBusinessDefinition
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary

@Slf4j
@Transactional
class BusinessDefinitionService extends DataDictionaryComponentService<Term, NhsDDBusinessDefinition> {

    @Override
    def show(String branch, String id) {
        Term businessDefinition = termService.get(id)

        String description = convertLinksInDescription(branch, businessDefinition.description)
        String shortDesc = replaceLinksInShortDescription(getShortDescription(businessDefinition, null))
        def result = [
                catalogueId: businessDefinition.id.toString(),
                name: businessDefinition.code,
                stereotype: "businessDefinition",
                shortDescription: shortDesc,
                description: description,
                alsoKnownAs: getAliases(businessDefinition)
        ]
        return result
    }

    @Override
    Set<Term> getAll(UUID versionedFolderId, boolean includeRetired = false) {

        Terminology busDefTerminology = nhsDataDictionaryService.getBusinessDefinitionTerminology(versionedFolderId)

        List<Term> terms = termService.findAllByTerminologyId(busDefTerminology.id)

        terms.findAll {term ->
            includeRetired || !catalogueItemIsRetired(term)
        }
    }

    @Override
    Term getItem(UUID id) {
        termService.get(id)
    }

    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".NHS business definition"
    }

    @Override
    NhsDDBusinessDefinition getNhsDataDictionaryComponentFromCatalogueItem(Term catalogueItem, NhsDataDictionary dataDictionary) {
        NhsDDBusinessDefinition businessDefinition = new NhsDDBusinessDefinition()
        nhsDataDictionaryComponentFromItem(catalogueItem, businessDefinition)
        return businessDefinition
    }

    void persistBusinessDefinitions(NhsDataDictionary dataDictionary,
                                    VersionedFolder dictionaryFolder, String currentUserEmailAddress) {

        Terminology terminology = new Terminology(
            label: NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME,
            folder: dictionaryFolder,
            createdBy: currentUserEmailAddress,
            authority: authorityService.defaultAuthority)

        Map<String, Term> allTerms = [:]
        dataDictionary.businessDefinitions.each {name, businessDefinition ->

            // Either this is the first time we've seen a term...
            // .. or if we've already got one, we'll overwrite it with this one
            // (if this one isn't retired)
            if(!allTerms[name] || !businessDefinition.isRetired()) {

                Term term = new Term(
                    code: name,
                    label: name,
                    definition: name,
                    // Leave Url blank for now
                    // url: businessDefinition.otherProperties["ddUrl"].replaceAll(" ", "%20"),
                    description: businessDefinition.definition,
                    createdBy: currentUserEmailAddress,
                    depth: 1,
                    terminology: terminology)

                addMetadataFromComponent(term, businessDefinition, currentUserEmailAddress)

                allTerms[name] = term
            }
        }
        allTerms.values().each { term ->
            terminology.addToTerms(term)
        }

        if (terminology.validate()) {
            terminology = terminologyService.saveModelWithContent(terminology)
        } else {
            GormUtils.outputDomainErrors(messageSource, terminology) // TODO throw exception???
        }


    }

}
