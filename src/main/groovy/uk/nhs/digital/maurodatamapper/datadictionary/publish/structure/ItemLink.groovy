package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.XRef

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishHelper

class ItemLink implements DitaAware<XRef>, HtmlBuilder {
    final UUID itemId
    final UUID branchId

    final String stereotype
    final DictionaryItemState state
    final String name

    final String outputClass

    ItemLink(
        UUID itemId,
        UUID branchId,
        String stereotype,
        DictionaryItemState state,
        String name,
        String outputClass) {
        this.itemId = itemId
        this.branchId = branchId
        this.stereotype = stereotype
        this.state = state
        this.name = name
        this.outputClass = outputClass
    }

    static ItemLink create(NhsDataDictionaryComponent component) {
        new ItemLink(
            component.catalogueItemId,
            component.branchId,
            component.stereotype,
            component.itemState,
            component.name,
            component.outputClass)
    }

    @Override
    XRef generateDita() {
        String linkOutputClass = outputClass
        String xrefId = PublishHelper.createXrefId(stereotype, name, state)

        XRef.build(format: "html", keyRef: xrefId, outputClass: linkOutputClass) {
            txt name
        }
    }

    @Override
    void buildHtml(MarkupBuilder builder) {
        String href = "#/preview/${branchId}/${outputClass}/${itemId}"

        builder.a(class: outputClass, title: name, href: href) {
            mkp.yield(name)
        }
    }
}
