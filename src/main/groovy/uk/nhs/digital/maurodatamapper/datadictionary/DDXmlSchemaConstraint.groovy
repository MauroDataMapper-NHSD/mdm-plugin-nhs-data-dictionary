package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Topic

class DDXmlSchemaConstraint extends DataDictionaryComponent<Term> {

    @Override
    void fromXml(Object xml) {
        definition = xml.definition

        super.fromXml(xml)
        titleCaseName = xml."class".TitleCaseName.text()
        stringValues["TitleCaseName"] = titleCaseName

        uin = xml."class".uin.text()
        stringValues["uin"] = uin

        name = xml."class".name.text()
        stringValues["name"] = name

        ddUrl = xml."class".DD_URL.text()
        stringValues["DD_URL"] = ddUrl

    }

    @Override
    Term toCatalogueItem(DataDictionary dataDictionary) {

        // List<String> path = DDHelperFunctions.getPath(base_uri, "Supporting_Definitions", "\\.txaClass20")

        String termName = titleCaseName
        Term term = new Term(code: termName,
                             definition: DDHelperFunctions.tidyLabel(termName),
                             url: stringValues["ultimate-path"],
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
        List<String> path = DDHelperFunctions.getPath(ddUrl, "XML_Schema_Constraints", "\\.txaClass20")
        return path[0]
    }


    @Override
    void fromCatalogueExport(DataDictionary dataDictionary, def catalogueXml, UUID parentId, UUID containerId) {

        definition = catalogueXml.description.text()

        name = catalogueXml.code

        catalogueId = UUID.fromString(catalogueXml.id.text())
        parentCatalogueId = parentId
        containerCatalogueId = containerId

        super.fromCatalogueExport(dataDictionary, catalogueXml, parentId, containerId, true)


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
        return "xml_schema_constraint_" + DDHelperFunctions.makeValidDitaName(name)
    }

    @Override
    String getOutputClassForLink() {
        String outputClass = "xmlSchemaConstraint"
        if(stringValues["isRetired"] == "true") {
            outputClass += " retired"
        }
        return outputClass

    }

    @Override
    String getOutputClassForTable() {
        return "XML Schema Constraint"
    }

    @Override
    String getTypeText() {
        return "XML schema constraint"
    }

    @Override
    String getTypeTitle() {
        return "XML Schema Constraint"
    }




}
