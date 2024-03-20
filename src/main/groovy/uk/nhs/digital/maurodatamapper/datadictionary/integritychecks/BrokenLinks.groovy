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
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent

import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
class BrokenLinks implements IntegrityCheck {

    String name = "Broken Links in description"

    String description = "Check that all external links in descriptions lead to a valid web page"

    static Pattern pattern = Pattern.compile("<a[\\s]*href=\"(http[^\"]*)\"[^>]*>([^<]*)</a>")

    @Override
    List<NhsDataDictionaryComponent> runCheck(NhsDataDictionary dataDictionary) {
        Map<String, List<NhsDataDictionaryComponent>> linkComponentMap = [:]
        List<NhsDataDictionaryComponent> errorComponents = []
        dataDictionary.allComponents.
            findAll {!it.isRetired() && it.definition }.
            each {component ->
                Matcher matcher = pattern.matcher(component.definition)
                while(matcher.find()) {
                    List<NhsDataDictionaryComponent> components = linkComponentMap[matcher.group(1)]
                    if(components) {
                        components.add(component)
                    } else {
                        linkComponentMap[matcher.group(1)] = [component]
                    }
                }
            }
        linkComponentMap.each {link, componentList ->
            try {
                def code = new URL(link).openConnection().with {
                    requestMethod = 'HEAD'
                    connect()
                    responseCode
                }

                if(code != 200) {
                    componentList.each {component ->
                        log.error("${component.stereotype},${component.name},${link}, ${code}")
                    }
                }
                if(code == 404) {
                    errorComponents.addAll(componentList)
                    log.info("Broken link: " + link)
                }
            } catch(Exception e) {
                componentList.each {component ->
                    log.error("${component.stereotype},${component.name},${link}, Exception")
                }
                errorComponents.addAll(componentList)
            }
        }
        errors = (errorComponents.toSet()).toList()
    }

}
