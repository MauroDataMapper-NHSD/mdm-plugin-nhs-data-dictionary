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

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType

import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDDataSetFolder
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

class AllItemsHaveShortDescription implements IntegrityCheck {

    String name = "All items have a short description"

    String description = "Check that all items have a short description"

    @Override
    List<NhsDataDictionaryComponent> runCheck(NhsDataDictionary dataDictionary) {

        errors = dataDictionary.getAllComponents().findAll {component ->
            String shortDesc = component.otherProperties["shortDescription"]
            if(component instanceof NhsDDDataSetFolder) {
                System.err.println(((NhsDDDataSetFolder)component).folderPath)
                System.err.println("${component.name} : ${shortDesc}")
            }
            return (shortDesc == null || shortDesc == "")
        }
        return errors
    }

}
