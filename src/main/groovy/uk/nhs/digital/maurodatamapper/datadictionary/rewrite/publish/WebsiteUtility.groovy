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
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.TopicRef
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Linking
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

    static final Map<String, String> allStereotypes = [
            'Attributes': 'attribute',
            'Business Definitions': 'businessDefinition',
            'Classes': 'class',
            'Data Sets': 'dataSet',
            'Data Set Constraints': 'dataSetConstraint',
            'Elements': 'element',
            'Supporting Information': 'supportingInformation'
    ]

    static final String GITHUB_BRANCH_URL = "https://github.com/jamesrwelch/DataDictionaryPublication/archive/refs/heads/master.zip"
    static final Boolean TEST_GITHUB = true
    static final String TEST_GITHUB_DIR = "/Users/james/git/nhsd/james-fork/DataDictionaryPublication/Website"

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

        allStereotypes.each {name, stereotype ->
            ditaProject.addMapToMainMap('', stereotype, "All ${name}", Toc.NO)
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

        //System.err.println(ditaOutputDirectory)
        overwriteGithubDir(ditaOutputDirectory)


        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy")
        String date = simpleDateFormat.format(new Date())

        String filename = dataDictionary.branchName + '-' + date + ".zip"

        ZipFile zipFile = new ZipFile(outputPath.toString() + File.separator + filename)
        zipFile.addFolder(new File(ditaOutputDirectory))

        return zipFile.getFile()
    }

    static void overwriteGithubDir(String outputPath){

        if(TEST_GITHUB) {
            FileUtils.copyDirectory(new File(TEST_GITHUB_DIR), new File(outputPath))
        } else {
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
    }

    static Topic getFlatIndexTopic(Map<String, List<NhsDataDictionaryComponent>> componentMap, String indexPrefix, String indexTopicTitle) {

        Topic.build(
            id: "${indexPrefix}.index"
        ) {
            title indexTopicTitle
            titlealts {
                searchtitle "All Items: ${indexTopicTitle}"
            }
            componentMap.each {alphaIndex, componentList ->
                topic (id: "${indexPrefix}.index.${alphaIndex}"){
                    title alphaIndex.toUpperCase()
                    body {
                        simpletable(relColWidth: ["10*"], outputClass: "table table-striped table-sm") {
                            stHead(outputClass: "thead-light") {
                                stentry "Item Name"
                            }
                            componentList.each {component ->
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


    static void generateIndexTopics(NhsDataDictionary dataDictionary, DitaProject2 ditaProject, PublishOptions publishOptions) {

        if(publishOptions.isPublishAttributes()) {
            Topic attributesIndexTopic = getFlatIndexTopic(dataDictionary.componentsByIndex(dataDictionary.attributes.values(), false),
                                                           "attributes", "Attributes Index")
            ditaProject.addTopicToMainMap("", attributesIndexTopic, Toc.YES)
        }
        if(publishOptions.isPublishElements()) {
            Topic elementsIndexTopic = getFlatIndexTopic(dataDictionary.componentsByIndex(dataDictionary.elements.values(), false),
                                                         "elements", "Data Elements Index")
            ditaProject.addTopicToMainMap("", elementsIndexTopic, Toc.YES)
        }
        if(publishOptions.isPublishClasses()) {
            Topic classesIndexTopic = getFlatIndexTopic(dataDictionary.componentsByIndex(dataDictionary.classes.values(), false),
                                                        "classes", "Classes Index")
            ditaProject.addTopicToMainMap("", classesIndexTopic, Toc.YES)
        }
        if(publishOptions.isPublishBusinessDefinitions()) {
            Topic nhsBusinessDefinitionsIndexTopic = getFlatIndexTopic(dataDictionary.componentsByIndex(dataDictionary.businessDefinitions.values()
                                                                                                        , false),
                                                                   "nhsBusinessDefinitions", "NHS Business Definitions Index")
            ditaProject.addTopicToMainMap("", nhsBusinessDefinitionsIndexTopic, Toc.YES)

        }
        if(publishOptions.isPublishSupportingInformation()) {
            Topic supportingInformationIndexTopic = getFlatIndexTopic(dataDictionary.componentsByIndex(dataDictionary.supportingInformation.values
                                                                                                       (), false),
                                                                  "supportingInformation", "Supporting Information Index")
            ditaProject.addTopicToMainMap("", supportingInformationIndexTopic, Toc.YES)
        }

    }

    static void generateAllItemsIndex(NhsDataDictionary dataDictionary, DitaProject2 ditaProject, PublishOptions publishOptions) {
/*        DitaMap allItemsIndexMap = DitaMap.build(id: "allItemsIndex") {
            title "All Items Index"
        }

        ditaProject.addMapToMainMap("", allItemsIndexMap, Toc.YES)
*/
        List<TopicRef> topicRefs = []

        dataDictionary.allComponentsByIndex(true).each {alphaIndex, components ->
            Topic indexPage = Topic.build (id: "allItemsIndex-${alphaIndex}") {
                title "${alphaIndex}"
                body {
                    simpletable(relColWidth: ["7*", "3*"], outputClass: "table table-striped table-sm") {
                        stHead(outputClass: "thead-light") {
                            stentry "Item Name"
                            stentry "Item Type"
                        }
                        components.each {component ->
                            strow {
                                stentry {
                                    xRef component.calculateXRef()
                                }
                                stentry component.stereotype
                            }
                        }
                    }
                }
            }
            topicRefs.add(TopicRef.build(href: "topics/allItemsIndex-${alphaIndex}.dita"))
            ditaProject.addTopic("", indexPage)
        }

        TopicRef indexTopicRef = TopicRef.build(keys: ['allItemsIndexDescription'],
                                                href:'topics/allItemsIndexDescription.dita',
                                                toc: Toc.YES,
                                                chunk: ['by-topic'],
                                                linking: Linking.NORMAL) {
            topicRefs.each {
                topicRef it
            }
        }
        Topic indexDescriptionTopic = Topic.build(id:'allItemsIndexDescription') {
            title "All Items Index"
            body {
                p "This is just a holding page and should be overridden before publication"
            }
        }
        ditaProject.addTopic('', indexDescriptionTopic)

        ditaProject.addTopicRefToMainMap(indexTopicRef)
    }

}
