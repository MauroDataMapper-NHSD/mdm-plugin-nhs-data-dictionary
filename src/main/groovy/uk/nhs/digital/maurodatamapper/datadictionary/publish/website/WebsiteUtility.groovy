/*
 * Copyright 2020-2023 University of Oxford and NHS England
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

import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.maurodatamapper.dita.DitaProject
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.DitaMap
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.TopicRef
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.TopicSet
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Linking
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Toc

import net.lingala.zip4j.ZipFile
import org.apache.commons.io.FileUtils
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSet
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetFolder
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishOptions

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat

@Slf4j
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

    static final String GITHUB_BRANCH_URL = "https://github.com/NHSDigital/DataDictionaryPublication/archive/refs/heads/master.zip"
    static final Boolean TEST_GITHUB = true
    static final String TEST_GITHUB_DIR = "/Users/james/git/metadata-catalogue/DataDictionaryPublication/Website"

    static final String TO_BE_OVERRIDDEN_TEXT = "This text should be overridden by custom text stored in a GitHub library"

    static File generateWebsite(NhsDataDictionary dataDictionary, Path outputPath, PublishOptions publishOptions) {

        DitaProject ditaProject = new DitaProject("NHS Data Model and Dictionary","nhs_data_dictionary")
        ditaProject.useTopicsFolder = false



        Map<String, NhsDataDictionaryComponent> pathLookup = [:]

        dataDictionary.getAllComponents().each { component ->
            ditaProject.addExternalKey(component.getDitaKey(), component.otherProperties["ddUrl"])
            pathLookup[component.getMauroPath()] = component
        }
        dataDictionary.allComponents.each {component ->
            component.replaceLinksInDefinition(pathLookup)
            component.updateWhereUsed()
        }


//        allStereotypes.each {name, stereotype ->
//
//            ditaProject.registerMap('', stereotype, "All ${name}", Toc.NO)
//        }


        if(publishOptions.publishDataSetFolders || publishOptions.publishDataSets) {
            DataSetsWebsiteHelper.dataSetsIndex(dataDictionary, publishOptions, ditaProject)
        }

        generateIndexTopics(dataDictionary, ditaProject, publishOptions)
        generateAllItemsIndex(dataDictionary, ditaProject, publishOptions)

        dataDictionary.allComponents.
            sort {it.name }.
            each {component ->
                if(!(component instanceof NhsDDDataSet || component instanceof  NhsDDDataSetFolder)) {
                    if (publishOptions.isPublishableComponent(component)) {
                        String path = "${component.getPluralStereotypeForWebsite()}/${component.getDitaKey()}"
                        ditaProject.registerTopic(path, component.generateTopic())
                    }
                }
            }



        String ditaOutputDirectory = outputPath.toString() + File.separator + "dita"
        ditaProject.writeToDirectory(Paths.get(ditaOutputDirectory))

        log.error(ditaOutputDirectory)
        overwriteGithubDir(ditaOutputDirectory)


        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy")
        String date = simpleDateFormat.format(new Date())

        String filename = dataDictionary.branchName + '-' + date + ".zip"

        long startTime = System.currentTimeMillis()
        ZipFile zipFile = new ZipFile(outputPath.toString() + File.separator + filename)
        //zipFile.addFolder(new File(ditaOutputDirectory))
        log.info('Zip complete in {}', Utils.timeTaken(startTime))

        return zipFile.getFile()
    }

    static void overwriteGithubDir(String outputPath){

        if(TEST_GITHUB) {
            //FileUtils.copyDirectory(new File(TEST_GITHUB_DIR), new File(outputPath))
            moveDirectory(new File(TEST_GITHUB_DIR), new File(outputPath))
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

    static List<Topic> getFlatIndexTopics(Map<String, List<NhsDataDictionaryComponent>> componentMap, String indexPrefix, String indexTopicTitle) {

        return componentMap.collect {alphaIndex, componentList ->
            Topic.build (id: "${indexPrefix}-index-${alphaIndex}"){
                title alphaIndex.toUpperCase()
                body {
                    simpletable(relColWidth: ["10*"], outputClass: "table table-sm table-striped") {
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

    static Topic getFlatIndexTopic(Map<String, NhsDataDictionaryComponent> componentMap, String indexPrefix, String indexTopicTitle) {

        Topic.build(
            id: "${indexPrefix}-index"
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

    static void generateIndexTopics(NhsDataDictionary dataDictionary, DitaProject ditaProject, PublishOptions publishOptions) {

        if(publishOptions.isPublishElements()) {
            generateIndexMap(ditaProject, "elements", "Elements", dataDictionary, dataDictionary.elements.values())

        }
        if(publishOptions.isPublishAttributes()) {
            generateIndexMap(ditaProject, "attributes", "Attributes", dataDictionary, dataDictionary.attributes.values())
        }
        if(publishOptions.isPublishClasses()) {
            generateIndexMap(ditaProject, "classes", "Classes", dataDictionary, dataDictionary.classes.values())
        }
        if(publishOptions.isPublishBusinessDefinitions()) {
            generateIndexMap(ditaProject, "nhsBusinessDefinitions", "NHS Business Definitions", dataDictionary, dataDictionary.businessDefinitions.values())

        }
        if(publishOptions.isPublishSupportingInformation()) {
            generateIndexMap(ditaProject, "supportingInformation", "Supporting Information", dataDictionary, dataDictionary.supportingInformation.values())
        }
    }

    static void generateAllItemsIndex(NhsDataDictionary dataDictionary, DitaProject ditaProject, PublishOptions publishOptions) {

        TopicSet indexTopicSet = TopicSet.build( id: "allItems-index-topicset", keyRef: "allItems-index-overview", navTitle: "All Items Index")

        dataDictionary.allComponentsByIndex(true).each {alphaIndex, components ->
            Topic indexPage = Topic.build (id: "allItems-index-${alphaIndex}") {
                title "All Items: ${alphaIndex}"
                body {
                    simpletable(relColWidth: ["7*", "3*"], outputClass: "table table-sm table-striped") {
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
            indexTopicSet.topicRef(TopicRef.build(keyRef: "allItems-index-${alphaIndex}"))
            ditaProject.registerTopic("allItems", indexPage)
        }



        DitaMap indexMap = DitaMap.build(id: 'allItems-index', toc: Toc.YES) {
            title "All Items Index"
            topicSet indexTopicSet
        }
        ditaProject.registerMap("", indexMap)

        Topic allItemsOverview = Topic.build {
            id 'allItems-index-overview'
            title 'All Items Index'
            shortdesc 'Lists all the items in the dictionary in alphabetical order'
            body {
                p TO_BE_OVERRIDDEN_TEXT
            }
        }
        ditaProject.registerTopic("", allItemsOverview)

        ditaProject.mainMap.mapRef {
            toc Toc.YES
            keyRef 'allItems-index'
        }
    }

    final static Map<String, String> shortDescMap = [
            "Elements": "Data Elements are the data items used withing Data Sets.",
            "Attributes": "The part of the data model describing the characteristics of Classes.  Attributes define the data within the model.",
            "Classes": "The part of the data model describing the aspects of the health and care business with significant characteristics.",
            "NHS Business Definitions":"The part of the data model that links the logical classes to the context of the health and care business.",
            "Supporting Information": "Provides information to help users understand content in the NHS Data Model and Dictionary. In addition you can also find the COVID-19 Population Risk Assessment Code List and General Practice Data for Planning and Research (GPDPR)."
    ]

    static void generateIndexMap(DitaProject ditaProject,
                     String lowercaseStereotype, String stereotype, NhsDataDictionary dataDictionary,
                     Collection<NhsDataDictionaryComponent> components) {
        DitaMap indexMap = DitaMap.build(id: "${lowercaseStereotype}-index") {
            title stereotype
        }

        Topic indexOverview = Topic.build {
            id "${lowercaseStereotype}-index-overview"
            title "${stereotype}"
            shortdesc shortDescMap[stereotype]?:"List of ${stereotype}"
            body {
                p TO_BE_OVERRIDDEN_TEXT
            }
        }
        ditaProject.registerTopic(lowercaseStereotype, indexOverview)


        TopicSet topicSet = TopicSet.build( id: "${lowercaseStereotype}-index-topicset",
                                            keyRef: "${lowercaseStereotype}-index-overview",
                                            chunk: ["to-content"],
                                            linking: Linking.NORMAL,
                                            navTitle: stereotype)
        indexMap.topicSet(topicSet)

        List<Topic> indexTopics = getFlatIndexTopics(dataDictionary.componentsByIndex(components, false),
                                                            lowercaseStereotype, stereotype)

        indexTopics.each {topic ->
            ditaProject.registerTopic(lowercaseStereotype, topic)
            topicSet.topicRef(keyRef:topic.id, linking: Linking.NORMAL)
        }
        ditaProject.registerMap("", indexMap)
        ditaProject.mainMap.mapRef {
            toc Toc.YES
            keyRef "${lowercaseStereotype}-index"
        }
    }

    private static void moveDirectory(File parentFrom, File parentTo) {
        log.warn("Moving " + parentFrom.toPath() + " to " + parentTo)

        for (File file : parentFrom.listFiles()) {

            // Is a regular file?
            if (!file.isDirectory()) { // Is a regular file
                File newName = new File(parentTo, file.getName())
                file.renameTo(newName)
                log.warn("Moved " + file.getAbsolutePath() + " to " + newName.getAbsolutePath())
            } else { // Is a directory
                File newName = new File(parentTo, file.getName())
                newName.mkdirs()
                log.warn("Moving dir " + file.getAbsolutePath() + " to " + newName.getAbsolutePath())
                moveDirectory(file, newName)
            }
        }
    }

}
