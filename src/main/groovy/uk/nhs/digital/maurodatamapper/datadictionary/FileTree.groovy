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

import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Body
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Map
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.MapRef
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Ref
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Topic
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.TopicRef
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.TopicSet

@Slf4j
class FileTree {

    String name
    String key
    String titleClass
    List<FileTree> children = []
    Topic parentTopic
    List<Topic> topics = []

    void print(String indent = "") {
        log.trace(indent + name)
        children.each{
            it.print(indent + "  ")
        }
        topics.each {
            log.trace(indent + "  " + it.title + "(Topic)")
        }
    }

    void addTopicsAtPath(List<String> path, Topic parentTopic, List<Topic> topics, String key = '') {
        if(!path || path.size() == 0) {
            this.topics.addAll(topics)
            if(key) {
                this.key = key
            }
            if(parentTopic) {
                this.parentTopic = parentTopic
            }
        }
        else {
            List<String> newPath = []
            newPath.addAll(path)
            String root = newPath.remove(0)
            FileTree child = children.find{it.name == root}
            if(!child) {
                child = new FileTree(name: root)
                children.add(child)
            }
            child.addTopicsAtPath(newPath, parentTopic, topics, key)
        }
    }

    void writeToFiles(String path, String prevId = null, boolean toc = false, DataDictionary dataDictionary) {
        String ditaName = DDHelperFunctions.makeValidDitaName(name)
        String id = ditaName
        if(prevId) {
            id = prevId + "." + id
        }
        Map map = new Map()
        TopicSet topicSet = new TopicSet(navtitle: name, id: id + ".group", toc: toc)
        if(ditaName == "data_sets") {
            topicSet.href = ditaName + "_overview.dita"
            /*topicSet.keys = [ditaName + "_introduction"] */
        } else if(ditaName == "all_items_index__a-z_") {
            topicSet.href = "all_items_index_overview.dita"
            /*topicSet.keys = ["all_items_index_overview"] */
        }
        else {
            topicSet.href = ditaName + ".dita"
            //topicSet.keys = [ditaName]
        }
        map.refs.add(topicSet)
        if(name.equalsIgnoreCase("Retired") || path.contains("retired")) {
            topicSet.toc = false
        }

        Topic indexTopic = new Topic(title: name, id: id)
        indexTopic.body = new Body()

        if(path.contains("data_sets") || path.contains("messages")) {

            DDWebPage matchingWebPage = dataDictionary.webPages.values().find {
                it.name == name + " Introduction" ||
                it.name == name + "_Introduction" ||
                it.stringValues["aliasSchema"] == name
            }
            if(matchingWebPage) {
                log.debug("matching: " + matchingWebPage.name)
                indexTopic.body.bodyElements.add(new Html(content: DDHelperFunctions.parseHtml(matchingWebPage.definition) ?: ''))
            }

        }

        if(topics.size() == 0) {
            children.sort{it.name}.each {child ->
                MapRef mapRef = new MapRef(
                        href: ditaName + "/" + DDHelperFunctions.makeValidDitaName(child.name) + ".ditamap",
                        //keys: [ditaName + "_" + DDHelperFunctions.makeValidDitaName(child.name)],
                        toc: toc,
                )
                if(name.equalsIgnoreCase("Retired") || child.name.equalsIgnoreCase("Retired") || path.contains("retired")) {
                    mapRef.toc = false
                }
                topicSet.refs.add(mapRef)
                child.writeToFiles(path + "/" + ditaName, id, toc, dataDictionary)

                //XRef xref = new XRef(keyref: "datasets_${DDHelperFunctions.makeValidDitaName(child.name)}")
                //ul.entries.add(new Li(xref: xref))

            }
            map.title = name
        } else {
            topicSet.chunk = "to-content"
            indexTopic.title = name
            indexTopic.titleClass = titleClass
            topics.each { topic ->
                topicSet.refs.add(new TopicRef(href: ditaName + "/" + DDHelperFunctions.makeValidDitaName(topic.title) + ".dita", toc: false))
                //map.refs.add(new TopicRef(toc: false))
                /*if (this.key) {
                    topicSet.keys = [this.key]
                }*/
                topic.outputAsFile(path + "/" + ditaName + "/" + DDHelperFunctions.makeValidDitaName(topic.title) + ".dita")
            }
        }
        if(parentTopic) {
            parentTopic.outputAsFile(path + "/" + ditaName + ".dita")
        } else {
            indexTopic.outputAsFile(path + "/" + ditaName + ".dita")
        }
        map.outputAsFile(path + "/" + ditaName + ".ditamap")
    }

    Ref outputAsTopicRef(String absPath, String relPath, String prevId = null, Boolean toc = false) {
        String ditaName = DDHelperFunctions.makeValidDitaName(name)
        String id = ditaName

        if(prevId) {
            id = prevId + "." + id
        }

        if(parentTopic) {
            parentTopic.outputAsFile(absPath + "/" + ditaName + ".dita")
        } else {
            Topic indexTopic = new Topic(title: name,
                id: ditaName,
                body: new Body(),
                titleClass: titleClass)
            indexTopic.outputAsFile(absPath + "/" + ditaName + ".dita")
        }

        if(topics.size() > 0) {
            MapRef mapRef = new MapRef(href: relPath + "/" + ditaName + ".ditamap", toc: toc)
            Map map = new Map(title: name)
            TopicSet topicSet = new TopicSet(
                    navtitle: name,
                    id: id + ".group",
                    href: ditaName + ".dita",
                    chunk: "to-content" /*,
                    keys: [id.replaceAll("\\.", "_")]*/)
            topics.each {childTopic ->
                topicSet.refs.add(new TopicRef(href: ditaName + "/" + DDHelperFunctions.makeValidDitaName(childTopic.title) + ".dita",
                        toc: toc/*,
                        keys: [ditaName + "_" + DDHelperFunctions.makeValidDitaName(childTopic.title)]*/))
                childTopic.outputAsFile(absPath + "/" + ditaName + "/" + DDHelperFunctions.makeValidDitaName(childTopic.title) + ".dita")
                if(name.equalsIgnoreCase("Retired") || childTopic.title.equalsIgnoreCase("Retired") || relPath.contains("retired")) {
                    topicSet.toc = false
                }
            }
            map.refs.add(topicSet)
            map.outputAsFile(absPath + "/" + ditaName + ".ditamap")
            //topicRef.subTopics.add(mapRef)
            return mapRef
        } else if(children.size() > 0) {
            TopicRef topicRef = new TopicRef(href: relPath + "/" + ditaName + ".dita", toc: toc)
            if(name.equalsIgnoreCase("Retired") ) {
                topicRef.toc = false
            }
            children.sort{it.name}.each {fileTree ->
                topicRef.subTopics.add(fileTree.outputAsTopicRef(absPath + "/" + ditaName, relPath + "/" + ditaName, id, toc))
            }
            return topicRef
        } else {return null}
    }


}
