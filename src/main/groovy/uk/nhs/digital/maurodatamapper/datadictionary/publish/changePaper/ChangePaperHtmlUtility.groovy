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
package uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper

import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.publish.ItemLinkScanner
import uk.nhs.digital.maurodatamapper.datadictionary.publish.MauroCatalogueItemPathResolver
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishTarget

class ChangePaperHtmlUtility {
    static ChangePaperPreview generateChangePaper(
        MauroCatalogueItemPathResolver pathResolver,
        NhsDataDictionary thisDataDictionary,
        NhsDataDictionary previousDataDictionary,
        boolean includeDataSets = false) {
        PublishContext publishContext = new PublishContext(PublishTarget.CHANGE_PAPER)
        publishContext.setItemLinkScanner(
            ItemLinkScanner.createForHtmlPreview(
                thisDataDictionary.containingVersionedFolder.id,
                pathResolver))

        ChangePaper changePaper = new ChangePaper(
            thisDataDictionary,
            previousDataDictionary,
            includeDataSets)

        ChangePaperPreviewBackground background = createBackground(changePaper, publishContext)
        List<ChangePaperPreviewStereotype> stereotypes = createStereotypes(changePaper, publishContext)

        new ChangePaperPreview(background: background, stereotypes: stereotypes)
    }

    private static ChangePaperPreviewBackground createBackground(
        ChangePaper changePaper,
        PublishContext publishContext) {
        changePaper.background = publishContext.replaceLinksInString(changePaper.background)

        new ChangePaperPreviewBackground(
            reference: changePaper.reference,
            type: changePaper.type,
            versionNo: changePaper.versionNo,
            subject: changePaper.subject,
            effectiveDate: changePaper.effectiveDate,
            reasonForChange: changePaper.reasonForChange,
            publicationDate: changePaper.publicationDate,
            background: changePaper.background,
            sponsor: changePaper.sponsor,
            contactDetails: changePaper.contactDetails
        )
    }

    private static List<ChangePaperPreviewStereotype> createStereotypes(
        ChangePaper changePaper,
        PublishContext publishContext) {
        if (!changePaper.stereotypedChanges || changePaper.stereotypedChanges.empty) {
            return []
        }

        changePaper.stereotypedChanges.collect { stereotypedChange ->
            List<ChangePaperPreviewItem> changes = createChangeItems(stereotypedChange, publishContext)

            new ChangePaperPreviewStereotype(name: stereotypedChange.stereotypeName, changes: changes)
        }
    }

    private static List<ChangePaperPreviewItem> createChangeItems(
        StereotypedChange stereotypedChange,
        PublishContext publishContext) {
        stereotypedChange.changedItems.collect { changedItem ->
            String htmlOutput = changedItem.publishStructure.generateHtml(publishContext)

            new ChangePaperPreviewItem(
                name: changedItem.publishStructure.name,
                summary: changedItem.publishStructure.description,
                htmlOutput: htmlOutput)
        }
    }
}
