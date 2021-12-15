package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

@Slf4j
@Transactional
class SupportingInformationService extends DataDictionaryComponentService <Term> {

    @Override
    Map indexMap(Term catalogueItem) {
        [
            catalogueId: catalogueItem.id.toString(),
            name: catalogueItem.code,
            stereotype: "supportingInformation"

        ]
    }

    @Override
    def show(String branch, String id) {
        Term supportingInformation = termService.get(id)

        String description = convertLinksInDescription(branch, supportingInformation.description)
        String shortDesc = replaceLinksInShortDescription(getShortDescription(supportingInformation, null))
        def result = [
                catalogueId: supportingInformation.id.toString(),
                name: supportingInformation.code,
                stereotype: "supportingInformation",
                shortDescription: shortDesc,
                description: description,
                alsoKnownAs: getAliases(supportingInformation)
        ]
        return result
    }

    @Override
    Set<Term> getAll() {
        Terminology supDefTerminology = terminologyService.findCurrentMainBranchByLabel(NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME)
        supDefTerminology.terms.findAll{
            (!it.metadata.find{md -> md.namespace == getMetadataNamespace() &&
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
        DDHelperFunctions.metadataNamespace + ".supporting information"
    }

    @Override
    String getShortDescription(Term supportingInformation, DataDictionary dataDictionary) {
        if(isPreparatory(supportingInformation)) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + DDHelperFunctions.parseHtml(supportingInformation.description) + "</xml>")
                String firstParagraph = xml.p[0].text()
                if (xml.p.size() == 0) {
                    firstParagraph = xml.text()
                }
                String firstSentence = firstParagraph.substring(0, firstParagraph.indexOf(". ") + 1)
                return firstSentence
            } catch (Exception e) {
                log.error("Couldn't parse: " + supportingInformation)
                return supportingInformation.code
            }
        }
    }

    void ingestFromXml(def xml, Folder dictionaryFolder, DataModel coreDataModel, String currentUserEmailAddress,
                       NhsDataDictionary nhsDataDictionary) {

        Terminology terminology = new Terminology(
            label: NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME,
            folder: dictionaryFolder,
            createdBy: currentUserEmailAddress,
            authority: authorityService.defaultAuthority)

        Map<String, Term> allTerms = [:]
        xml.DDWebPage.findAll { ddWebPage ->
            ddWebPage."base-uri".text().contains("Supporting_Definitions") ||
                ddWebPage."base-uri".text().contains("Supporting_Information") ||
                ddWebPage."base-uri".text().contains("CDS_Supporting_Information")
        }.each { ddWebPage ->

            String code = ddWebPage.TitleCaseName.text()
            // Either this is the first time we've seen a term...
            // .. or if we've already got one, we'll overwrite it with this one
            // (if this one isn't retired)
            if(!allTerms[code] || ddWebPage.isRetired.text() != "true") {

                Term term = new Term(
                    code: code,
                    label: code,
                    definition: code,
                    url: ddWebPage.DD_URL.text().replaceAll(" ", "%20"),
                    description: DDHelperFunctions.parseHtml(ddWebPage.definition),
                    createdBy: currentUserEmailAddress,
                    depth: 1,
                    terminology: terminology)
                addMetadataFromXml(term, ddWebPage, currentUserEmailAddress)
                allTerms[code] = term
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
