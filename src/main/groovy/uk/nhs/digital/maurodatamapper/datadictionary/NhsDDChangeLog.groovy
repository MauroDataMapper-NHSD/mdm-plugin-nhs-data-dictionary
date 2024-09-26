package uk.nhs.digital.maurodatamapper.datadictionary

class NhsDDChangeLog {
    String reference
    String referenceUrl
    String description
    String implementationDate

    NhsDDChangeLog(NhsDDBranch branch, String changeRequestUrl) {
        this.reference = branch.reference
        if (!this.reference || this.reference.empty) {
            this.reference = "n/a"
        }
        else {
            this.referenceUrl = changeRequestUrl.replace(NhsDataDictionary.CHANGE_REQUEST_NUMBER_TOKEN, this.reference)
        }

        this.description = branch.subject
        if (!this.description || this.description.empty) {
            this.description = ""
        }

        this.implementationDate = branch.effectiveDate
        if (!this.implementationDate || this.implementationDate.empty) {
            this.implementationDate = "Immediate"
        }
    }
}
