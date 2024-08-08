/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
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
                        if (folder.ditaFolderPath.size() == 1 && !folder.isRetired()) {
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
            String path = "data_sets/" + StringUtils.join(dataSet.getDitaFolderPath(), "/").toLowerCase()
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
        ditaProject.registerTopic("", dataSetsOverview)

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
        String path = "data_sets/" + StringUtils.join(folder.getDitaFolderPath(), "/").toLowerCase()
        ditaProject.registerMap(path, dataSetsIndexMap, folder.getNameWithoutNonAlphaNumerics().toLowerCase())
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
        String path = "data_sets/" + StringUtils.join(folder.getDitaFolderPath(), "/").toLowerCase()
        ditaProject.registerTopic(path, folderOverviewTopic)

    }
}