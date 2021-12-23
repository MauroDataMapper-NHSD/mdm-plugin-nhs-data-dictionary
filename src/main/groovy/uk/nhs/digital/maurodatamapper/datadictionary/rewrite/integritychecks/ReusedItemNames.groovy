package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType

import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

class ReusedItemNames implements IntegrityCheck {

    String name = "Re-used item names"

    String description = "Check that item names of retired classes, attributes and elements have not been re-used"

    @Override
    List<NhsDataDictionaryComponent> runCheck(NhsDataDictionary dataDictionary) {

        Set<String> duplicateAttributes = findDuplicates(dataDictionary.attributes.values().collect {it.name})
        Set<String> duplicateElements = findDuplicates(dataDictionary.elements.values().collect {it.name})
        Set<String> duplicateClasses = findDuplicates(dataDictionary.classes.values().collect {it.name})

        Set<NhsDataDictionaryComponent> foundDuplicates = []
        if (duplicateAttributes.size() > 0) {
            duplicateAttributes.each {attName ->
                foundDuplicates.addAll(dataDictionary.attributes.values().findAll {it.name == attName})
            }
        }
        if (duplicateElements.size() > 0) {
            duplicateElements.each {elemName ->
                foundDuplicates.addAll(dataDictionary.elements.values().findAll {it.name == elemName})
            }
        }
        if (duplicateClasses.size() > 0) {
            duplicateClasses.each {className ->
                foundDuplicates.addAll(dataDictionary.classes.values().findAll {it.name == className})
            }
        }
        errors = foundDuplicates as List
        return errors
    }

    private static <T> Set<T> findDuplicates(Collection<T> collection) {

        Set<T> duplicates = new HashSet<>(1000);
        Set<T> uniques = new HashSet<>();

        for (T t : collection) {
            if (!uniques.add(t)) {
                duplicates.add(t)
            }
        }

        return duplicates;
    }

}
