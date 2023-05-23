package uk.nhs.digital.maurodatamapper.datadictionary.publish.website

import org.apache.commons.lang3.StringUtils
import uk.ac.ox.softeng.maurodatamapper.dita.DitaProject
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.DitaMap
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Linking
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Toc
import uk.ac.ox.softeng.maurodatamapper.dita.helpers.HtmlHelper
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetFolder
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishOptions

class DataSetsWebsiteHelper {


    static void dataSetsIndex(NhsDataDictionary dataDictionary, PublishOptions publishOptions, DitaProject ditaProject) {

        dataDictionary.dataSetFolders.values().each { folders ->
            folders.each { folder ->
                generateDitaMapForFolder(folder, dataDictionary, publishOptions, ditaProject)
                generateOverviewTopicForFolder(folder, dataDictionary, publishOptions, ditaProject)
            }
        }

        DitaMap dataSetsIndexMap = DitaMap.build {
            id "data_sets"
            title "Data Sets"
            topicSet { topicSet ->
                navTitle "Data Sets"
                id 'data_sets_group'
                keyRef 'data_sets_overview'
                toc Toc.YES
                linking Linking.NORMAL
                dataDictionary.dataSetFolders.values().each { folders ->
                    folders.sort { it.name }.each { folder ->
                        if (folder.ditaFolderPath.size() == 0 && !folder.isRetired()) {
                            topicSet.mapRef {
                                toc Toc.YES
                                keyRef folder.getDitaKey()
                            }
                        }
                    }
                }
            }
        }
        ditaProject.registerMap("", dataSetsIndexMap)
        ditaProject.mainMap.mapRef {
            toc Toc.YES
            keyRef "data_sets"
        }

        createOverviewPage(ditaProject)


        dataDictionary.dataSets.values().each { dataSet ->
            String path = "data_sets/" + StringUtils.join(dataSet.getDitaFolderPath(), "/")
            DitaMap dataSetMap = dataSet.generateMap()
            ditaProject.registerMap(path, dataSetMap)
            ditaProject.registerTopic(path, dataSet.generateTopic())
            dataSetMap.topicRef {
                toc Toc.NO
                keyRef dataSet.getDitaKey()
            }
        }


    }


    static void createOverviewPage(DitaProject ditaProject) {
        Topic dataSetsOverview = Topic.build {
            id 'data_sets_overview'
            title 'Data Sets'
            shortdesc 'Data Sets provide the specification for data collections and for data analyses.'
            body {
                p WebsiteUtility.TO_BE_OVERRIDDEN_TEXT
            }
        }
        ditaProject.registerTopic("data_sets", dataSetsOverview)

    }


    static void generateDitaMapForFolder(NhsDDDataSetFolder folder, NhsDataDictionary dataDictionary, PublishOptions publishOptions, DitaProject ditaProject) {

        DitaMap dataSetsIndexMap = DitaMap.build {
            id folder.getDitaKey()
            title folder.getNameWithRetired()
            topicSet { topicSet ->
                navTitle folder.getNameWithRetired()
                id "${folder.getDitaKey()}_group"
                keyRef "${folder.getDitaKey()}_overview"
                toc Toc.YES
                linking Linking.NORMAL
                folder.childFolders.values().sort { it.name }.each { childFolder ->
                    topicSet.mapRef {
                        toc Toc.YES
                        keyRef childFolder.getDitaKey()
                    }
                }
                folder.dataSets.values().sort {it.name }.each { dataSet ->
                    topicSet.topicRef {
                        toc Toc.YES
                        keyRef dataSet.getDitaKey()
                    }
                }
            }
        }
        String path = "data_sets/" + StringUtils.join(folder.getDitaFolderPath(), "/")
        ditaProject.registerMap(path, dataSetsIndexMap)
    }

    static void generateOverviewTopicForFolder(NhsDDDataSetFolder folder, NhsDataDictionary dataDictionary, PublishOptions publishOptions, DitaProject ditaProject) {
        Topic folderOverviewTopic = Topic.build {
            id "${folder.getDitaKey()}_overview"
            title folder.getNameWithRetired()
            shortdesc folder.getShortDescription()
            body {
                if (folder.definition) {
                    div HtmlHelper.replaceHtmlWithDita(folder.definition)
                }
            }
        }
        String path = "data_sets/" + StringUtils.join(folder.getDitaFolderPath(), "/")
        ditaProject.registerTopic(path, folderOverviewTopic)

    }
}