package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import groovy.util.logging.Slf4j
import groovy.xml.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Topic

@Slf4j
class DDBusinessDefinition extends DataDictionaryComponent<Term> {

    @Override
    void fromXml(Object xml) {
        definition = xml.definition

        super.fromXml(xml)

    }

    @Override
    Term toCatalogueItem(DataDictionary dataDictionary) {

        //List<String> path = DDHelperFunctions.getPath(ddUrl, "nhs_business_definitions", "\\.txaClass20")

        //String termName = path[1]
        Term term = new Term(code: titleCaseName,
                definition: DDHelperFunctions.tidyLabel(titleCaseName),
                url: ddUrl.replaceAll(" ", "%20"),
                description: DDHelperFunctions.parseHtml(definition),
                sourceTermRelationships: [])

        addAllMetadata(term)

        catalogueItem = term
        return term

    }

    @Override
    List<Topic> toDitaTopics(DataDictionary dataDictionary) {


        Topic descriptionTopic = createDescriptionTopic()

        List<Topic> returnTopics = [descriptionTopic]
        Topic aliasTopic = getAliasTopic()
        if(aliasTopic) {
            returnTopics.add(aliasTopic)
        }

        Topic whereUsedTopic = generateWhereUsedTopic(dataDictionary)
        if(whereUsedTopic) {
            returnTopics.add(whereUsedTopic)
        }

        return returnTopics
    }

    String getParentTermName() {
        return titleCaseName.take(1)
    }


    @Override
    void fromCatalogueExport(DataDictionary dataDictionary, def catalogueJson, UUID parentId, UUID containerId) {

        definition = catalogueJson.description

        name = catalogueJson.code

        catalogueId = UUID.fromString(catalogueJson.id.toString())
        parentCatalogueId = parentId
        containerCatalogueId = containerId

        super.fromCatalogueExport(dataDictionary, catalogueJson, parentId, containerId, false)
    }

    @Override
    void fromCatalogueItem(DataDictionary dataDictionary, Term term, UUID parentId, UUID containerId, MetadataService metadataService) {

        //clazz = xml."class"

        definition = term.description
        name = term.code

        catalogueId = term.id
        this.parentCatalogueId = parentId
        this.containerCatalogueId = containerId

        super.fromCatalogueItem(dataDictionary, term, parentId, containerId, metadataService)
        catalogueItem = term

    }

/*    @Override
    void updateDescriptionInCatalogue(BindingMauroDataMapperClient mdmClient) {
        mdmClient.updateTermDescription(containerCatalogueId, catalogueId, definition.toString())
    }
*/
    @Override
    String getDitaKey() {
        return "business_definition_" + DDHelperFunctions.makeValidDitaName(name)
    }

    @Override
    String getOutputClassForLink() {
        String outputClass = "businessDefinition"
        if(stringValues["isRetired"] == "true") {
            outputClass += " retired"
        }
        return outputClass

    }

    @Override
    String getOutputClassForTable() {
        return "NHS Business Definition"
    }

    @Override
    String getTypeText() {
        return "NHS business definition"
    }

    @Override
    String getTypeTitle() {
        return "NHS Business Definition"
    }

    @Override
    String getShortDesc(DataDictionary dataDictionary) {
        if(isPrepatory) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + DDHelperFunctions.parseHtml(definition.toString()) + "</xml>")
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
                log.error("Couldn't parse: " + definition)
                return name
            }
        }
    }

    @Override
    String getInternalLink() {
        return "](te:${DataDictionary.BUSDEF_TERMINOLOGY_NAME}|tm:${name})"
    }



}
