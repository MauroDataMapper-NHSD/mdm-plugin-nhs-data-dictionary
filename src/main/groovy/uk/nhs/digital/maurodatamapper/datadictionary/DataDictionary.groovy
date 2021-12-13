package uk.nhs.digital.maurodatamapper.datadictionary


import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship

import groovy.util.logging.Slf4j

import java.util.regex.Matcher

@Slf4j
class DataDictionary {


    static final String MAP_FILE_NAME = "map.ditamap"

    static boolean FINALISED = false


    static XmlParser xmlParser = new XmlParser()

    // attributes and elements can be looked up by uin (other maps are by name... for now at least)
    Map<String, DDAttribute> attributes = [:]
    Map<String, DDElement> elements = [:]


    Map<String, DDClass> classes = [:]
    Map<String, DDDataSet> dataSets = [:]
    Map<String, DDWebPage> webPages = [:]
    Map<String, DDBusinessDefinition> businessDefinitions = [:]
    Map<String, DDSupportingDefinition> supportingInformation = [:]
    Map<String, DDXmlSchemaConstraint> xmlSchemaConstraints = [:]

    Map<String, DataType> dataTypes = [:]

    Terminology webPagesTerminology

    UUID coreDataModelId
    UUID businessDefinitionsTerminologyId
    UUID supportingDefinitionsTerminologyId
    UUID xmlSchemaConstraintsTerminologyId
    UUID webPagesTerminologyId

    Map<String, String> urlMap = [:]

    Map<String, DataDictionaryComponent> internalLinks = [:]

    Map<String, List<String>> unmatchedUrls = [:]


    Terminology buildBusDefTerminology() {
        Terminology businessDefinitionsTerminology = new Terminology(label: BUSDEF_TERMINOLOGY_NAME)
        TermRelationshipType broaderThan = new TermRelationshipType(
                label: 'broaderThan',
                description: 'Broader Than',
                displayLabel: 'Broader Than',
                parentalRelationship: true
        )
        TermRelationshipType narrowerThan = new TermRelationshipType(
                label: 'narrowerThan',
                description: 'Narrower Than',
                displayLabel: 'Narrower Than',
                childRelationship: true
        )
        businessDefinitionsTerminology.addToTermRelationshipTypes(broaderThan)
        businessDefinitionsTerminology.addToTermRelationshipTypes(narrowerThan)

        Map<String, Term> topTerms = [:]
        businessDefinitions.values().each { businessDefinition ->
            String parentTermName = businessDefinition.getParentTermName()
            Term topTerm = topTerms[parentTermName]
            if(!topTerm) {
                topTerm = new Term(code: parentTermName,
                        definition: parentTermName,
                        terminology: businessDefinitionsTerminology,
                        url: """http://www.datadictionary.nhs.uk/data_dictionary/nhs_business_definitions_${parentTermName.toLowerCase()}_child.asp""",
                        description: "No description",
                        depth: 1,
                        sourceTermRelationships: [])
                topTerms[parentTermName] = topTerm
                businessDefinitionsTerminology.addToTerms(topTerm)
            }
            Term thisTerm = (Term) businessDefinition.toCatalogueItem()
            thisTerm.depth = 2
            thisTerm.terminology = businessDefinitionsTerminology
            businessDefinitionsTerminology.addToTerms(thisTerm)
            TermRelationship tr = new TermRelationship(sourceTerm: thisTerm, targetTerm: topTerm, relationshipType: narrowerThan)
            thisTerm.sourceTermRelationships << tr
            TermRelationship tr2 = new TermRelationship(sourceTerm: topTerm, targetTerm: thisTerm, relationshipType: broaderThan)
            topTerm.sourceTermRelationships << tr2
        }
        return businessDefinitionsTerminology

    }

    Terminology buildSupDefTerminology() {
        Terminology supportingDefinitionsTerminology = new Terminology(label: SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME)
        TermRelationshipType broaderThan = new TermRelationshipType(
                label: 'broaderThan',
                description: 'Broader Than',
                displayLabel: 'Broader Than',
                parentalRelationship: true
        )
        TermRelationshipType narrowerThan = new TermRelationshipType(
                label: 'narrowerThan',
                description: 'Narrower Than',
                displayLabel: 'Narrower Than',
                childRelationship: true
        )
        supportingDefinitionsTerminology.addToTermRelationshipTypes(broaderThan)
        supportingDefinitionsTerminology.addToTermRelationshipTypes(narrowerThan)

        supportingInformation.values().each { supportingDefinition ->
            Term thisTerm = (Term) supportingDefinition.toCatalogueItem()
            thisTerm.depth = 1
            thisTerm.terminology = supportingDefinitionsTerminology
            supportingDefinitionsTerminology.addToTerms(thisTerm)
        }
        return supportingDefinitionsTerminology

    }


