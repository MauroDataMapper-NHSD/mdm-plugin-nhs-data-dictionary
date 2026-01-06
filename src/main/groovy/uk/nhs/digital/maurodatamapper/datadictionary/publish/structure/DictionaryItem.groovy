/*
 * Copyright 2020-2025 University of Oxford and NHS England
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
package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishHelper
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishTarget
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change

enum DictionaryItemState {
    ACTIVE,
    RETIRED,
    PREPARATORY
}

class DictionaryItem implements DitaAware<Topic>, HtmlAware, DiffAware<DictionaryItem, DictionaryItem> {
    final UUID id
    final UUID branchId
    final String stereotype
    final String name
    final DictionaryItemState state
    final String outputClass
    final String description

    final List<Section> sections = []

    DictionaryItem(
        UUID id,
        UUID branchId,
        String stereotype,
        String name,
        DictionaryItemState state,
        String outputClass,
        String description) {
        this.id = id
        this.branchId = branchId
        this.stereotype = stereotype
        this.name = name
        this.state = state
        this.outputClass = outputClass
        this.description = description
    }

    static DictionaryItem create(NhsDataDictionaryComponent component) {
        new DictionaryItem(
            component.catalogueItemId,
            component.branchId,
            component.stereotype,
            component.name,
            component.itemState,
            component.outputClass,
            component.shortDescription)
    }

    DictionaryItem addSection(Section section) {
        if (section) {
            sections.add(section)
        }
        this
    }

    String getOfficialName() {
        PublishHelper.createOfficialName(name, state)
    }

    String getXrefId() {
        PublishHelper.createXrefId(stereotype, name, state)
    }

    @Override
    DictionaryItem produceDiff(DictionaryItem previous) {

        boolean formatLengthSectionChanged = false;
        boolean defaultCodeSectionChanged = false;
        boolean descriptionSectionChanged = false;
        List<Section> diffSections = []
        this.sections.each {currentSection ->
            Section previousSection = previous ? previous.sections.find { it.type == currentSection.type } : null
            Section diffSection = currentSection.produceDiff(previousSection)
            if (diffSection) {
                diffSections.add(diffSection)

                if("Data Element".equals(this.stereotype)){
                    
                    if("formatLength".equals(currentSection.type)){
                        formatLengthSectionChanged = true;
                    }
                    if("defaultCodes".equals(currentSection.type)){
                        defaultCodeSectionChanged = true;
                    }
                }

                if("Attribute".equals(this.stereotype)){

                    if("description".equals(currentSection.type)){
                        descriptionSectionChanged = true;
                    }
                }
            } else {

                if("Data Element".equals(this.stereotype)){

                    if("formatLength".equals(currentSection.type)){
                        Section formatlengthsection = ((FormatLengthSection)currentSection).addFormatLengthSection(previousSection)
                        diffSections.add(formatlengthsection)
                        log.warn("came here formatLength::"+formatLengthSectionChanged)
                    }
                    if("defaultCodes".equals(currentSection.type)){
                        Section defaultCodeSection = ((CodesSection)currentSection).addDefaultCodeSection()
                        if(defaultCodeSection == null){
                            log.warn("Section is null");
                            diffSections.add(defaultCodeSection)
                        }
                        log.warn("came here defaultcodes::"+defaultCodeSectionChanged)
                    }
                }

                if("Attribute".equals(this.stereotype)){

                    if("description".equals(currentSection.type)){
                        Section descriptionSection = ((DescriptionSection)currentSection).addDescriptionSection()
                        diffSections.add(descriptionSection)
                        log.warn("came here descriptionSectionChanged::"+descriptionSectionChanged)
                    }
                }
            }
        }

        if (diffSections.empty) {
            return null
        }

        DictionaryItem diff = new DictionaryItem(
            this.id,
            this.branchId,
            this.stereotype,
            this.name,
            this.state,
            this.outputClass,
            getSummaryOfSectionTitlesForDiff(diffSections,this.stereotype,formatLengthSectionChanged,defaultCodeSectionChanged,descriptionSectionChanged))

        diffSections.each {diffSection ->
            diff.addSection(diffSection)
        }

        diff
    }

    @Override
    Topic generateDita(PublishContext context) {
        String titleOutputClass = context.target == PublishTarget.WEBSITE ? this.outputClass : ""

        String topicTitle = context.target == PublishTarget.WEBSITE ? getOfficialName() : name

        String shortDescription = context.target == PublishTarget.CHANGE_PAPER
            ? "Change to ${stereotype}: ${description}"
            : description

        Topic.build(id: xrefId) {
            title(outputClass: titleOutputClass) {
                text topicTitle
            }
            shortdesc shortDescription

            if (context.target == PublishTarget.CHANGE_PAPER) {
                body {
                    this.sections.each { section ->
                        div section.generateDita(context) as Div
                    }
                }
            }
            else {
                this.sections.each { section ->
                    topic section.generateDita(context) as Topic
                }
            }
        }
    }

    @Override
    String generateHtml(PublishContext context) {
        context.target == PublishTarget.CHANGE_PAPER
            ? generateChangePaperHtml(context)
            : generateWebsiteHtml(context)
    }

    private String generateWebsiteHtml(PublishContext context) {
        StringWriter writer = new StringWriter()
        MarkupBuilder builder = PublishHelper.createMarkupBuilder(writer, context.prettyPrintHtml)

        String titleCssClass = "${HtmlConstants.CSS_TOPIC_TITLE} ${outputClass}"

        builder.h1(class: titleCssClass) {
            mkp.yield(getOfficialName())
        }

        if (description) {
            builder.div(class: HtmlConstants.CSS_TOPIC_BODY) {
                builder.p(class: HtmlConstants.CSS_TOPIC_SHORTDESC) {
                    mkp.yield(description)
                }
            }
        }

        writer.toString()
    }

    private String generateChangePaperHtml(PublishContext context) {
        StringWriter writer = new StringWriter()
        MarkupBuilder builder = PublishHelper.createMarkupBuilder(writer, context.prettyPrintHtml)

        builder.div {
            h3 name
            h4 "Change to ${stereotype}: ${description}"
            div {
                this.sections.each { section ->
                    section.buildHtml(context, builder)
                }
            }
        }

        writer.toString()
    }

    private static String getSummaryOfSectionTitlesForDiff(List<Section> sections,String type,boolean formatLengthSectionChanged,
        boolean defaultCodeSectionChanged,boolean descriptionSectionChanged) {
        if (sections.empty) {
            return ""
        }

        if (sections.any {it.title == Change.NEW_TYPE }) {
            return Change.NEW_TYPE
        }

        if (sections.any {it.title == Change.RETIRED_TYPE }) {
            return Change.RETIRED_TYPE
        }

        List<Section> finalSectionList  = []
        sections.each{element -> 
                            if("Data Element".equals(type) && element.type.equals("formatLength")){

                                 if(formatLengthSectionChanged){

                                    finalSectionList.add(element)
                                 } 

                            } else if ("Data Element".equals(type) && element.type.equals("defaultCodes")){

                                 if(defaultCodeSectionChanged){

                                    finalSectionList.add(element)
                                 } 

                            } else if ("Attribute".equals(type) && element.type.equals("description")){

                                 if(descriptionSectionChanged){

                                    finalSectionList.add(element)
                                 } 

                            } else {
                                finalSectionList.add(element)
                            }  
                               
                      }

        finalSectionList . collect {it.title }.join(", ")
    }
}
