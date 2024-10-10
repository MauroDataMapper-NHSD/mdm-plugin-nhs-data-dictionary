package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishHelper

enum DictionaryItemState {
    ACTIVE,
    RETIRED,
    PREPARATORY
}

class DictionaryItem implements DitaAware<Topic>, HtmlAware {
    final UUID id
    final UUID branchId
    final String stereotype
    final String name
    final DictionaryItemState state
    final String outputClass
    final String description

    final List<Section> sections = []

    DictionaryItem(
        UUID id,
        UUID branchId,
        String stereotype,
        String name,
        DictionaryItemState state,
        String outputClass,
        String description) {
        this.id = id
        this.branchId = branchId
        this.stereotype = stereotype
        this.name = name
        this.state = state
        this.outputClass = outputClass
        this.description = description
    }

    DictionaryItem addSection(Section section) {
        if (section) {
            sections.add(section)
        }
        this
    }

    String getOfficialName() {
        PublishHelper.createOfficialName(name, state)
    }

    String getXrefId() {
        PublishHelper.createXrefId(stereotype, name, state)
    }

    @Override
    Topic generateDita() {
        String titleOutputClass = this.outputClass

        Topic.build(id: xrefId) {
            title (outputClass: titleOutputClass) {
                text getOfficialName()
            }
            shortdesc description
            sections.each { section ->
                topic section.generateDita()
            }
        }
    }

    @Override
    String generateHtml() {
        StringWriter writer = new StringWriter()
        MarkupBuilder builder = new MarkupBuilder(writer)

        String titleCssClass = "title topictitle1 ${outputClass}"

        builder.h1(class: titleCssClass) {
            mkp.yield(getOfficialName())
        }

        if (description) {
            builder.div(class: "- topic/body body") {
                builder.p(class: "- topic/shortdesc shortdesc") {
                    mkp.yield(description)
                }
            }
        }

        writer.toString()
    }
}
