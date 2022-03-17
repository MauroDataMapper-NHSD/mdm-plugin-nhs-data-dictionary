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
package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType

import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Body
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.KeyDef
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Map
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.MapRef
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Ref
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STEntry
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STHead
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STRow
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.SimpleTable
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Topic
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.TopicRef
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.TopicSet

import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

@InheritConstructors
@Slf4j
class GenerateDita {


    DataDictionary dataDictionary
    DataDictionaryOptions options

    static Pattern pattern = Pattern.compile("(\\[([^\\]]*)\\])\\([^\\)]*\\)")

    List<String> unmatchedUrls = []
    String outputDir

    GenerateDita(DataDictionary dataDictionary1, DataDictionaryOptions options) {
        dataDictionary = dataDictionary1
        this.options = options
        outputDir = options.ditaOutputDir
    }

    GenerateDita(DataDictionary dataDictionary1, String outputDir) {
        dataDictionary = dataDictionary1
        this.options = null
        this.outputDir = outputDir
    }

    static void cleanSourceTarget(String ditaOutputDir) {
        Files.createDirectories(Paths.get(ditaOutputDir))
        //Files.createDirectories(Paths.get(targetPath))

        FileUtils.cleanDirectory(new File(ditaOutputDir))
        //FileUtils.cleanDirectory(new File(targetPath))

    }

    void generateInput() {

        //DataClass classesDataClass = dataDictionary.sourceModel.getChildDataClasses().find{it.label == "Classes"}
        //DataClass attributesDataClass = dataDictionary.sourceModel.getChildDataClasses().find{it.label == "Attributes"}
        //DataClass elementsDataClass = dataDictionary.sourceModel.getChildDataClasses().find{it.label == "Elements"}


        Map map = new Map()
        map.title = "NHS Data Dictionary"

        Topic homeTopic = new Topic(id: "index", title: "Home", abstr: "NHS Data Model and Dictionary (Version 3)")
        homeTopic.body = new Body()
        homeTopic.body.bodyElements.add(new Html(content: "<p><b>Welcome to the NHS Data Model and Dictionary for England</b></p>"))
        homeTopic.body.bodyElements.add(new Html(content: "<p>If you would like to:</p>" +
                "<ul>" +
                "<li>know more about us or need help using the NHS Data Model and Dictionary, see the Help " +
                "pages</li>\n" +
                "<li>view our Frequently Asked Questions, see Frequently Asked Questions.</li></ul>"))
        homeTopic.body.bodyElements.add(new Html(content: "<p>The NHS Data Model and Dictionary provides a reference point for approved " +
                "Information Standards Notices to support health care activities within the NHS in " +
                "England. It has been developed for everyone who is actively involved in the collection " +
                "of data and the management of information in the NHS.</p>"))
        homeTopic.body.bodyElements.add(new Html(content: "<p>The NHS Data Model and Dictionary is maintained and published by the NHS Data Model " +
                "and Dictionary Service and all changes are governed by the Data Coordination Board " +
                "(DCB) process. Changes are published as Information Standards Notices (ISN) and Data " +
                "Dictionary Change Notices (DDCN).</p>"))

        /*map.topicRefs.addAll([homeTopicRef, classesTopicRef, attributesTopicRef, elementsTopicRef, datasetsTopicRef, busDefsTopicRef,
                              collectionsTopicRef, allTopicRef])*/
        //TopicRef homeTopicRef = new TopicRef(href: "index.dita", keys: ["index"])
        //File homeTopicFile = new File(sourcePath + "/index.dita")
        //homeTopic.outputAsFile(homeTopicFile)

        //map.refs.addAll(homeTopicRef)

        Map keyMap = new Map(title: "Key Definitions")

        if (!options || options.processClasses) {
            log.info "Generating classes..."
            MapRef classesMapRef = new MapRef(href: "classes.ditamap", toc: false)
            generateClasses(keyMap)
            map.refs.add(classesMapRef)
            MapRef indexMapRef = new MapRef(href: "classes-index.ditamap")
            map.refs.add(indexMapRef)

        }

        if (!options || options.processAttributes) {
            log.info "Generating attributes..."
            //MapRef attributesMapRef = new MapRef(href: "attributes.ditamap")

            MapRef attributesMapRef = new MapRef(href: "attributes.ditamap", toc: false)
            generateAttributes(keyMap)
            map.refs.add(attributesMapRef)
            MapRef indexMapRef = new MapRef(href: "attributes-index.ditamap")
            map.refs.add(indexMapRef)
        }
        if (!options || options.processElements) {
            log.info "Generating elements..."
            MapRef elementsMapRef = new MapRef(href: "data_elements.ditamap", toc: false)
            generateElements(keyMap)
            map.refs.add(elementsMapRef)
            MapRef indexMapRef = new MapRef(href: "data_elements-index.ditamap", toc: true)
            map.refs.add(indexMapRef)

        }
        if (!options || options.processDataSets) {
            log.info "Generating data sets..."
            generateDataSets(keyMap)
            MapRef mapRef = new MapRef(href: "data_sets.ditamap")
            map.refs.add(mapRef)
        }
        if (!options || options.processNhsBusinessDefinitions) {
            log.info "Generating business definitions..."
            generateBusinessDefinitions(keyMap)
            MapRef mapRef = new MapRef(href: "nhs_business_definitions.ditamap", toc: false)
            map.refs.add(mapRef)
            MapRef indexMapRef = new MapRef(href: "nhs_business_definitions-index.ditamap")
            map.refs.add(indexMapRef)

        }
        if (!options || options.processSupportingDefinitions) {
            log.info "Generating supporting information..."
            generateSupportingDefinitions(keyMap)
            MapRef mapRef = new MapRef(href: "supporting_information.ditamap", toc: false)
            map.refs.add(mapRef)
            MapRef indexMapRef = new MapRef(href: "supporting_information-index.ditamap")
            map.refs.add(indexMapRef)
        }
        if (!options || options.processXmlSchemaConstraints) {
            log.info "Generating xml schema constraints..."
            generateXmlSchemaConstraints(keyMap)
            MapRef mapRef = new MapRef(href: "xml_schema_constraints.ditamap", toc: false)
            map.refs.add(mapRef)
            //MapRef indexMapRef = new MapRef(href: "xml_schema_constraints-index.ditamap")
            //map.refs.add(indexMapRef)

        }

        generateAllItemsIndex()
        MapRef allTopicRef = new MapRef(href: "all_items_index__a-z_.ditamap")
        map.refs.addAll(allTopicRef)

        keyMap.outputAsFile(outputDir + "/keydefs.ditamap")

        MapRef keyMapRef = new MapRef(href: "keydefs.ditamap", toc: false)
        map.refs.add(keyMapRef)

        //File outputFile = new File()
        map.outputAsFile(outputDir + "/" + "map.ditamap")

    }

