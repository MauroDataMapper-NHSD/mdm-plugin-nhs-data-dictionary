package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

class NhsDDCode {

    String code
    String definition
    String publishDate
    String webOrder
    String webPresentation
    Boolean isDefault

    NhsDDAttribute owningAttribute
    List<NhsDDElement> usedByElements = []

    Term catalogueItem

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
        this.usedByElements.add(owningElement)
    }

    NhsDDCode() {

    }


}
