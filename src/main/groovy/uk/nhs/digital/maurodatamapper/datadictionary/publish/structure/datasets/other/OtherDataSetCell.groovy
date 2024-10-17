package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.datasets.other

import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.ItemLink

class OtherDataSetCell {
}

class OtherDataSetItemLinkCell extends OtherDataSetCell {
    final String name
    final ItemLink itemLink
    final boolean allowMultiple

    OtherDataSetItemLinkCell(String name, ItemLink itemLink) {
        this(name, itemLink, false)
    }

    OtherDataSetItemLinkCell(String name, ItemLink itemLink, boolean allowMultiple) {
        this.name = name
        this.itemLink = itemLink
        this.allowMultiple = allowMultiple
    }
}

class OtherDataSetChoiceCell extends OtherDataSetCell {
    final String operator   // AND, OR, AND/OR
    final List<OtherDataSetItemLinkCell> cells

    OtherDataSetChoiceCell(String operator, List<OtherDataSetItemLinkCell> cells) {
        this.operator = operator
        this.cells = cells
    }
}

class OtherDataSetAddressCell extends OtherDataSetCell {
    final OtherDataSetItemLinkCell element
    final ItemLink address1
    final ItemLink address2

    OtherDataSetAddressCell(OtherDataSetItemLinkCell element, ItemLink address1, ItemLink address2) {
        this.element = element
        this.address1 = address1
        this.address2 = address2
    }
}
