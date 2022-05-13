/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 */
package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish


import uk.ac.ox.softeng.maurodatamapper.dita.DitaProject2
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.DitaMap
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Toc

import net.lingala.zip4j.ZipFile
import org.apache.commons.io.FileUtils
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat

class WebsiteUtility {

    static final List<String> allStereotypes = [
            'attribute',
            'businessDefinition',
            'class',
            'dataSet',
            'dataSetConstraint',
            'element',
            'supportingInformation'
    ]

    static final String GITHUB_BRANCH_URL = "https://github.com/NHSDigital/DataDictionaryPublication/archive/refs/heads/master.zip"

    static File generateWebsite(NhsDataDictionary dataDictionary, Path outputPath, PublishOptions publishOptions) {

        DitaProject2 ditaProject = new DitaProject2("NHS Data Model and Dictionary","nhs_data_dictionary")


        generateIndexTopics(dataDictionary, ditaProject, publishOptions)
        generateAllItemsIndex(dataDictionary, ditaProject, publishOptions)

        Map<String, NhsDataDictionaryComponent> pathLookup = [:]

        dataDictionary.getAllComponents().each { component ->
            ditaProject.addExternalKey(component.getDitaKey(), component.otherProperties["ddUrl"])
            pathLookup[component.getMauroPath()] = component
        }


        dataDictionary.allComponents.each {component ->
            component.replaceLinksInDefinition(pathLookup)
        }

        allStereotypes.each {stereotype ->
            ditaProject.addMapToMainMap('', stereotype, stereotype, Toc.NO)

        }


        dataDictionary.allComponents.
            sort {it.name }.
            each {component ->
                if(publishOptions.isPublishableComponent(component)) {
                    String path = "${component.stereotypeForPreview}/${component.nameWithoutNonAlphaNumerics.substring(0,1)}/${component.getNameWithoutNonAlphaNumerics()}"
                    ditaProject.addTopicToMapById(path, component.generateTopic(pathLookup), component.stereotypeForPreview, Toc.NO)
                }
        }

        String ditaOutputDirectory = outputPath.toString() + File.separator + "dita"
        ditaProject.writeToDirectory(Paths.get(ditaOutputDirectory))

        //overwriteGithubDir(ditaOutputDirectory)


        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy")
        String date = simpleDateFormat.format(new Date())

        String filename = dataDictionary.branchName + '-' + date + ".zip"

        ZipFile zipFile = new ZipFile(outputPath.toString() + File.separator + filename)
        zipFile.addFolder(new File(ditaOutputDirectory))

        return zipFile.getFile()
    }

    static void overwriteGithubDir(String outputPath){

        // Create a temporary directory for the downloaded zip
        Path tempPath = Files.createTempDirectory("ditaGeneration")
        String sourceFile = tempPath.toString() + "/github_download.zip"

        // Get the zip file and save it into the directory
        InputStream inputStream = new URL(GITHUB_BRANCH_URL).openStream()
        Files.copy(inputStream, Paths.get(sourceFile), StandardCopyOption.REPLACE_EXISTING)



        // Extract the necessary contents and copy them to the right place
        ZipFile zipFile = new ZipFile(sourceFile)
        zipFile.extractFile("DataDictionaryPublication-master/Website/", outputPath)
        FileUtils.copyDirectory(new File(outputPath + "/DataDictionaryPublication-master/Website/"), new File(outputPath))

        // tidy up
        Files.delete(new File(sourceFile).toPath())
        FileUtils.deleteDirectory(new File(outputPath + "/DataDictionaryPublication-master/"))
    }

    static Topic getFlatIndexTopic(Map<String, NhsDataDictionaryComponent> componentMap, String indexPrefix, String indexTopicTitle) {

        Topic.build(
            id: "${indexPrefix}.index"
        ) {
            title indexTopicTitle
            titlealts {
                searchtitle "All Items: ${indexTopicTitle}"
            }
            List<String> alphabet = ['0-9']
            alphabet.addAll('a'..'z')

            alphabet.each {alphIndex ->
                List<NhsDataDictionaryComponent> indexMap = componentMap.findAll {name, component ->
                    !component.isRetired() &&
                    ((alphIndex == '0-9' && Character.isDigit(name.charAt(0))) ||
                     name.toLowerCase().startsWith(alphIndex))
                }.values().sort {it.name}

                if(indexMap) {
                    topic (id: "${indexPrefix}.index.${alphIndex}"){
                        title alphIndex.toUpperCase()
                        body {
                            simpletable(relColWidth: ["10*"], outputClass: "table table-striped table-sm") {
                                stHead(outputClass: "thead-light") {
                                    stentry "Item Name"
                                }
                                indexMap.each {component ->
                                    strow {
                                        stentry {
                                            xRef component.calculateXRef()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    static void generateIndexTopics(NhsDataDictionary dataDictionary, DitaProject2 ditaProject, PublishOptions publishOptions) {

        if(publishOptions.isPublishAttributes()) {
            Topic attributesIndexTopic = getFlatIndexTopic(dataDictionary.attributes, "attributes", "Attributes")
            ditaProject.addTopicToMainMap("", attributesIndexTopic, Toc.YES)
        }
        if(publishOptions.isPublishElements()) {
            Topic elementsIndexTopic = getFlatIndexTopic(dataDictionary.elements, "elements", "Data Elements")
            ditaProject.addTopicToMainMap("", elementsIndexTopic, Toc.YES)
        }
        if(publishOptions.isPublishClasses()) {
            Topic classesIndexTopic = getFlatIndexTopic(dataDictionary.classes, "classes", "Classes")
            ditaProject.addTopicToMainMap("", classesIndexTopic, Toc.YES)
        }
        if(publishOptions.isPublishBusinessDefinitions()) {
            Topic nhsBusinessDefinitionsIndexTopic = getFlatIndexTopic(dataDictionary.businessDefinitions,
                                                                   "nhsBusinessDefinitions", "NHS Business Definitions")
            ditaProject.addTopicToMainMap("", nhsBusinessDefinitionsIndexTopic, Toc.YES)

        }
        if(publishOptions.isPublishSupportingInformation()) {
            Topic supportingInformationIndexTopic = getFlatIndexTopic(dataDictionary.supportingInformation,
                                                                  "supportingInformation", "Supporting Information")
            ditaProject.addTopicToMainMap("", supportingInformationIndexTopic, Toc.YES)
        }

    }

    static void generateAllItemsIndex(NhsDataDictionary dataDictionary, DitaProject2 ditaProject, PublishOptions publishOptions) {
        DitaMap allItemsIndexMap = DitaMap.build(id: "allItemsIndex") {
            title "All Items Index"
        }

        ditaProject.addMapToMainMap("", allItemsIndexMap, Toc.YES)

        List<String> alphabet = ['0-9']
        alphabet.addAll('a'..'z')

        alphabet.each {alphIndex ->
            Topic indexPage = Topic.build (id: "allItemsIndex${alphIndex}") {
                title "All Items Index: ${alphIndex}"
            }
            ditaProject.addTopicToMapById("", indexPage, "allItemsIndex", Toc.YES)
        }
    }

}
