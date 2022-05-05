package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish.changePaper

import uk.ac.ox.softeng.maurodatamapper.dita.meta.DitaElement

import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

class Change {
    String changeType
    String stereotype

    NhsDataDictionaryComponent oldItem
    NhsDataDictionaryComponent newItem

    String htmlDetail
    DitaElement ditaDetail

    Change() { }

    String changeText(String stereotype) {
        if(changeType == "New") {
            return "New " + stereotype
        }
        if(changeType == "Retired") {
            return "Changed status to retired"
        }
        if(changeType == "Description") {
            return "Changed description"
        }
    }

}
