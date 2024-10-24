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

import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent
import java.time.LocalDate

class AllItemsAreWithinValidDateRange implements IntegrityCheck {

    String name = "All items are within their validity dates"

    String description = "Check that items are within their To and From dates "

    @Override
    List<IntegrityCheckError> runCheck(NhsDataDictionary dataDictionary) {
        errors = dataDictionary.getAllComponents()
            .findAll {component -> !component.isRetired() && validateDateRange(component, LocalDate.now()) }
            .collect { component -> new IntegrityCheckError(component) }

        return errors
    }

    //there's a test set for this that runs through the various scenarios, see: AllItemsAreWithinValidDateRangeSpec
    static Boolean validateDateRange(NhsDataDictionaryComponent component, LocalDate dateNow){

        if (component.fromDate && component.toDate) {
            {
                if (component.toDate.isBefore(component.fromDate)) {
                    return true
                }

                if (component.toDate.isBefore(dateNow)) {
                    return true
                }
            }
        }
            if (component.toDate && !component.fromDate){
                return true
            }

       return false
    }

}