    void generateClasses(Map keyMap) {

        writeToTree("Classes", dataDictionary.classes.values(), keyMap, false)
        generateIndex(dataDictionary.classes.values(), "classes", "Classes")
    }

    void generateDataSets(Map keyMap) {

        writeToTree("Data Sets", dataDictionary.dataSets.values(), keyMap, true)



    }

    Ref generateAttributes(Map keyMap) {

        writeToTree("Attributes", dataDictionary.attributes.values(), keyMap, false)
        generateIndex(dataDictionary.attributes.values(), "attributes", "Attributes")
    }

    void generateElements(Map keyMap) {
        writeToTree("Data Elements", dataDictionary.elements.values(), keyMap, false)
        generateIndex(dataDictionary.elements.values(), "data_elements", "Data Elements")
    }


    void generateBusinessDefinitions(Map keyMap) {

        writeToTree("NHS Business Definitions", dataDictionary.businessDefinitions.values(), keyMap, false)
        generateIndex(dataDictionary.businessDefinitions.values(), "nhs_business_definitions", "NHS Business Definitions")
    }

    void generateSupportingDefinitions(Map keyMap) {

        writeToTree("Supporting Information", dataDictionary.supportingInformation.values(), keyMap, false)
        generateIndex(dataDictionary.supportingInformation.values(), "supporting_information", "Supporting Information")

    }

    void generateXmlSchemaConstraints(Map keyMap) {

        writeToTree("XML Schema Constraints", dataDictionary.xmlSchemaConstraints.values(), keyMap, false)
        //generateIndex(dataDictionary.xmlSchemaConstraints.values(), "xml_schema_constraints", "XML Schema Constraints")

    }

    static String makeDitaKey(String href) {
        String result = href.replaceAll("/", "_")
        result = result.replaceAll("\\.dita", "")

        return result

    }

    void replaceInternalLinksWithXRefs() {
        List<DataDictionaryComponent> allComponents = []
        allComponents.addAll(dataDictionary.attributes.values())
        allComponents.addAll(dataDictionary.elements.values())
        allComponents.addAll(dataDictionary.classes.values())
        allComponents.addAll(dataDictionary.dataSets.values())
        allComponents.addAll(dataDictionary.businessDefinitions.values())
        allComponents.addAll(dataDictionary.supportingInformation.values())
        allComponents.addAll(dataDictionary.xmlSchemaConstraints.values())
        allComponents.addAll(dataDictionary.webPages.values())

        allComponents.each { component ->
            replaceInternalLinkWithXRef(component)
        }


    }


