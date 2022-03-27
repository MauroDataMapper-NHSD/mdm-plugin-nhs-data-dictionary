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

import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Topic

class DDDataSetConstraint extends DataDictionaryComponent<Term> {

    @Override
    void fromXml(Object xml) {
        definition = xml.definition

        super.fromXml(xml)
        titleCaseName = xml."class".TitleCaseName.text()
        stringValues["TitleCaseName"] = titleCaseName

        uin = xml."class".uin.text()
        stringValues["uin"] = uin

        name = xml."class".name.text()
        stringValues["name"] = name

        ddUrl = xml."class".DD_URL.text()
        stringValues["DD_URL"] = ddUrl

    }

    @Override
    Term toCatalogueItem(DataDictionary dataDictionary) {

        // List<String> path = DDHelperFunctions.getPath(base_uri, "Supporting_Definitions", "\\.txaClass20")

        String termName = titleCaseName
        Term term = new Term(code: termName,
                             definition: DDHelperFunctions.tidyLabel(termName),
                             url: stringValues["ultimate-path"],
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
        List<String> path = DDHelperFunctions.getPath(ddUrl, "XML_Schema_Constraints", "\\.txaClass20")
        return path[0]
    }


    @Override
    void fromCatalogueExport(DataDictionary dataDictionary, def catalogueXml, UUID parentId, UUID containerId) {

        definition = catalogueXml.description.text()

        name = catalogueXml.code

        catalogueId = UUID.fromString(catalogueXml.id.text())
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
        return "data_set_constraint_" + DDHelperFunctions.makeValidDitaName(name)
    }

    @Override
    String getOutputClassForLink() {
        String outputClass = "dataSetConstraint"
        if(stringValues["isRetired"] == "true") {
            outputClass += " retired"
        }
        return outputClass

    }

    @Override
    String getOutputClassForTable() {
        return "Data Set Constraint"
    }

    @Override
    String getTypeText() {
        return "Data Set constraint"
    }

    @Override
    String getTypeTitle() {
        return "Data Set Constraint"
    }

    @Override
    String getInternalLink() {
        return "](te:${DataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME}|tm:${titleCaseName})"
    }



}