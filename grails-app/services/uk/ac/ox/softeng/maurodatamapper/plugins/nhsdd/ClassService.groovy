package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass

import grails.gorm.transactions.Transactional
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary

@Transactional
class ClassService extends DataDictionaryComponentService <DataClass> {

    @Override
    Map indexMap(DataClass catalogueItem) {
        [
            catalogueId: catalogueItem.id.toString(),
            name: catalogueItem.label,
            stereotype: "class"

        ]
    }

    @Override
    def show(String branch, String id) {
        DataClass dataClass = dataClassService.get(id)

        String description = convertLinksInDescription(branch, dataClass.description)


        def result = [
                catalogueId: dataClass.id.toString(),
                name: dataClass.label,
                stereotype: "class",
                shortDescription: getShortDescription(dataClass),
                description: description,
                alsoKnownAs: getAliases(dataClass)

        ]
        return result
    }

    @Override
    Set<DataClass> getAll() {
        nhsDataDictionaryService.getDataClasses(false)
    }

    @Override
    DataClass getItem(UUID id) {
        dataClassService.get(id)
    }

    @Override
    String getMetadataNamespace() {
        DDHelperFunctions.metadataNamespace + ".class"
    }


}
