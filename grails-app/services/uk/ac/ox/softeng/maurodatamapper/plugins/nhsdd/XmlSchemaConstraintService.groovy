package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary

@Slf4j
@Transactional
class XmlSchemaConstraintService extends DataDictionaryComponentService <Term> {

    @Override
    Map indexMap(Term catalogueItem) {
        [
            catalogueId: catalogueItem.id.toString(),
            name: catalogueItem.code,
            stereotype: "xmlSchemaConstraint"

        ]
    }

    @Override
    def show(String branch, String id) {
        Term xmlSchemaConstraint = termService.get(id)

        String description = convertLinksInDescription(branch, xmlSchemaConstraint.description)

        String shortDesc = replaceLinksInShortDescription(getShortDescription(xmlSchemaConstraint, null))

        def result = [
                catalogueId: xmlSchemaConstraint.id.toString(),
                name: xmlSchemaConstraint.code,
                stereotype: "xmlSchemaConstraint",
                shortDescription: shortDesc,
                description: description,
                alsoKnownAs: getAliases(xmlSchemaConstraint)

        ]
        return result
    }

    @Override
    Set<Term> getAll() {
        Terminology supDefTerminology = terminologyService.findCurrentMainBranchByLabel(DataDictionary.XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME)
        supDefTerminology.terms.findAll{
            (!it.metadata.find{md -> md.namespace == DDHelperFunctions.metadataNamespace + ".XML schema constraint" &&
                        md.key == "isRetired" &&
                        md.value == "true"
                })}

    }

    @Override
    Term getItem(UUID id) {
        termService.get(id)
    }

    @Override
    String getMetadataNamespace() {
        DDHelperFunctions.metadataNamespace + ".XML schema constraint"
    }

    String getShortDescription(Term catalogueItem, DataDictionary dataDictionary) {
        return catalogueItem.code
    }

    void ingestFromXml(def xml, Folder dictionaryFolder, DataModel coreDataModel, String currentUserEmailAddress,
                       NhsDataDictionary nhsDataDictionary) {

        Terminology terminology = new Terminology(
            label: NhsDataDictionary.XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME,
            folder: dictionaryFolder,
            createdBy: currentUserEmailAddress,
            authority: authorityService.defaultAuthority)

        Map<String, Term> allTerms = [:]
        xml.DDXmlSchemaConstraint.each {ddXmlSchemaConstraint ->

            String code = ddXmlSchemaConstraint."class".TitleCaseName[0].text()
            // Either this is the first time we've seen a term...
            // .. or if we've already got one, we'll overwrite it with this one
            // (if this one isn't retired)
            if (!allTerms[code] || ddXmlSchemaConstraint."class".isRetired.text() != "true") {

                Term term = new Term(
                    code: code,
                    label: code,
                    definition: code,
                    url: ddXmlSchemaConstraint."class".DD_URL."class".text().replaceAll(" ", "%20"),
                    description: DDHelperFunctions.parseHtml(ddXmlSchemaConstraint.definition),
                    createdBy: currentUserEmailAddress,
                    depth: 1,
                    terminology: terminology)
                addMetadataFromXml(term, ddXmlSchemaConstraint.class, currentUserEmailAddress)
                allTerms[code] = term
            }
        }

        allTerms.values().each {term ->
            terminology.addToTerms(term)
        }

        if (terminology.validate()) {
            terminology = terminologyService.saveModelWithContent(terminology)
        } else {
            log.debug(terminology.errors)
        }
    }
}