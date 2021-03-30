package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd


import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import grails.gorm.transactions.Transactional
import groovy.util.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

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
        Terminology supDefTerminology = terminologyService.findCurrentMainBranchByLabel(DataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME)
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
        DDHelperFunctions.metadataNamespace + ".Supporting information"
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

}
