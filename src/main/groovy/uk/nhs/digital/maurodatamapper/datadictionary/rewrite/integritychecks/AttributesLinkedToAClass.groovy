package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType

import uk.nhs.digital.maurodatamapper.datadictionary.DDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

class AttributesLinkedToAClass implements IntegrityCheck {

    String name = "Attributes linked to a class"

    String description = "Check that all live attributes are linked to a class"

    @Override
    List<NhsDataDictionaryComponent> runCheck(NhsDataDictionary dataDictionary) {

        Map<String, NhsDDAttribute> allUnusedAttributesMap = new HashMap<String, NhsDDAttribute>(dataDictionary.attributes)

        dataDictionary.classes.values().each {dataClass ->
            dataClass.allAttributes().each {attribute ->
                allUnusedAttributesMap.remove(attribute.name)
            }
        }

        allUnusedAttributesMap.removeAll {name, attribute ->
            attribute.isRetired()
        }
        return allUnusedAttributesMap.values() as List
    }

}
