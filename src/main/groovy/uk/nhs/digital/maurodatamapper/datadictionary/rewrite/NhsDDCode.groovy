package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

class NhsDDCode {

    String code
    String definition
    String publishDate
    String webOrder
    String webPresentation

    NhsDDAttribute owningAttribute

    boolean isRetired
    String retiredDate

    Map<String, String> propertiesAsMap() {
        return [
            "publishDate"    : publishDate,
            "isRetired"      : isRetired.toString(),
            "retiredDate"    : retiredDate,
            "webOrder"       : webOrder,
            "webPresentation": webPresentation
        ]
    }

    NhsDDCode(NhsDDAttribute owningAttribute) {
        this.owningAttribute = owningAttribute
    }




}
