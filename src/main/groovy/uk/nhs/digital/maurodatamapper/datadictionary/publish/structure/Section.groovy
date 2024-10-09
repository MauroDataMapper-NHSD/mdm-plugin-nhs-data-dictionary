package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.meta.DitaElement

class Section implements DitaAware<Topic> {
    final DictionaryItem parent

    final String type
    final String title

    final List<ContentAware> contents = []

    Section(DictionaryItem parent, String type, String title) {
        this.parent = parent
        this.type = type
        this.title = title
    }

    Section addContent(ContentAware content) {
        this.contents.add(content)
        this
    }

    Topic generateDita() {
        String xrefId = "${parent.getXrefId()}_${type}"

        List<ContentAware> bodyContents = this.contents

        Topic.build(id: xrefId) {
            title title
            body {
                bodyContents.each { content ->
                    div content.generateDita()
                }
            }
        }
    }
}
