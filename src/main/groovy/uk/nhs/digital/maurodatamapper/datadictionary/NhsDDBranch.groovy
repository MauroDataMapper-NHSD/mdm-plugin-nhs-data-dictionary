package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder

class NhsDDBranch {
    static final String METADATA_NAMESPACE = 'uk.nhs.datadictionary.workItem'
    static final String METADATA_KEY_TYPE = 'type'
    static final String METADATA_KEY_REFERENCE = 'reference'
    static final String METADATA_KEY_EFFECTIVE_DATE = 'effectiveDate'

    String id
    String branchName
    String modelVersion
    String modelVersionLabel
    boolean finalised

    String type
    String reference
    String effectiveDate

    NhsDDBranch(VersionedFolder versionedFolder) {
        this.id = versionedFolder.id
        this.branchName = versionedFolder.branchName
        this.modelVersion = versionedFolder.modelVersion
        this.modelVersionLabel = versionedFolder.modelVersionTag
        this.finalised = versionedFolder.finalised.booleanValue()

        this.type = versionedFolder.findMetadataByNamespaceAndKey(METADATA_NAMESPACE, METADATA_KEY_TYPE)?.value ?: ''
        this.reference = versionedFolder.findMetadataByNamespaceAndKey(METADATA_NAMESPACE, METADATA_KEY_REFERENCE)?.value ?: ''
        this.effectiveDate = versionedFolder.findMetadataByNamespaceAndKey(METADATA_NAMESPACE, METADATA_KEY_EFFECTIVE_DATE)?.value ?: ''
    }
}
