package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import grails.gorm.transactions.Transactional
import groovy.util.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.DDBusinessDefinition
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

@Transactional
class BusinessDefinitionService extends DataDictionaryComponentService <Term> {

    @Override
    Map indexMap(Term catalogueItem) {
        [
            catalogueId: catalogueItem.id.toString(),
            name: catalogueItem.code,
            stereotype: "businessDefinition"
        ]
    }

    @Override
    def show(String branch, String id) {
        Term businessDefinition = termService.get(id)

        String description = convertLinksInDescription(branch, businessDefinition.description)

        def result = [
                catalogueId: businessDefinition.id.toString(),
                name: businessDefinition.code,
                stereotype: "businessDefinition",
                shortDescription: getShortDescription(businessDefinition),
                description: description,
                alsoKnownAs: getAliases(businessDefinition)
        ]
        return result
    }

    @Override
    Set<Term> getAll() {
        Terminology busDefTerminology = terminologyService.findCurrentMainBranchByLabel(DataDictionary.BUSDEF_TERMINOLOGY_NAME)
        busDefTerminology.terms.findAll{
            (it.depth > 1 &&
                !it.metadata.find{md -> md.namespace == DDHelperFunctions.metadataNamespace + ".NHS business definition" &&
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
        DDHelperFunctions.metadataNamespace + ".NHS business definition"
    }

    @Override
    String getShortDescription(Term businessDefinition) {
        if(isPreparatory(businessDefinition)) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + DDHelperFunctions.parseHtml(businessDefinition.description.toString()) + "</xml>")
                String firstParagraph = xml.p[0].text()
                if (xml.p.size() == 0) {
                    firstParagraph = xml.text()
                }
                String firstSentence = firstParagraph.substring(0, firstParagraph.indexOf(".") + 1)
                if (firstSentence.toLowerCase().contains("is a")) {
                    String secondParagraph = xml.p[1].text()
                    String secondSentence = secondParagraph.substring(0, secondParagraph.indexOf(".") + 1)
                    return secondSentence
                }
                return firstSentence
            } catch (Exception e) {
                log.error("Couldn't parse: " + businessDefinition.description)
                return businessDefinition.code
            }
        }

    }

}
