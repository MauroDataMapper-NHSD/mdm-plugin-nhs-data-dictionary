package uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDElement
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent

class ChangeFunctions {
    static <T extends ChangeAware> boolean areEqual(List<T> first, List<T> second) {
        if (!first) {
            return false
        }

        if (!second) {
            return false
        }

        if (first.size() != second.size()) {
            return false
        }

        first.every { firstItem ->
            second.any { secondItem -> secondItem.discriminator == firstItem.discriminator }
        }
    }

    static <T extends ChangeAware> List<T> getDifferences(List<T> source, List<T> compare) {
        source.findAll { sourceItem ->
            !compare.find {compareItem -> compareItem.discriminator == sourceItem.discriminator }
        }
    }

    static <T extends ChangeAware & NhsDataDictionaryComponent> StringWriter createUnorderedListHtml(String title, List<T> currentItems, List<T> previousItems) {
        List<T> newItems = getDifferences(currentItems, previousItems)
        List<T> removedItems = getDifferences(previousItems, currentItems)

        if (newItems.empty && removedItems.empty) {
            return null
        }

        StringWriter htmlWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(htmlWriter)

        markupBuilder.div {
            p {
                b title
            }
            ul {
                currentItems.each {element ->
                    if (newItems.any { it.discriminator == element.discriminator }) {
                        markupBuilder.li(class: "new") {
                            mkp.yield(element.name)
                        }
                    }
                    else {
                        markupBuilder.li {
                            mkp.yield(element.name)
                        }
                    }
                }
                removedItems.each { element ->
                    markupBuilder.li(class: "deleted") {
                        mkp.yield(element.name)
                    }
                }
            }
        }

        htmlWriter
    }
}
