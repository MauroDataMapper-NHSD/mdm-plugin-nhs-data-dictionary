package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

import grails.gorm.transactions.Transactional
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions

@Transactional
class ElementService extends DataDictionaryComponentService<DataElement> {

    @Override
    Map indexMap(DataElement object) {
        [
            catalogueId: object.id.toString(),
            name: object.label,
            stereotype: "element"
        ]
    }

    @Override
    def show(String branch, String id) {
        DataElement dataElement = dataElementService.get(id)

        String description = convertLinksInDescription(branch, dataElement.description)


        def result = [
                catalogueId: dataElement.id.toString(),
                name: dataElement.label,
                stereotype: "element",
                shortDescription: getShortDescription(dataElement),
                description: description,
                alsoKnownAs: getAliases(dataElement)
        ]
        return result
    }

    @Override
    Set<DataElement> getAll() {
        nhsDataDictionaryService.getDataFieldNotes(false)
    }

    @Override
    DataElement getItem(UUID id) {
        dataElementService.get(id)
    }

    @Override
    String getMetadataNamespace() {
        DDHelperFunctions.metadataNamespace + ".element"
    }

}
