package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Topic

@Slf4j
class DDWebPage extends DataDictionaryComponent<Term> {


    @Override
    void fromXml(Object xml) {
        definition = xml.definition

        //properties = clazz.property

        super.fromXml(xml)

    }

    @Override
    Term toCatalogueItem(DataDictionary dataDictionary) {

        // List<String> path = DDHelperFunctions.getPath(base_uri, "Supporting_Definitions", "\\.txaClass20")

        String termName = titleCaseName
        Term term = new Term(code: termName,
                definition: DDHelperFunctions.tidyLabel(termName),
                url: ddUrl,
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

/*    String getParentTermName() {
        List<String> path = DDHelperFunctions.getPath(ddUrl, "Supporting_Definitions", "\\.txaClass20")
        return path[0]
    }
*/

    @Override
    void fromCatalogueExport(DataDictionary dataDictionary, def catalogueXml, UUID parentId, UUID containerId) {

        definition = catalogueXml.description.text()

        name = catalogueXml.code

        catalogueId = UUID.fromString(catalogueXml.id.text())
        parentCatalogueId = parentId
        containerCatalogueId = containerId

        super.fromCatalogueExport(dataDictionary, catalogueXml, parentId, containerId, true)


    }

/*    @Override
    void updateDescriptionInCatalogue(BindingMauroDataMapperClient mdmClient) {
        mdmClient.updateTermDescription(containerCatalogueId, catalogueId, definition.toString())
    }
*/
    @Override
    String getDitaKey() {
        if(ddUrl.contains("/messages/")) {
            return "dataset_" + DDHelperFunctions.makeValidDitaName(name)
        } else {
            return "supporting_definition_" + DDHelperFunctions.makeValidDitaName(name)
        }
    }

    @Override
    String getOutputClassForLink() {
        String outputClass
        if(path.contains("/messages/")) {
            outputClass = "dataset"
        } else {
            outputClass = "supportingInformation"
        }

        if(stringValues["isRetired"] == "true") {
            outputClass += " retired"
        }
        return outputClass

    }

    @Override
    String getOutputClassForTable() {
        if(path.contains("/messages/")) {
            return "Data Set"
        } else {
            return "Supporting Information"
        }

    }

    @Override
    String getTypeText() {
        if(path.contains("/messages/")) {
            return "Data Set"
        } else {
            return "Supporting Information"
        }
    }

    @Override
    String getTypeTitle() {
        if(path.contains("/messages/")) {
            return "Data Set"
        } else {
            return "Supporting Information"
        }
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
                return firstSentence
            } catch (Exception e) {
                log.error("Couldn't parse: " + definition)
                return name
            }
        }
    }
}
