package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

class ItemLink {
    final UUID id
    final UUID branchId
    final String stereotype
    final DictionaryItemState state
    final String label

    ItemLink(
        UUID id,
        UUID branchId,
        String stereotype,
        DictionaryItemState state,
        String label) {
        this.id = id
        this.branchId = branchId
        this.stereotype = stereotype
        this.state = state
        this.label = label
    }

    // TODO: generate link (DITA key and HTML link address)
}
