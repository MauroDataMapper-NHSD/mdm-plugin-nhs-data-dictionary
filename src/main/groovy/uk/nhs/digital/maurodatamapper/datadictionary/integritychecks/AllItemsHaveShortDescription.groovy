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
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetFolder
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent

@Slf4j
class AllItemsHaveShortDescription implements IntegrityCheck {

    String name = "All items have a short description"

    String description = "Check that all items have a short description"

    @Override
    List<IntegrityCheckError> runCheck(NhsDataDictionary dataDictionary) {

        errors = dataDictionary.getAllComponents()
            .findAll {component ->
                String shortDesc = component.getShortDescription()
                if(component instanceof NhsDDDataSetFolder) {
                    log.debug(((NhsDDDataSetFolder)component).folderPath.toString())
                    log.debug("${component.name} : ${shortDesc}")
                }
                return (shortDesc == null || shortDesc == "")
            }
            .collect { component -> new IntegrityCheckError(component) }

        return errors
    }

}
