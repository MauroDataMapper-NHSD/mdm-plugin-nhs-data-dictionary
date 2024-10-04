package uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper

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
}
