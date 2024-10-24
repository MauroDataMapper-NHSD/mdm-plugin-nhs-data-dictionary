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
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.meta.SpaceSeparatedStringList

import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.ChangeFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.ClassAttributeRow
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.ClassAttributeSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.ClassRelationshipRow
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.ClassRelationshipSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItem
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItemState
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.ItemLink

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
    DictionaryItem getPublishStructure() {
        DictionaryItem dictionaryItem = DictionaryItem.create(this)

        addDescriptionSection(dictionaryItem)

        if (itemState == DictionaryItemState.ACTIVE) {
            addClassAttributeSection(dictionaryItem)
            addClassRelationshipSection(dictionaryItem)
            addWhereUsedSection(dictionaryItem)
            addAliasesSection(dictionaryItem)
        }

        addChangeLogSection(dictionaryItem)

        dictionaryItem
    }

    void addClassAttributeSection(DictionaryItem dictionaryItem) {
        List<ClassAttributeRow> keyRows = keyAttributes
            .findAll { attribute -> !attribute.isRetired() }
            .sort { attribute -> attribute.name.toLowerCase() }
            .collect { attribute -> new ClassAttributeRow(attribute.isKey, ItemLink.create(attribute))}

        List<ClassAttributeRow> otherRows = otherAttributes
            .findAll { attribute -> !attribute.isRetired() }
            .sort { attribute -> attribute.name.toLowerCase() }
            .collect { attribute -> new ClassAttributeRow(attribute.isKey, ItemLink.create(attribute))}

        List<ClassAttributeRow> allRows = keyRows + otherRows

        dictionaryItem.addSection(new ClassAttributeSection(dictionaryItem, allRows))
    }

    void addClassRelationshipSection(DictionaryItem dictionaryItem) {
        if (classRelationships) {
            List<ClassRelationshipRow> rows = allRelationships().collect {relationship ->
                new ClassRelationshipRow(
                    relationship.isKey,
                    relationship.relationshipDescription,
                    ItemLink.create(relationship.targetClass))
            }

            dictionaryItem.addSection(new ClassRelationshipSection(dictionaryItem, rows))
        }
    }

    @Override
    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []
        topics.add(descriptionTopic())
        if (isActivePage()) {
            topics.add(attributesTopic())
            if (classRelationships) {
                topics.add(classRelationshipsTopic())
            }
            if (whereUsed) {
                topics.add(whereUsedTopic())
            }
            if (getAliases()) {
                topics.add(aliasesTopic())
            }
        }
        topics.add(changeLogTopic())
        return topics
    }

    Topic attributesTopic() {
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

    @Deprecated
    @Override
    List<Change> getChanges(NhsDataDictionaryComponent previousComponent) {
        List<Change> changes = []

        Change descriptionChange = createDescriptionChange(previousComponent)
        if (descriptionChange) {
            changes.add(descriptionChange)
        }

        if (isActivePage()) {
            Change attributesChange = createAttributesChange(previousComponent as NhsDDClass)
            if (attributesChange) {
                changes.add(attributesChange)
            }

            Change relationshipsChange = createRelationshipsChange(previousComponent as NhsDDClass)
            if (relationshipsChange) {
                changes.add(relationshipsChange)
            }

            Change aliasesChange = createAliasesChange(previousComponent)
            if (aliasesChange) {
                changes.add(aliasesChange)
            }
        }

        changes
    }

    Change createAttributesChange(NhsDDClass previousClass) {
        List<NhsDDAttribute> currentAttributes = this.allAttributes().findAll { !it.isRetired() }
        List<NhsDDAttribute> previousAttributes = previousClass ? previousClass.allAttributes().findAll { !it.isRetired() } : []

        if (ChangeFunctions.areEqual(currentAttributes, previousAttributes)) {
            // Identical lists. If this is a new item, this will never be true so will always include an "Attributes" change
            return null
        }

        List<NhsDDAttribute> newAttributes = ChangeFunctions.getDifferences(currentAttributes, previousAttributes)
        List<NhsDDAttribute> removedAttributes = ChangeFunctions.getDifferences(previousAttributes, currentAttributes)

        if (newAttributes.empty && removedAttributes.empty) {
            return null
        }

        StringWriter htmlWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(htmlWriter)

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

        createChange(Change.CHANGED_ATTRIBUTES_TYPE, previousClass, htmlWriter)
    }

    Change createRelationshipsChange(NhsDDClass previousClass) {
        List<NhsDDClassRelationship> currentRelationships = this.allRelationships()
        List<NhsDDClassRelationship> previousRelationships = previousClass ? previousClass.allRelationships() : []

        if (ChangeFunctions.areEqual(currentRelationships, previousRelationships)) {
            // Identical lists. If this is a new item, this will never be true so will always include a "Relationships" change
            return null
        }

        List<NhsDDClassRelationship> newRelationships = ChangeFunctions.getDifferences(currentRelationships, previousRelationships)
        List<NhsDDClassRelationship> removedRelationships = ChangeFunctions.getDifferences(previousRelationships, currentRelationships)

        if (newRelationships.empty && removedRelationships.empty) {
            return null
        }

        StringWriter htmlWriter = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(htmlWriter)

        String title = "Each $name"

        markupBuilder.div {
            p title
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
                        if (newRelationships.any {it.discriminator == relationship.discriminator}) {
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

        createChange(Change.CHANGED_RELATIONSHIPS_TYPE, previousClass, htmlWriter)
    }
}