    Terminology buildXmlSchemaConstraintsTerminology() {
        Terminology xmlSchemaConstraintsTerminology = new Terminology(label: XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME)
        TermRelationshipType broaderThan = new TermRelationshipType(
                label: 'broaderThan',
                description: 'Broader Than',
                displayLabel: 'Broader Than',
                parentalRelationship: true
        )
        TermRelationshipType narrowerThan = new TermRelationshipType(
                label: 'narrowerThan',
                description: 'Narrower Than',
                displayLabel: 'Narrower Than',
                childRelationship: true
        )
        xmlSchemaConstraintsTerminology.addToTermRelationshipTypes(broaderThan)
        xmlSchemaConstraintsTerminology.addToTermRelationshipTypes(narrowerThan)

        xmlSchemaConstraints.values().each { xmlSchemaConstraint ->

            Term thisTerm = (Term) xmlSchemaConstraint.toCatalogueItem()
            thisTerm.depth = 1
            thisTerm.terminology = xmlSchemaConstraintsTerminology
            xmlSchemaConstraintsTerminology.addToTerms(thisTerm)

        }
        return xmlSchemaConstraintsTerminology

    }



    Terminology buildWebPagesTerminology() {
        Terminology webPagesTerminology = new Terminology(label: WEB_PAGES_TERMINOLOGY_NAME)
        TermRelationshipType broaderThan = new TermRelationshipType(
                label: 'broaderThan',
                description: 'Broader Than',
                displayLabel: 'Broader Than',
                parentalRelationship: true
        )
        TermRelationshipType narrowerThan = new TermRelationshipType(
                label: 'narrowerThan',
                description: 'Narrower Than',
                displayLabel: 'Narrower Than',
                childRelationship: true
        )
        webPagesTerminology.addToTermRelationshipTypes(broaderThan)
        webPagesTerminology.addToTermRelationshipTypes(narrowerThan)

        //       Map<String, Term> topTerms = [:]
        webPages.values().each { webPage ->
            //String parentTermName = businessDefinition.getParentTermName()

            Term thisTerm = (Term) webPage.toCatalogueItem()
            thisTerm.depth = 1
            thisTerm.terminology = webPagesTerminology
            webPagesTerminology.addToTerms(thisTerm)

        }
        webPagesTerminology
    }



    void addToUrlMap(DataDictionaryComponent component, String internalLink) {
        urlMap[component.ddUrl] = internalLink
        urlMap[component.ddUrl.replace("https://v3.", "https://")] = internalLink
        urlMap[component.ddUrl.replace("https://v3.", "https://www.")] = internalLink
    }






    String replaceLinksInString(String original, String componentName) {
        String newDescription = original
        if (original) {
            Matcher matcher = pattern.matcher(newDescription)
            //boolean found = false
            while (matcher.find()) {
                String matchedUrl = urlMap[matcher.group(1)]
                if(matchedUrl) {
                    String replacement = "[" + matcher.group(2).replaceAll("_", " ") + matchedUrl
                    newDescription = newDescription.replace(matcher.group(0), replacement)
                    //found = true
                } else {
                    List<String> existingUnmatched = unmatchedUrls[componentName]
                    if(existingUnmatched) {
                        existingUnmatched.add(matcher.group(0))
                    } else {
                        unmatchedUrls[componentName] = [matcher.group(0)]
                    }
                }
            }
        }
        return newDescription?.trim() ?: null

    }




    void loadIds(def dataClass, String modelId, String parentClassId) {

        String classUltimatePath = DDHelperFunctions.getMetadataValue(dataClass, "ultimate-path")
        if(classUltimatePath && !urlMap[classUltimatePath]) {
            urlMap[classUltimatePath] = "[${dataClass.label}](MC|DC|${modelId}|${parentClassId}|${dataClass.id})".toString()
        }

        dataClass.dataElements.each { dataElement ->
            String elementUltimatePath = DDHelperFunctions.getMetadataValue(dataElement, "ultimate-path")
            if(elementUltimatePath && !urlMap[elementUltimatePath]) {
                urlMap[elementUltimatePath] = "[${dataElement.label}](MC|DE|${modelId}|${dataClass.id}|${dataElement.id})".toString()
            }
        }
        dataClass.dataClasses.each { childDataClass ->
            loadIds(childDataClass, modelId, dataClass.id.toString())
        }

    }

    List<DataDictionaryComponent> getAllComponents() {
        List<DataDictionaryComponent> returnValues = []
        returnValues.addAll(attributes.values())
        returnValues.addAll(elements.values())
        returnValues.addAll(classes.values())
        returnValues.addAll(dataSets.values())

        returnValues.addAll(businessDefinitions.values())
        returnValues.addAll(supportingInformation.values())
        returnValues.addAll(xmlSchemaConstraints.values())

        return returnValues
    }



/*    def updateMetadataValue(CatalogueClient catalogueClient, UUID facetOwnderId, UUID metadataID, String newNamespace, String newKey, String newValue, String connectionName = "_default") {
        RestResponse restResponse = catalogueClient.namedConnections[connectionName].doPut(CatalogueEndpoint.UPDATE_METADATA_ENDPOINT, [facetOwnderId.toString(), metadataID.toString()],
                HttpStatus.OK,
                { namespace = newNamespace
                    key = newKey
                    value = newValue})
        return restResponse
    }
*/


}
