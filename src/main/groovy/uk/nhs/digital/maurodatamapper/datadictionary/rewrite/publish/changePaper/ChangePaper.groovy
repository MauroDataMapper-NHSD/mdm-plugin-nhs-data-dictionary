/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish.changePaper

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

class ChangePaper {

    List<StereotypedChange> stereotypedChanges = []

    String reference
    String type
    String versionNo
    String subject
    String effectiveDate
    String reasonForChange
    String publicationDate
    String background
    String sponsor

    ChangePaper(NhsDataDictionary nhsDataDictionary, NhsDataDictionary oldDataDictionary) {
        reference = nhsDataDictionary.workItemDetails['reference'] ?: 'CRXXXX'
        type = nhsDataDictionary.workItemDetails['type'] ?: 'Change Request'
        versionNo = nhsDataDictionary.workItemDetails['versionNo'] ?: '1.0'
        subject = nhsDataDictionary.workItemDetails['subject'] ?: 'NHS England and NHS Improvement'
        effectiveDate = nhsDataDictionary.workItemDetails['effectiveDate'] ?: 'Immediate'
        reasonForChange = nhsDataDictionary.workItemDetails['reasonForChange'] ?: 'Change to definitions'
        publicationDate = new Date().toString()
        background = nhsDataDictionary.workItemDetails['backgroundText'] ?: backgroundText()
        sponsor = nhsDataDictionary.workItemDetails['sponsor'] ?: 'NHS Digital'

        stereotypedChanges = calculateChanges(nhsDataDictionary, oldDataDictionary)
    }

    Map<String, String> propertiesAsMap() {

        Map<String, String> response = [:]

        response['Reference'] = reference
        response['Type'] = type
        response['Version no.'] = versionNo
        response['Subject'] = subject
        response['Effective date'] = effectiveDate
        response['Reason for change'] = reasonForChange
        response['Publication Date'] = publicationDate
        response['Background'] = background
        response['Sponsor'] = sponsor

        return response
    }

    private List<StereotypedChange> calculateChanges(NhsDataDictionary thisDataDictionary, NhsDataDictionary previousDataDictionary) {
        List<StereotypedChange> changedItems = []

        changedItems += new StereotypedChange(stereotypeName: "NHS Business Definition",
              changedItems: compareMaps(thisDataDictionary.businessDefinitions, previousDataDictionary.businessDefinitions))
        changedItems += new StereotypedChange(stereotypeName: "Supporting Information",
              changedItems: compareMaps(thisDataDictionary.supportingInformation, previousDataDictionary.supportingInformation))
        changedItems += new StereotypedChange(stereotypeName: "Attribute",
              changedItems: compareMaps(thisDataDictionary.attributes, previousDataDictionary.attributes))
        changedItems += new StereotypedChange(stereotypeName: "Data Element",
              changedItems: compareMaps(thisDataDictionary.elements, previousDataDictionary.elements))
        changedItems += new StereotypedChange(stereotypeName: "Class",
              changedItems: compareMaps(thisDataDictionary.classes, previousDataDictionary.classes))
        changedItems += new StereotypedChange(stereotypeName: "Data Set",
              changedItems: compareMaps(thisDataDictionary.dataSets, previousDataDictionary.dataSets))
        changedItems += new StereotypedChange(stereotypeName: "Data Set Constraint",
              changedItems: compareMaps(thisDataDictionary.dataSetConstraints, previousDataDictionary.dataSetConstraints))

        return changedItems
    }

    private static List<ChangedItem> compareMaps(
        Map<String, NhsDataDictionaryComponent> newList,
        Map<String, NhsDataDictionaryComponent> oldList) {
        List<ChangedItem> changedItems = []
        newList.sort{ it.key }.each {name, component ->
            NhsDataDictionaryComponent previousComponent = oldList[name]
            List<Change> changes = component.getChanges(previousComponent)
            if (changes) {
                changedItems.add(new ChangedItem(
                    dictionaryComponent: component,
                    changes: changes
                ))
            }
        }
        return changedItems
    }

    static String backgroundText() {
        StringWriter writer = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(writer)
        markupBuilder.div {
            p "This Change Request adds the ???? Data Set and supporting definitions to the NHS Data Model and Dictionary to support the Information Standard."
            p {
                mkp.yield "A short demonstration is available which describes \"How to Read an NHS Data Model and Dictionary Change Request\", " +
                    "in an easy to understand screen capture including a voice over and readable captions. This demonstration can be viewed at: "

                a (href: "https://datadictionary.nhs.uk/elearning/change_request/index.html",
                   "https://datadictionary.nhs.uk/elearning/change_request/index.html.")
                mkp.yield "."
            }

            p {
                mkp.yield "Note: if the web page does not open, please copy the link and paste into the web browser. A guide to how to use the " +
                  "demonstration can be found at: "
                a(href: "https://datadictionary.nhs.uk/help/demonstrations.html", "Demonstrations")
                mkp.yield "."
            }
        }
        return writer.toString()
    }
/*
    Map<String, List<Change>> getStereotypedChanges() {

        Map<String, List<Change>> stereotypedChangesResult = [:]

        changes.each { change ->
            List<Change> changesForStereotype = stereotypedChangesResult[change.stereotype]
            if(!changesForStereotype) {
                changesForStereotype = [change]
                stereotypedChangesResult[change.stereotype] = changesForStereotype

            } else {
                changesForStereotype.add(change)
            }
        }
        return stereotypedChangesResult
    }
*/
}
