package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.fhir

class FhirProperty {

    String code
    String valueCode
    String valueString
    String valueDateTime

    String getValue() {
        if(valueCode) {
            return valueCode
        } else if (valueString) {
            return valueString
        } else if (valueDateTime) {
            return valueDateTime
        }

    }



}
