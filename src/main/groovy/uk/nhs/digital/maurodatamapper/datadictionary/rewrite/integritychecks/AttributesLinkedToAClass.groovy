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
package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks


import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

class AttributesLinkedToAClass implements IntegrityCheck {

    String name = "Attributes linked to a class"

    String description = "Check that all live attributes are linked to a class"

    @Override
    List<NhsDataDictionaryComponent> runCheck(NhsDataDictionary dataDictionary) {

        Map<String, NhsDDAttribute> allUnusedAttributesMap = new HashMap<String, NhsDDAttribute>(dataDictionary.attributes)

        dataDictionary.classes.values().each {dataClass ->
            dataClass.allAttributes().each {attribute ->
                allUnusedAttributesMap.remove(attribute.name)
            }
        }

        allUnusedAttributesMap.removeAll {name, attribute ->
            attribute.isRetired()
        }
        return allUnusedAttributesMap.values() as List
    }

}
