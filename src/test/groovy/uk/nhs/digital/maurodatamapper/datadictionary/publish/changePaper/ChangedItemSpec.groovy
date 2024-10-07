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

import spock.lang.Specification

class ChangedItemSpec extends Specification {
    void "should return empty string for summary of changes with no changes"() {
        given: "there are no changes in a changed item"
        ChangedItem changedItem = new ChangedItem()

        when: "getting the summary of changes"
        String summaryOfChanges = changedItem.getSummaryOfChanges()

        then: "the string is empty"
        verifyAll {
            summaryOfChanges == ""
        }
    }

    void "should return 'New' for summary of changes when at least one change is new"() {
        given: "there are changes in a changed item"
        ChangedItem changedItem = new ChangedItem(changes: [
            new Change(changeType: Change.ALIASES_TYPE),
            new Change(changeType: Change.CHANGED_ATTRIBUTES_TYPE),
            new Change(changeType: Change.NATIONAL_CODES_TYPE),
            new Change(changeType: Change.NEW_TYPE),
        ])

        when: "getting the summary of changes"
        String summaryOfChanges = changedItem.getSummaryOfChanges()

        then: "the string only contains the new change"
        verifyAll {
            summaryOfChanges == Change.NEW_TYPE
        }
    }

    void "should return 'Retired' for summary of changes when at least one change is retired"() {
        given: "there are changes in a changed item"
        ChangedItem changedItem = new ChangedItem(changes: [
            new Change(changeType: Change.ALIASES_TYPE),
            new Change(changeType: Change.CHANGED_ATTRIBUTES_TYPE),
            new Change(changeType: Change.NATIONAL_CODES_TYPE),
            new Change(changeType: Change.RETIRED_TYPE),
        ])

        when: "getting the summary of changes"
        String summaryOfChanges = changedItem.getSummaryOfChanges()

        then: "the string only contains the retired change"
        verifyAll {
            summaryOfChanges == Change.RETIRED_TYPE
        }
    }

    void "should return all change types for summary of changes when not new or retired"() {
        given: "there are changes in a changed item"
        ChangedItem changedItem = new ChangedItem(changes: [
            new Change(changeType: Change.UPDATED_DESCRIPTION_TYPE),
            new Change(changeType: Change.ALIASES_TYPE),
            new Change(changeType: Change.CHANGED_ATTRIBUTES_TYPE),
            new Change(changeType: Change.NATIONAL_CODES_TYPE)
        ])

        when: "getting the summary of changes"
        String summaryOfChanges = changedItem.getSummaryOfChanges()

        then: "the string only contains the retired change"
        verifyAll {
            summaryOfChanges == "${Change.UPDATED_DESCRIPTION_TYPE}, ${Change.ALIASES_TYPE}, ${Change.CHANGED_ATTRIBUTES_TYPE}, ${Change.NATIONAL_CODES_TYPE}"
        }
    }
}