    void replaceInternalLinkWithXRef(DataDictionaryComponent component) {
        String description = component.definition.toString()
        String newDescription = replaceXRefString(component, description)
        component.definition = newDescription

        if(component instanceof DDAttribute) {
            DataType dataType = ((DataElement)((DDAttribute) component).catalogueItem).dataType
            if(dataType && dataType instanceof EnumerationType) {
                ((EnumerationType) dataType).enumerationValues.each {enumValue ->
                    enumValue.metadata.each{metadata ->
                        metadata.value = replaceXRefString(component, metadata.value)
                    }
                }
            }
        }
        if(component instanceof DDElement) {
            DataType dataType = ((DataElement)((DDElement) component).catalogueItem).dataType
            if(dataType instanceof EnumerationType) {
                ((EnumerationType) dataType).enumerationValues.each {enumValue ->
                    enumValue.metadata.each{metadata ->
                        metadata.value = replaceXRefString(component, metadata.value)
                    }
                }
            }
        }


    }

    String replaceXRefString(DataDictionaryComponent component, String description) {
        String newDescription = description
        if (newDescription) {
            Matcher matcher = pattern.matcher(newDescription)
            while (matcher.find()) {
                String linkToFind = "]" + matcher.group(0).split("]")[1]
                if(!dataDictionary.internalLinks[linkToFind]) {
                    //log.debug("cannot match: " + linkToFind)
                    /*dataDictionary.internalLinks.each {
                        log.debug(it)
                    }*/
                }
                DataDictionaryComponent ddc = dataDictionary.internalLinks[linkToFind]
                if (ddc) {
                    String replacement =
                            "<xref outputclass=\"${ddc.getOutputClassForLink()}\" keyref=\"${ddc.getDitaKey()}\">${matcher.group(2)}</xref>"
                    newDescription = newDescription.replace(matcher.group(0), replacement)
                    ddc.whereMentioned.add(component)
                } else {
                    unmatchedUrls.add(matcher.group(0))
                }
            }
        }
        return newDescription
    }

    void generateAllItemsIndex() {
        List<DataDictionaryComponent> allItems = []
        allItems.addAll(dataDictionary.dataSets.values())
        allItems.addAll(dataDictionary.classes.values())
        allItems.addAll(dataDictionary.attributes.values())
        allItems.addAll(dataDictionary.elements.values())
        allItems.addAll(dataDictionary.businessDefinitions.values())
        allItems.addAll(dataDictionary.supportingInformation.values())
        allItems.addAll(dataDictionary.xmlSchemaConstraints.values())

        HashMap<Character, List<DataDictionaryComponent>> itemPages = [:]
        allItems.sort {it.name.toLowerCase()}.each {item ->
            Character prefix = item.name.charAt(0)
            if (prefix == ('1' as char) || prefix == '2' as char || prefix == '3' as char || prefix == '4' as char || prefix == '5' as char ||
                prefix == ('6' as char) || prefix == '7' as char || prefix == '8' as char || prefix == '9' as char) {
                prefix = ('0' as char)
            }
            prefix = prefix.toUpperCase()
            if (itemPages[prefix]) {
                itemPages[prefix].add(item)
            } else {
                itemPages[prefix] = [item]
            }
        }
        java.util.Map<String, Topic> prefixTopics = [:]
        itemPages.keySet().sort().each {prefix ->
            Topic prefixTopic = new Topic(id: "allItems.${prefix}", title: "Index",
                                          searchTitle: "All Items: ${prefix}")
            SimpleTable indexTable = new SimpleTable(relColWidth: "3* 2*")
            indexTable.stHead = new STHead(stEntries: [new STEntry(value: "Item Name"), new STEntry(value: "Item Type")])
            itemPages[prefix].each {de ->
                indexTable.stRows.add(new STRow(stEntries: [new STEntry(xref: de.getXRef()), new STEntry(value: de.getOutputClassForTable())]))
            }

            prefixTopic.body.bodyElements.add(indexTable)

            prefixTopics["All Items: " + prefix] = prefixTopic
        }

        FileTree allItemsTree = new FileTree(name: "All Items Index (A-Z)")
        prefixTopics.each {entry ->
            allItemsTree.addTopicsAtPath([entry.key],
                    null,
                    [entry.value],
                    "allItems_" + DDHelperFunctions.makeValidDitaName(entry.key))
        }
        //allItemsTree.print()
        //log.debug(sourcePath)
        allItemsTree.writeToFiles(outputDir, null, true, dataDictionary)


    }

