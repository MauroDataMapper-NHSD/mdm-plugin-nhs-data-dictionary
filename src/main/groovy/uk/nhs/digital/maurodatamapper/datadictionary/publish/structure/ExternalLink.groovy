package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.XRef
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Scope

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.ChangeAware

class ExternalLink implements DitaAware<XRef>, HtmlBuilder, ChangeAware{
    final String name
    final String url
    final String outputClass

    final DiffStatus diffStatus

    ExternalLink(String name, String url, String outputClass) {
        this(name, url, outputClass, DiffStatus.NONE)
    }

    ExternalLink(String name, String url, String outputClass, DiffStatus diffStatus) {
        this.name = name
        this.url = url
        this.outputClass = outputClass
        this.diffStatus = diffStatus
    }

    @Override
    String getDiscriminator() {
        "${name}_${url}"
    }

    @Override
    XRef generateDita(PublishContext context) {
        String linkOutputClass = outputClass
        XRef.build(
            scope: Scope.EXTERNAL,
            format: "html",
            href: url,
            outputClass: linkOutputClass
        ) {
            txt name
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        builder.a(class: outputClass, title: name, href: href) {
            mkp.yield(name)
        }
    }
}
