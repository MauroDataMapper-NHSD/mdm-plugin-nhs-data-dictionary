package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.fhir

import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDAttribute

import java.time.Instant

class FhirCodeSystem implements FhirResource {

    // TODO - take these from system properties
    public static final String URL_PREFIX =  "https://maurotest.datadictionary.nhs.uk/code_system/union/"
    public static final String SYSTEM = "https://datadictionary.nhs.uk/V3"
    public static final String IDENTIFIER_PREFIX = "https://datadictionary.nhs.uk/data_dictionary/attributes/"

    String id
    String version
    String url
    String identifierSystem
    String identifierValue
    String name
    String title
    String status
    String experimental
    String date
    String description
    List<FhirConcept> concepts = []


    static FhirCodeSystem fromDDAttribute(NhsDDAttribute attribute, String versionId, String sharedPublishDate) {

        String adjustedName = attribute.name.toLowerCase().replace(" ", "_")
        String idAdjustedName = attribute.name.toLowerCase().replace(" ", "-")
        new FhirCodeSystem().tap {
            id = "MauroTest-NHS-Data_Dictionary-CS-" + idAdjustedName + "-" + versionId
            version = versionId
            url = "${URL_PREFIX}${adjustedName}"
            identifierSystem = SYSTEM
            identifierValue = "${IDENTIFIER_PREFIX}${adjustedName}"
            name = attribute.name.toUpperCase().replaceAll("[^A-Za-z0-9]", "_")
            title = attribute.getTitleCaseName()
            status = "active"
            experimental = "false"
            date = sharedPublishDate
            description = attribute.getShortDescription()
            attribute.codes
                .sort { it.code }
                .each { attributeCode ->
                    FhirConcept concept = new FhirConcept().tap {
                        code = attributeCode.code
                        display = attributeCode.definition
                        definition = attributeCode.definition

                        properties.add(new FhirProperty(code: "Publish Date", valueDateTime: sharedPublishDate))
                        properties.add(new FhirProperty(code: "status", valueCode: attribute.isRetired()? "retired" : "active"))
                        properties.add(new FhirProperty(code: "Data Attribute", valueString: attributeCode.owningAttribute.getTitleCaseName()))
                        attributeCode.usedByElements.each {usedByElement ->
                            properties.add(new FhirProperty(code: "Data Element", valueString: usedByElement.getTitleCaseName()))
                        }
                    }
                concepts.add(concept)
                }
        }
    }


}
