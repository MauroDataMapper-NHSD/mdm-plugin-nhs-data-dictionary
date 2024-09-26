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
package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.helpers.HtmlHelper
import uk.ac.ox.softeng.maurodatamapper.dita.meta.DitaElement
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList

import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change

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

                // Must track in the DD attribute that this is the owner class, required by the
                // attribute to understand hierarchical information in Mauro
                attribute.parentClass = this
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

    List<NhsDDClassRelationship> allRelationships() {
        classRelationships.sort { a, b ->
            b.isKey <=> a.isKey ?: a.targetClass.name.toLowerCase() <=> b.targetClass.name.toLowerCase()
        }
    }

    String getMauroPath() {
        if(isRetired()) {
            "dm:${NhsDataDictionary.CLASSES_MODEL_NAME}|dc:Retired|dc:${name}"
        } else {
            return "dm:${NhsDataDictionary.CLASSES_MODEL_NAME}|dc:${name}"
        }
    }

    @Override
    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []
        topics.add(descriptionTopic())
        topics.add(classLinksTopic())
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
        List<NhsDDAttribute> attributes = allAttributes().findAll { !it.isRetired() }

        Topic.build (id: getDitaKey() + "_attributes") {
            title "Attributes"
            body {
                if (attributes.size() == 0) {
                    p "This class has no attributes"
                } else {
                    simpletable(relColWidth: new SpaceSeparatedStringList(["1*", "9*"]), outputClass: "table table-sm") {
                        stHead(outputClass: "thead-light") {
                            stentry "Key"
                            stentry "Attribute Name"
                        }
                        keyAttributes
                            .findAll { !it.isRetired() }
                            .sort { it.name.toLowerCase() }
                            .each { attribute ->
                                strow {
                                    stentry 'Key'
                                    stentry {
                                        xRef attribute.calculateXRef()
                                    }
                                }
                            }
                        otherAttributes
                            .findAll { !it.isRetired() }
                            .sort { it.name.toLowerCase() }
                            .each { attribute ->
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
    }
    Topic classRelationshipsTopic() {
        Topic.build(id: getDitaKey() + "_relationships") {
            title "Relationships"
            body {
                p "Each $name:"
                simpletable(relColWidth: new SpaceSeparatedStringList (["1*", "5*", "5*"]), outputClass: "table table-sm") {
                    stHead(outputClass: "thead-light") {
                        stentry "Key"
                        stentry "Relationship"
                        stentry "Class"
                    }
                    allRelationships().each {relationship ->
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

    @Override
    void buildChangeList(List<Change> changes, NhsDataDictionaryComponent previousComponent, boolean includeDataSets) {
        // Run standard change checks first
        NhsDataDictionaryComponent.super.buildChangeList(changes, previousComponent, includeDataSets)

        NhsDDClass previousClass = previousComponent as NhsDDClass
        if (!previousClass) {
            return
        }

        Change changedAttributesChange = createChangedAttributesChange(previousClass)
        if (changedAttributesChange) {
            changes.add(changedAttributesChange)
        }

        Change changedRelationshipsChange = createChangedRelationshipsChange(previousClass)
        if (changedRelationshipsChange) {
            changes.add(changedRelationshipsChange)
        }
    }

    Change createChangedAttributesChange(NhsDDClass previousClass) {
        List<NhsDDAttribute> currentAttributes = this.allAttributes().findAll { !it.isRetired() }
        List<NhsDDAttribute> previousAttributes = previousClass.allAttributes().findAll { !it.isRetired() }

        // Find all attributes in this one but not the previous - "new attributes"
        List<NhsDDAttribute> newAttributes = currentAttributes.findAll { currentAttribute ->
            !previousAttributes.find {previousAttribute -> previousAttribute.name == currentAttribute.name }
        }

        // Find all the attributes in the previous one but not this one - "removed attributes"
        List<NhsDDAttribute> removedAttributes = previousAttributes.findAll {previousAttribute ->
            !currentAttributes.find {currentAttribute -> currentAttribute.name == previousAttribute.name }
        }

        if (newAttributes.empty && removedAttributes.empty) {
            return null
        }

        String htmlDetail = createAttributesTableChangeHtml(currentAttributes, newAttributes, removedAttributes)
        Div ditaDetail = HtmlHelper.replaceHtmlWithDita(htmlDetail)

        new Change(
            changeType: Change.CHANGED_ATTRIBUTES_TYPE,
            stereotype: stereotype,
            oldItem: previousClass,
            newItem: this,
            htmlDetail: htmlDetail,
            ditaDetail: ditaDetail
        )
    }

    Change createChangedRelationshipsChange(NhsDDClass previousClass) {
        List<NhsDDClassRelationship> currentRelationships = this.allRelationships()
        List<NhsDDClassRelationship> previousRelationships = previousClass.allRelationships()

        // Find all relationships in this one but not the previous - "new relationships"
        List<NhsDDClassRelationship> newRelationships = currentRelationships.findAll {currentRelationship ->
            !previousRelationships.find { previousRelationship ->
                previousRelationship.changePaperDiscriminator == currentRelationship.changePaperDiscriminator
            }
        }

        // Find all the relationships in the previous one but not this one - "removed relationships"
        List<NhsDDClassRelationship> removedRelationships = previousRelationships.findAll {previousRelationship ->
            !currentRelationships.find {currentRelationship ->
                currentRelationship.changePaperDiscriminator == previousRelationship.changePaperDiscriminator
            }
        }

        if (newRelationships.empty && removedRelationships.empty) {
            return null
        }

        String htmlDetail = createRelationshipsTableChangeHtml(currentRelationships, newRelationships, removedRelationships)
        Div ditaDetail = HtmlHelper.replaceHtmlWithDita(htmlDetail)

        new Change(
            changeType: Change.CHANGED_RELATIONSHIPS_TYPE,
            stereotype: stereotype,
            oldItem: previousClass,
            newItem: this,
            htmlDetail: htmlDetail,
            ditaDetail: ditaDetail
        )
    }

    static String createAttributesTableChangeHtml(
        List<NhsDDAttribute> currentAttributes,
        List<NhsDDAttribute> newAttributes,
        List<NhsDDAttribute> removedAttributes) {
        StringWriter stringWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(stringWriter)

        markupBuilder.div {
            p "Attributes of this Class are:"
            table(class: "attribute-table") {
                thead {
                    markupBuilder.th(width: "5%") {
                        mkp.yield("Key")
                    }
                    markupBuilder.th(width: "95%") {
                        mkp.yield("Attribute Name")
                    }
                }
                tbody {
                    currentAttributes.each { attribute ->
                        if (newAttributes.any { it.name == attribute.name }) {
                            markupBuilder.tr {
                                markupBuilder.td(class: "new") {
                                    mkp.yield(attribute.isKey ? "K": "")
                                }
                                markupBuilder.td(class: "new") {
                                    mkp.yield(attribute.name)
                                }
                            }
                        } else {
                            markupBuilder.tr {
                                td {
                                    mkp.yield(attribute.isKey ? "K": "")
                                }
                                td attribute.name
                            }
                        }
                    }
                    removedAttributes.each { attribute ->
                        markupBuilder.tr {
                            markupBuilder.td(class: "deleted") {
                                mkp.yield(attribute.isKey ? "K": "")
                            }
                            markupBuilder.td(class: "deleted") {
                                mkp.yield(attribute.name)
                            }
                        }
                    }
                }
            }
        }

        return stringWriter.toString()
    }

    static String createRelationshipsTableChangeHtml(
        List<NhsDDClassRelationship> currentRelationships,
        List<NhsDDClassRelationship> newRelationships,
        List<NhsDDClassRelationship> removedRelationships) {
        StringWriter stringWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(stringWriter)

        markupBuilder.div {
            p "Each $name"
            table(class: "relationship-table") {
                thead {
                    markupBuilder.th(width: "5%") {
                        mkp.yield("Key")
                    }
                    markupBuilder.th(width: "55%") {
                        mkp.yield("Relationship")
                    }
                    markupBuilder.th(width: "40%") {
                        mkp.yield("Class")
                    }
                }
                tbody {
                    currentRelationships.each {relationship ->
                        if (newRelationships.any {it.changePaperDiscriminator == relationship.changePaperDiscriminator}) {
                            markupBuilder.tr {
                                markupBuilder.td(class: "new") {
                                    mkp.yield(relationship.isKey ? "K" : "")
                                }
                                markupBuilder.td(class: "new") {
                                    mkp.yield(relationship.relationshipDescription)
                                }
                                markupBuilder.td(class: "new") {
                                    mkp.yield(relationship.targetClass.name)
                                }
                            }
                        } else {
                            markupBuilder.tr {
                                td {
                                    mkp.yield(relationship.isKey ? "K" : "")
                                }
                                td relationship.relationshipDescription
                                td relationship.targetClass.name
                            }
                        }
                    }
                    removedRelationships.each {relationship ->
                        markupBuilder.tr {
                            markupBuilder.td(class: "deleted") {
                                mkp.yield(relationship.isKey ? "K" : "")
                            }
                            markupBuilder.td(class: "deleted") {
                                mkp.yield(relationship.relationshipDescription)
                            }
                            markupBuilder.td(class: "deleted") {
                                mkp.yield(relationship.targetClass.name)
                            }
                        }
                    }
                }
            }
        }

        return stringWriter.toString()
    }
}
