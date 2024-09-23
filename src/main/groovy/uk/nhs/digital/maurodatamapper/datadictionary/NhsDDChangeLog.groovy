package uk.nhs.digital.maurodatamapper.datadictionary

class NhsDDChangeLog {
    String reference
    String description
    String implementationDate

    NhsDDChangeLog(NhsDDBranch branch) {
        this.reference = branch.reference
        if (!this.reference || this.reference.empty) {
            this.reference = "n/a"
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
