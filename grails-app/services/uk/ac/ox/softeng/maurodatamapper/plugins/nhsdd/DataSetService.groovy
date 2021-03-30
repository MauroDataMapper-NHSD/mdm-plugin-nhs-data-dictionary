package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel

import grails.gorm.transactions.Transactional
import groovy.util.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionaryComponent
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html

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
        String shortDesc = replaceLinksInShortDescription(getShortDescription(dataModel, null))


        def result = [
                catalogueId: dataModel.id.toString(),
                name: dataModel.label,
                stereotype: "dataSet",
                shortDescription: shortDesc,
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

    @Override
    String getShortDescription(DataModel dataSet, DataDictionary dataDictionary) {
        if(isPreparatory(dataSet)) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {

            List<String> aliases = [dataSet.label]
            DataDictionaryComponent.aliasFields.keySet().each { aliasType ->
                String alias = DDHelperFunctions.getMetadataValue(dataSet, aliasType)
                if (alias) {
                    aliases.add(alias)
                }
            }


            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + DDHelperFunctions.parseHtml(dataSet.description.toString()) + "</xml>")
                String allParagraphs = ""
                xml.p.each { paragraph ->
                    allParagraphs += paragraph.text()
                }
                String nextSentence = ""
                while (!aliases.find { it -> nextSentence.contains(it) } && allParagraphs.contains(".")) {
                    nextSentence = allParagraphs.substring(0, allParagraphs.indexOf(".") + 1)
                    if (aliases.find { it -> nextSentence.contains(it) }) {
                        if(nextSentence.startsWith("Introduction")) {
                            nextSentence = nextSentence.replaceFirst("Introduction", "")
                        }
                        return nextSentence
                    }
                    allParagraphs = allParagraphs.substring(allParagraphs.indexOf(".") + 1)
                }
                return dataSet.label
            } catch (Exception e) {
                log.error("Couldn't parse: " + dataSet.description)
                return dataSet.label
            }
        }
    }
}
