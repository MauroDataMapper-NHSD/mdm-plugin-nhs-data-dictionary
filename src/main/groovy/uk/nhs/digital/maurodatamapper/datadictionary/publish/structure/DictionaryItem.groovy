package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic

enum DictionaryItemState {
    ACTIVE,
    RETIRED,
    PREPARATORY
}

class DictionaryItem implements DitaAware<Topic> {
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
        sections.add(section)
        this
    }

    String getOfficialName() {
        if (state == DictionaryItemState.RETIRED) {
            return "$name (Retired)"
        }

        name
    }

    String getXrefId() {
        String encodedName = replaceNonAlphaNumerics(name)
        String retiredSuffix = state == DictionaryItemState.RETIRED ? "_retired" : ""
        String key = "${stereotype}_${encodedName}${retiredSuffix}".replace(" ", "_").toLowerCase()
        key
    }

    Topic generateDita() {
        String titleOutputClass = this.outputClass
        Topic.build(id: getXrefId()) {
            title (outputClass: titleOutputClass) {
                text getOfficialName()
            }
            shortdesc description
            sections.each { section ->
                topic section.generateDita()
            }
        }
    }

    private static String replaceNonAlphaNumerics(String value) {
        if (!value) {
            return ""
        }

        value.replaceAll("[^A-Za-z0-9- ]", "")
    }
}