    void generateIndex(Collection<DataDictionaryComponent> components, String keyPrefix, String header) {

        HashMap<Character, List<DataDictionaryComponent>> itemPages = [:]
        components
            .findAll{!it.isRetired}
            .sort {it.name.toLowerCase()}.each {item ->
                Character prefix = item.name.charAt(0)
                if (prefix == ('1' as char) || prefix == '2' as char || prefix == '3' as char || prefix == '4' as char || prefix == '5' as char ||
                        prefix == ('6' as char) || prefix == '7' as char || prefix == '8' as char || prefix == '9' as char) {
                    prefix = ('0' as char)
                }
                prefix = prefix.toUpperCase()
                if (itemPages[prefix]) {
                    itemPages[prefix].add(item)
                } else {
                    itemPages[prefix] = [item]
                }
            }
        java.util.Map<String, Topic> prefixTopics = [:]
        itemPages.keySet().sort().each {prefix ->
            String prefixTitle = prefix.toUpperCase()
            if(prefixTitle == "0") {
                prefixTitle = "0-9"
            }
            Topic prefixTopic = new Topic(id: "${keyPrefix}.${prefix.toLowerCase()}", title: prefixTitle,
                    searchTitle: "All Items: ${header}")
            SimpleTable indexTable = new SimpleTable(relColWidth: "10*")
            indexTable.stHead = new STHead(stEntries: [new STEntry(value: "Item Name")])
            itemPages[prefix].each {de ->
                indexTable.stRows.add(new STRow(stEntries: [new STEntry(xref: de.getXRef())]))
            }

            prefixTopic.body.bodyElements.add(indexTable)
            prefixTopic.outputAsFile(outputDir + "/" + keyPrefix + "/" + prefix.toLowerCase() + ".dita")
        }

        TopicSet indexTopicSet = new TopicSet(id: keyPrefix + "-index", chunk: "to-content", href: keyPrefix + "_overview.dita")
        itemPages.keySet().each {prefix ->
            TopicRef topicRef = new TopicRef(href: keyPrefix + "/" + prefix.toLowerCase() + ".dita", toc: true)
            indexTopicSet.refs.add(topicRef)
        }
        Topic indexTopic = new Topic(id: keyPrefix + "-index", title: header)
        indexTopic.outputAsFile(outputDir + "/" + keyPrefix + "-index.dita")
        Map indexMap = new Map(title: header)
        indexMap.refs.add(indexTopicSet)
        indexMap.outputAsFile(outputDir + "/" + keyPrefix.toLowerCase() + "-index.ditamap")

    }



    Ref writeToTree(String rootName, Collection<DataDictionaryComponent> components, Map keyMap, boolean toc = false) {
        FileTree fileTree = new FileTree(name: rootName)
        components.sort { it.name }.each { component ->
            List<Topic> topics = component.toDitaTopics(dataDictionary)
            List<String> topicPath = component.getPath()
            if(rootName != "Data Sets") {
                topicPath.add(DDHelperFunctions.makeValidDitaName(component.name))
            }
            fileTree.addTopicsAtPath(topicPath, component.getParentTopic(dataDictionary), topics)
            List<String> keyPath = topicPath.collect{it -> DDHelperFunctions.makeValidDitaName(it)}

//            String completePath = "https://datadictionary.nhs.uk/" + DDHelperFunctions.makeValidDitaName(rootName) + "/" + StringUtils.join(keyPath, "/") + ".html"
//            keyMap.refs.add(new KeyDef(href: completePath, scope: "external", keys: [component.getDitaKey()], format: "html"))

            String completePath = DDHelperFunctions.makeValidDitaName(rootName) + "/" + StringUtils.join(keyPath, "/")

            keyMap.refs.add(new KeyDef(href: completePath + ".dita", keys: [component.getDitaKey()]))
            topics.each {topic ->
                keyMap.refs.add(new KeyDef(href: completePath + "/" + DDHelperFunctions.makeValidDitaName(topic.title) + ".dita", keys: [component.getDitaKey() + "." + DDHelperFunctions.makeValidDitaName(topic.title)]))
            }


        }
        //fileTree.print()
        //log.debug(sourcePath)
        //fileTree.writeToFiles(sourcePath)
        return fileTree.writeToFiles(outputDir, null, toc, dataDictionary)
    }


}
