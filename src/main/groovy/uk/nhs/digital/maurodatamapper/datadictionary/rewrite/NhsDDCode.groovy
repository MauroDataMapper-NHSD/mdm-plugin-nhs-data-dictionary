package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

class NhsDDCode {

    String code
    String definition
    String publishDate
    String webOrder
    String webPresentation
    Boolean isDefault

    NhsDDAttribute owningAttribute
    NhsDDElement owningElement

    boolean isRetired
    String retiredDate

    Map<String, String> propertiesAsMap() {
        return [
            "publishDate"       : publishDate,
            "isRetired"         : isRetired.toString(),
            "retiredDate"       : retiredDate,
            "webOrder"          : webOrder,
            "webPresentation"   : webPresentation,
            "isDefault"         : isDefault.toString()
        ]
    }

    NhsDDCode(NhsDDAttribute owningAttribute) {
        this.owningAttribute = owningAttribute
    }

    NhsDDCode(NhsDDElement owningElement) {
        this.owningElement = owningElement
    }

    NhsDDCode() {

    }


}
