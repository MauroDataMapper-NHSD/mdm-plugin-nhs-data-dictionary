package uk.nhs.digital.maurodatamapper.datadictionary.publish

import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItemState

class PublishHelper {
    static String createOfficialName(String name, DictionaryItemState state) {
        if (state == DictionaryItemState.RETIRED) {
            return "$name (Retired)"
        }

        name
    }

    static String createXrefId(String stereotype, String name, DictionaryItemState state) {
        String encodedName = replaceNonAlphaNumerics(name)
        String retiredSuffix = state == DictionaryItemState.RETIRED ? "_retired" : ""
        String key = "${stereotype}_${encodedName}${retiredSuffix}".replace(" ", "_").toLowerCase()
        key
    }

    private static String replaceNonAlphaNumerics(String value) {
        if (!value) {
            return ""
        }

        value.replaceAll("[^A-Za-z0-9- ]", "")
    }
}
