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
    String getShortDescription(Term businessDefinition, DataDictionary dataDictionary) {
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

    void ingestFromXml(def xml, Folder dictionaryFolder, DataModel coreDataModel, String currentUserEmailAddress,
                       NhsDataDictionary nhsDataDictionary) {

        Terminology terminology = new Terminology(
                label: NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME,
                folder: dictionaryFolder,
                createdBy: currentUserEmailAddress,
                authority: authorityService.defaultAuthority)

        Map<String, Term> allTerms = [:]
        xml.DDBusinessDefinition.each { ddBusinessDefinition ->

            String code = ddBusinessDefinition.TitleCaseName.text()
            String description = DDHelperFunctions.parseHtml(ddBusinessDefinition.definition)
            // Either this is the first time we've seen a term...
            // .. or if we've already got one, we'll overwrite it with this one
            // (if this one isn't retired)
            if(!allTerms[code] || !ddBusinessDefinition.isRetired.text() == "true") {

                Term term = new Term(
                        code: code,
                        label: code,
                        definition: code,
                        url: ddBusinessDefinition.DD_URL.text().replaceAll(" ", "%20"),
                        description: description,
                        createdBy: currentUserEmailAddress,
                        depth: 1,
                        terminology: terminology)
                addMetadataFromXml(term, ddBusinessDefinition, currentUserEmailAddress)

                String shortDescription = getShortDesc(description, term, nhsDataDictionary)
                addToMetadata(term, "shortDescription", shortDescription, currentUserEmailAddress)

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

    String getShortDesc(String description, Term term, NhsDataDictionary dataDictionary) {
        boolean isPreparatory = term.metadata.any { it.key == "isPreparatory" && it.value == "true" }
        if(isPreparatory) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + description + "</xml>")
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
                log.warn("Couldn't parse description to get shortDesc because {}", e.getMessage())
                log.trace('Unparsable Description:: {}', description)
                return term.label
            }
        }
    }





}
