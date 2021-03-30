package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel

import grails.gorm.transactions.Transactional
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary

@Transactional
class DataSetService extends DataDictionaryComponentService <DataModel> {

    @Override
    Map indexMap(DataModel catalogueItem) {
        [
            catalogueId: catalogueItem.id.toString(),
            name: catalogueItem.label,
            stereotype: "dataSet"

        ]
    }

    @Override
    def show(String branch, String id) {
        DataModel dataModel = dataModelService.get(id)

        String description = convertLinksInDescription(branch, dataModel.description)


        def result = [
                catalogueId: dataModel.id.toString(),
                name: dataModel.label,
                stereotype: "dataSet",
                shortDescription: getShortDescription(dataModel),
                description: description,
                alsoKnownAs: getAliases(dataModel)

        ]
        return result
    }

    @Override
    Set<DataModel> getAll() {
        Folder folder = folderService.findByPath(DataDictionary.DATA_DICTIONARY_FOLDER_NAME.toString())
        Folder dataSetsFolder = folder.childFolders.find { it.label == DataDictionary.DATA_DICTIONARY_DATA_SETS_FOLDER_NAME }

        getAllDataSets(dataSetsFolder)
    }

    @Override
    DataModel getItem(UUID id) {
        dataModelService.get(id)
    }

    Set<DataModel> getAllDataSets(Folder dataSetsFolder) {
        if(dataSetsFolder.label != "Retired") {
            Set<DataModel> returnModels = []
            dataSetsFolder.childFolders.each { childFolder ->
                returnModels.addAll(getAllDataSets(childFolder))
            }
            returnModels.addAll(dataModelService.findAllByFolderId(dataSetsFolder.id))
            return returnModels
        } else return []
    }

    @Override
    String getMetadataNamespace() {
        DDHelperFunctions.metadataNamespace + ".data set"
    }


}
