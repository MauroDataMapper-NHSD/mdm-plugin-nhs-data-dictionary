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

import spock.lang.Specification
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute

class AllItemsAreWithinValidDateRangeSpec extends Specification {

    def "validateDateRange returns false for valid dates"() {
        given:
        def checker = new AllItemsAreWithinValidDateRange()
        def component = new NhsDDAttribute()

        component.otherProperties = ["validFrom": "3030-12-31", "validTo": "3040-01-01"]

        expect:
        checker.validateDateRange(component) == false
    }

    def "validateDateRange returns true for todate set before fromdate "() {
        given:
        def checker = new AllItemsAreWithinValidDateRange()
        def component = new NhsDDAttribute()
        component.otherProperties = ["validFrom": "2030-12-31", "validTo": "2030-12-01"]

        expect:
        checker.validateDateRange(component) == true
    }

    def "validateDateRange returns true when todate is set and fromdate is not"() {
        given:
        def checker = new AllItemsAreWithinValidDateRange()
        def component = new NhsDDAttribute()
        component.otherProperties = ["validTo": "2030-12-01"]

        expect:
        checker.validateDateRange(component) == true
    }

    def "validateDateRange returns true when todate has expired"() {
        given:
        def checker = new AllItemsAreWithinValidDateRange()
        def component = new NhsDDAttribute()
        component.otherProperties = ["validFrom": "2020-12-31", "validTo": "2020-01-01"]

        expect:
        checker.validateDateRange(component) == true
    }
}