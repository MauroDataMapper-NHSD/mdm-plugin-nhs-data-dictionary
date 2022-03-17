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

import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import groovy.util.logging.Slf4j
import groovy.xml.slurpersupport.GPathResult
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Topic

@Slf4j
class DDSupportingDefinition extends DataDictionaryComponent<Term> {

    @Override
    void fromXml(Object xml) {
        definition = xml.definition

        super.fromXml(xml)

    }

    @Override
    Term toCatalogueItem(DataDictionary dataDictionary) {

        // List<String> path = DDHelperFunctions.getPath(base_uri, "Supporting_Definitions", "\\.txaClass20")

        String termName = titleCaseName
        Term term = new Term(code: termName,
                             definition: DDHelperFunctions.tidyLabel(termName),
                             url: ddUrl,
                             description: DDHelperFunctions.parseHtml(definition),
                             sourceTermRelationships: [])

        addAllMetadata(term)

        catalogueItem = term
        return term

    }

    @Override
    List<Topic> toDitaTopics(DataDictionary dataDictionary) {


        Topic descriptionTopic = createDescriptionTopic()
        List<Topic> returnTopics = [descriptionTopic]
        Topic aliasTopic = getAliasTopic()
        if(aliasTopic) {
            returnTopics.add(aliasTopic)
        }

        Topic whereUsedTopic = generateWhereUsedTopic(dataDictionary)
        if(whereUsedTopic) {
            returnTopics.add(whereUsedTopic)
        }

        return returnTopics
    }

    String getParentTermName() {
        List<String> path = DDHelperFunctions.getPath(ddUrl, "Supporting_Definitions", "\\.txaClass20")
        return path[0]
    }


    @Override
    void fromCatalogueExport(DataDictionary dataDictionary, def catalogueXml, UUID parentId, UUID containerId) {

        definition = catalogueXml.description

        name = catalogueXml.code

        catalogueId = UUID.fromString(catalogueXml.id.toString())
        parentCatalogueId = parentId
        containerCatalogueId = containerId

        super.fromCatalogueExport(dataDictionary, catalogueXml, parentId, containerId, true)


    }

    @Override
    void fromCatalogueItem(DataDictionary dataDictionary, Term term, UUID parentId, UUID containerId, MetadataService metadataService) {

        //clazz = xml."class"

        definition = term.description
        name = term.code

        catalogueId = term.id
        this.parentCatalogueId = parentId
        this.containerCatalogueId = containerId

        super.fromCatalogueItem(dataDictionary, term, parentId, containerId, metadataService)
        catalogueItem = term

    }

/*    @Override
    void updateDescriptionInCatalogue(BindingMauroDataMapperClient mdmClient) {
        mdmClient.updateTermDescription(containerCatalogueId, catalogueId, definition.toString())
    }
*/
    @Override
    String getDitaKey() {
        return "supporting_definition_" + DDHelperFunctions.makeValidDitaName(name)
    }

    @Override
    String getOutputClassForLink() {
        String outputClass = "supportingInformation"
        if(stringValues["isRetired"] == "true") {
            outputClass += " retired"
        }
        return outputClass

    }

    @Override
    String getOutputClassForTable() {
        return "Supporting Information"
    }

    @Override
    String getTypeText() {
        return "Supporting information"
    }

    @Override
    String getTypeTitle() {
        return "Supporting Information"
    }


    @Override
    String getShortDesc(DataDictionary dataDictionary) {
        if(isPrepatory) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            try {
                GPathResult xml
                xml = Html.xmlSlurper.parseText("<xml>" + DDHelperFunctions.parseHtml(definition.toString()) + "</xml>")
                String firstParagraph = xml.p[0].text()
                if (xml.p.size() == 0) {
                    firstParagraph = xml.text()
                }
                String firstSentence = firstParagraph.substring(0, firstParagraph.indexOf(".") + 1)
                return firstSentence
            } catch (Exception e) {
                log.error("Couldn't parse: " + definition)
                return name
            }
        }
    }


    @Override
    String getInternalLink() {
        return "](te:${NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}|tm:${titleCaseName})"
    }

}
