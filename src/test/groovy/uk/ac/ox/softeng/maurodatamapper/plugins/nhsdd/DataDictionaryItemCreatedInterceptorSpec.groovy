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
package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.interceptors.DataDictionaryItemCreatedInterceptor

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class DataDictionaryItemCreatedInterceptorSpec extends Specification implements InterceptorUnitTest<DataDictionaryItemCreatedInterceptor> {
    void "should match against '#controller' and the '#action' action"(String controller, String action) {
        when:
        withRequest(controller: controller, action: action)

        then:
        interceptor.doesMatch()

        where:
        controller      | action
        "folder"        | "save"
        "dataModel"     | "save"
        "dataClass"     | "save"
        "dataElement"   | "save"
        "term"          | "save"
    }

    void "should not match against '#controller' and the '#action' action"(String controller, String action) {
        when:
        withRequest(controller: controller, action: action)

        then:
        !interceptor.doesMatch()

        where:
        controller      | action
        "folder"        | "update"
        "dataModel"     | "update"
        "dataClass"     | "update"
        "dataElement"   | "update"
        "term"          | "update"
        "folder"        | "delete"
        "dataModel"     | "delete"
        "dataClass"     | "delete"
        "dataElement"   | "delete"
        "term"          | "delete"
        "folder"        | "show"
        "dataModel"     | "show"
        "dataClass"     | "show"
        "dataElement"   | "show"
        "term"          | "show"
    }
}
