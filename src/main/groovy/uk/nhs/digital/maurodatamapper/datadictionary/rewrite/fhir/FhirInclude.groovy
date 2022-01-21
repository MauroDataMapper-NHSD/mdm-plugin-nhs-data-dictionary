package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.fhir

class FhirInclude {

    String system
    String version
    List<FhirConcept> concepts = []
}
