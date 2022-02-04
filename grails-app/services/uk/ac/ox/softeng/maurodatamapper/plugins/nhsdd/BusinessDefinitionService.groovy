package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDBusinessDefinition
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary

@Slf4j
@Transactional
class BusinessDefinitionService extends DataDictionaryComponentService<Term, NhsDDBusinessDefinition> {

    @Override
    NhsDDBusinessDefinition show(UUID versionedFolderId, String id) {
        Term businessDefinitionTerm = termService.get(id)
        NhsDDBusinessDefinition businessDefinition = getNhsDataDictionaryComponentFromCatalogueItem(businessDefinitionTerm, null)
        return businessDefinition
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

        TreeMap<String, Term> allTerms = new TreeMap<>()
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

        if (terminologyService.validate(terminology)) {
            terminology = terminologyService.saveModelWithContent(terminology)
        } else {
            GormUtils.outputDomainErrors(messageSource, terminology) // TODO throw exception???
        }


    }

    NhsDDBusinessDefinition getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        nhsDataDictionary.businessDefinitions.values().find {
            it.catalogueItem.id == catalogueItemId
        }
    }

}
