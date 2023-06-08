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
package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList

import groovy.util.logging.Slf4j

@Slf4j
class NhsDDClass implements NhsDataDictionaryComponent <DataClass> {

    @Override
    String getStereotype() {
        "Class"
    }

    @Override
    String getStereotypeForPreview() {
        "class"
    }

    @Override
    String getPluralStereotypeForWebsite() {
        "classes"
    }

    List<NhsDDCode> codes = []

    List<NhsDDClass> extendsClasses = []

    List<NhsDDAttribute> keyAttributes = []
    List<NhsDDAttribute> otherAttributes = []

    List<NhsDDClassRelationship> classRelationships = []

    List<NhsDDClassLink> classLinks = []

    @Override
    String calculateShortDescription() {
        if(isPreparatory()) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                String firstSentence = getFirstSentence()
                if (firstSentence.toLowerCase().contains("a subtype of")) {
                    String secondSentence = getSentence(1)
                    return secondSentence
                } else {
                    return firstSentence
                }
            } catch (Exception e) {
                log.error("Couldn't parse: " + definition)
                e.printStackTrace()
                return name
            }
        }
    }

    @Override
    String getXmlNodeName() {
        "DDClass"
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)

        xml.property.each { property ->
            String attributeUin = property.referencedElement.text()
            NhsDDAttribute attribute = dataDictionary.attributesByUin[attributeUin]
            if(attribute) {
                if("true" == property.isUnique.text()) {
                    keyAttributes.add(attribute)
                } else {
                    otherAttributes.add(attribute)
                }
            } else {
                log.debug("Cannot find attributeElement with Uin: " + attributeUin)
            }
        }
        classLinks = xml.link.collect{it -> new NhsDDClassLink(it)}
        dataDictionary.classesByUin[getUin()] = this
    }

    List<NhsDDAttribute> allAttributes() {
        keyAttributes + otherAttributes
    }

    String getMauroPath() {
        if(isRetired()) {
            "dm:${NhsDataDictionary.CLASSES_MODEL_NAME}|dc:${NhsDataDictionary.DATA_CLASSES_CLASS_NAME}|dc:Retired|dc:${name}"
        } else {
            return "dm:${NhsDataDictionary.CLASSES_MODEL_NAME}|dc:${NhsDataDictionary.DATA_CLASSES_CLASS_NAME}|dc:${name}"
        }
    }

    @Override
    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []
        topics.add(descriptionTopic())
        if(allAttributes()) {
            topics.add(classLinksTopic())
        }
        if(classRelationships) {
            topics.add(classRelationshipsTopic())
        }
        if(whereUsed) {
            topics.add(whereUsedTopic())
        }
        if(getAliases()) {
            topics.add(aliasesTopic())
        }
        return topics
    }

    Topic classLinksTopic() {
        Topic.build (id: getDitaKey() + "_attributes") {
            title "Attributes"
            body {
                simpletable(relColWidth: new SpaceSeparatedStringList (["1*", "9*"]), outputClass: "table table-sm") {
                    stHead (outputClass: "thead-light") {
                        stentry "Key"
                        stentry "Attribute Name"
                    }
                    keyAttributes.sort {it.name.toLowerCase()}.each {attribute ->
                        strow {
                            stentry 'Key'
                            stentry {
                                xRef attribute.calculateXRef()
                            }
                        }
                    }
                    otherAttributes.sort {it.name.toLowerCase()}.each {attribute ->
                        strow {
                            stentry ''
                            stentry {
                                xRef attribute.calculateXRef()
                            }
                        }
                    }

                }
            }
        }
    }
    Topic classRelationshipsTopic() {
        Topic.build(id: getDitaKey() + "_relationships") {
            title "Relationships"
            body {
                simpletable(relColWidth: new SpaceSeparatedStringList (["1*", "5*", "5*"]), outputClass: "table table-sm") {
                    stHead(outputClass: "thead-light") {
                        stentry "Key"
                        stentry "Relationship"
                        stentry "Class"
                    }
                    classRelationships.sort {it.targetClass.name.toLowerCase()}.each {relationship ->
                        strow {
                            stentry relationship.isKey?'Key':''
                            stentry relationship.relationshipDescription
                            stentry {
                                xRef relationship.targetClass.calculateXRef()
                            }
                        }
                    }
                }
            }
        }
    }
}
