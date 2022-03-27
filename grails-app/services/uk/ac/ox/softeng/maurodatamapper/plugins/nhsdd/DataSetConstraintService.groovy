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
package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDDataSetConstraint
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary

@Slf4j
@Transactional
class DataSetConstraintService extends DataDictionaryComponentService<Term, NhsDDDataSetConstraint> {

    @Override
    NhsDDDataSetConstraint show(UUID versionedFolderId, String id) {
        Term dataSetConstraintTerm = termService.get(id)
        NhsDDDataSetConstraint dataSetConstraint = getNhsDataDictionaryComponentFromCatalogueItem(dataSetConstraintTerm, null)
        dataSetConstraint.definition = convertLinksInDescription(versionedFolderId, dataSetConstraint.getDescription())
        return dataSetConstraint
    }

    @Override
    Set<Term> getAll(UUID versionedFolderId, boolean includeRetired = false) {

        Terminology dataSetConstraintTerminology = nhsDataDictionaryService.getDataSetConstraintTerminology(versionedFolderId)

        List<Term> terms = termService.findAllByTerminologyId(dataSetConstraintTerminology.id)

        terms.findAll {term ->
            includeRetired || !catalogueItemIsRetired(term)
        }

    }

    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".Data set constraint"
    }

    @Override
    NhsDDDataSetConstraint getNhsDataDictionaryComponentFromCatalogueItem(Term catalogueItem, NhsDataDictionary dataDictionary) {
        NhsDDDataSetConstraint dataSetConstraint = new NhsDDDataSetConstraint()
        nhsDataDictionaryComponentFromItem(catalogueItem, dataSetConstraint)
        dataSetConstraint.dataDictionary = dataDictionary
        return dataSetConstraint
    }

    void persistDataSetConstraints(NhsDataDictionary dataDictionary,
                                     VersionedFolder dictionaryFolder, String currentUserEmailAddress) {

        Terminology terminology = new Terminology(
            label: NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME,
            folder: dictionaryFolder,
            createdBy: currentUserEmailAddress,
            authority: authorityService.defaultAuthority,
            branchName: dataDictionary.branchName)

        TreeMap<String, Term> allTerms = new TreeMap<>()
        dataDictionary.dataSetConstraints.each {name, dataSetConstraint ->

            // Either this is the first time we've seen a term...
            // .. or if we've already got one, we'll overwrite it with this one
            // (if this one isn't retired)
            if(!allTerms[name] || !dataSetConstraint.isRetired()) {

                Term term = new Term(
                    code: name,
                    label: name,
                    definition: name,
                    // Leave Url blank for now
                    // url: businessDefinition.otherProperties["ddUrl"].replaceAll(" ", "%20"),
                    description: dataSetConstraint.definition,
                    createdBy: currentUserEmailAddress,
                    depth: 1,
                    terminology: terminology)

                addMetadataFromComponent(term, dataSetConstraint, currentUserEmailAddress)

                allTerms[name] = term
            }
        }
        allTerms.values().each { term ->
            terminology.addToTerms(term)
        }

        if (terminologyService.validate(terminology)) {
            terminology = terminologyService.saveModelWithContent(terminology)
        } else {
            GormUtils.outputDomainErrors(messageSource, terminology) // TODO throw exception???
        }

    }

    NhsDDDataSetConstraint getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        nhsDataDictionary.dataSetConstraints.values().find {
            it.catalogueItem.id == catalogueItemId
        }
    }


}