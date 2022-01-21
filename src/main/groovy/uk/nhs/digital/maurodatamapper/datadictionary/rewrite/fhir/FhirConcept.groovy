package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.fhir

class FhirConcept {

    String code
    String display
    String definition
    String publishDate
    String status

    List<FhirProperty> properties = []


}
