package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd


import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDSupportingInformation
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary

@Slf4j
@Transactional
class SupportingInformationService extends DataDictionaryComponentService<Term, NhsDDSupportingInformation> {

    @Override
    NhsDDSupportingInformation show(UUID versionedFolderId, String id) {
        Term supportingInformationTerm = termService.get(id)
        NhsDDSupportingInformation supportingInformation = getNhsDataDictionaryComponentFromCatalogueItem(supportingInformationTerm, null)
        return supportingInformation
    }

    @Override
    Set<Term> getAll(UUID versionedFolderId, boolean includeRetired = false) {
        Terminology supInfTerminology = nhsDataDictionaryService.getSupportingDefinitionTerminology(versionedFolderId)
        List<Term> terms = termService.findAllByTerminologyId(supInfTerminology.id)
        terms.findAll {term ->
            includeRetired || !catalogueItemIsRetired(term)
        }
    }

    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".supporting information"
    }

    @Override
    NhsDDSupportingInformation getNhsDataDictionaryComponentFromCatalogueItem(Term catalogueItem, NhsDataDictionary dataDictionary) {
        NhsDDSupportingInformation supportingInformation = new NhsDDSupportingInformation()
        nhsDataDictionaryComponentFromItem(catalogueItem, supportingInformation)
        return supportingInformation
    }

    void persistSupportingInformation(NhsDataDictionary dataDictionary,
                                      VersionedFolder dictionaryFolder, String currentUserEmailAddress) {

        Terminology terminology = new Terminology(
            label: NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME,
            folder: dictionaryFolder,
            createdBy: currentUserEmailAddress,
            authority: authorityService.defaultAuthority)

        Map<String, Term> allTerms = [:]
        dataDictionary.supportingInformation.each {name, supportingInformation ->

            // Either this is the first time we've seen a term...
            // .. or if we've already got one, we'll overwrite it with this one
            // (if this one isn't retired)
            if(!allTerms[name] || !supportingInformation.isRetired()) {

                Term term = new Term(
                    code: name,
                    label: name,
                    definition: name,
                    // Leave Url blank for now
                    // url: businessDefinition.otherProperties["ddUrl"].replaceAll(" ", "%20"),
                    description: supportingInformation.definition,
                    createdBy: currentUserEmailAddress,
                    depth: 1,
                    terminology: terminology)

                addMetadataFromComponent(term, supportingInformation, currentUserEmailAddress)

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

    NhsDDSupportingInformation getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        nhsDataDictionary.supportingInformation.values().find {
            it.catalogueItem.id == catalogueItemId
        }
    }


}
