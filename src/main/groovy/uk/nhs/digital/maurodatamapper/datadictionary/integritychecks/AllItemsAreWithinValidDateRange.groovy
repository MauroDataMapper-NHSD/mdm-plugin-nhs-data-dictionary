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
package uk.nhs.digital.maurodatamapper.datadictionary.integritychecks

import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetFolder
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent

class AllItemsAreWithinValidDateRange implements IntegrityCheck {

    String name = "All items are within their validity dates"

    String description = "Check that items are within their To and From dates "

    @Override
    List<NhsDataDictionaryComponent> runCheck(NhsDataDictionary dataDictionary) {

        // get current date
        // get all items
        // filter items that are within the date range
        // return the rest
        //does a companant have access to profile stuff? where is that?
        errors = dataDictionary.getAllComponents().findAll {component ->
           return !component.isRetired() &&
             !validateDateRange(component)
        }

        return errors
    }

    /*#
    items with dates:
    Attributes
    Business Definition
    Classes
    Code Sets
    Data Sets and Data Elements
    Supporting Definition
    Terminologies
    Terms
    Web Page
    Work Item
    */

    private Boolean validateDateRange(NhsDataDictionaryComponent component) {
        // date rules
        // 1. if from date is set, to date must be set else false
        // 2. if to date is set, from date must be set else false
        // 3. if from date is set, it must be before to date else false

        if (component.catalogueItemIdAsString == "9df3500b-b435-4ac8-bdb6-68a4effd5faa") {
        String fromDate = component.getFromDate()
        String toDate = component.getToDate()
        return false

            //so its getting dates correctly for stuff from the component catalog, do I need to handel the other stuff?
        }
       return true

    }

}
