package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

import grails.gorm.transactions.Transactional
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions

@Transactional
class AttributeService extends DataDictionaryComponentService<DataElement> {

    @Override
    Map indexMap(DataElement object) {
        [
            catalogueId: object.id.toString(),
            name: object.label,
            stereotype: "attribute"
        ]
    }

    @Override
    def show(String id) {
        DataElement dataElement = dataElementService.get(id)

        String description = dataElement.description

        def result = [
                catalogueId: dataElement.id.toString(),
                name: dataElement.label,
                stereotype: "attribute",
                shortDescription: getShortDescription(dataElement),
                description: description,
                alsoKnownAs: getAliases(dataElement)
        ]
        return result
    }

    @Override
    Set<DataElement> getAll() {
        nhsDataDictionaryService.getAttributes(false)
    }

    @Override
    DataElement getItem(UUID id) {
        dataElementService.get(id)
    }

    @Override
    String getMetadataNamespace() {
        DDHelperFunctions.metadataNamespace + ".attribute"
    }

}
