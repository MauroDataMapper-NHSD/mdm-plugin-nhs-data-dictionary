package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDXMLSchemaConstraint
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary

@Slf4j
@Transactional
class XmlSchemaConstraintService extends DataDictionaryComponentService<Term, NhsDDXMLSchemaConstraint> {

    @Override
    NhsDDXMLSchemaConstraint show(UUID versionedFolderId, String id) {
        Term xmlSchemaConstraintTerm = termService.get(id)
        NhsDDXMLSchemaConstraint xmlSchemaConstraint = getNhsDataDictionaryComponentFromCatalogueItem(xmlSchemaConstraintTerm, null)
        return xmlSchemaConstraint
    }

    @Override
    Set<Term> getAll(UUID versionedFolderId, boolean includeRetired = false) {

        Terminology xmlSchemaConstraintTerminology = nhsDataDictionaryService.getXmlSchemaConstraintTerminology(versionedFolderId)

        List<Term> terms = termService.findAllByTerminologyId(xmlSchemaConstraintTerminology.id)

        terms.findAll {term ->
            includeRetired || !catalogueItemIsRetired(term)
        }

    }

    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".XML schema constraint"
    }

    @Override
    NhsDDXMLSchemaConstraint getNhsDataDictionaryComponentFromCatalogueItem(Term catalogueItem, NhsDataDictionary dataDictionary) {
        NhsDDXMLSchemaConstraint xMLSchemaConstraint = new NhsDDXMLSchemaConstraint()
        nhsDataDictionaryComponentFromItem(catalogueItem, xMLSchemaConstraint)
        return xMLSchemaConstraint
    }

    void persistXmlSchemaConstraints(NhsDataDictionary dataDictionary,
                                     VersionedFolder dictionaryFolder, String currentUserEmailAddress) {

        Terminology terminology = new Terminology(
            label: NhsDataDictionary.XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME,
            folder: dictionaryFolder,
            createdBy: currentUserEmailAddress,
            authority: authorityService.defaultAuthority)

        Map<String, Term> allTerms = [:]
        dataDictionary.xmlSchemaConstraints.each {name, xmlSchemaConstraint ->

            // Either this is the first time we've seen a term...
            // .. or if we've already got one, we'll overwrite it with this one
            // (if this one isn't retired)
            if(!allTerms[name] || !xmlSchemaConstraint.isRetired()) {

                Term term = new Term(
                    code: name,
                    label: name,
                    definition: name,
                    // Leave Url blank for now
                    // url: businessDefinition.otherProperties["ddUrl"].replaceAll(" ", "%20"),
                    description: xmlSchemaConstraint.definition,
                    createdBy: currentUserEmailAddress,
                    depth: 1,
                    terminology: terminology)

                addMetadataFromComponent(term, xmlSchemaConstraint, currentUserEmailAddress)

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

    NhsDDXMLSchemaConstraint getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        nhsDataDictionary.xmlSchemaConstraints.values().find {
            it.catalogueItem.id == catalogueItemId
        }
    }


}