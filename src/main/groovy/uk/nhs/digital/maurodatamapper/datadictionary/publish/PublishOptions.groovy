/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.nhs.digital.maurodatamapper.datadictionary.publish

import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDBusinessDefinition
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSet
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetConstraint
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetFolder
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDElement
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDSupportingInformation
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDWebPage
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent

class PublishOptions {

    // Default for all options is true
    boolean publishAttributes = true
    boolean publishElements = true
    boolean publishClasses = true
    boolean publishDataSets = true
    boolean publishBusinessDefinitions = true
    boolean publishSupportingInformation = true
    boolean publishDataSetConstraints = true
    boolean publishDataSetFolders = true

    static PublishOptions fromParameters(Map<String, String> params = [:]) {

        new PublishOptions().tap {
            publishAttributes = Boolean.parseBoolean(params["attributes"]?:'true')
            publishElements = Boolean.parseBoolean(params["elements"]?:'true')
            publishClasses = Boolean.parseBoolean(params["classes"]?:'true')
            publishDataSets = Boolean.parseBoolean(params["dataSets"]?:'true')
            publishBusinessDefinitions = Boolean.parseBoolean(params["businessDefinitions"]?:'true')
            publishSupportingInformation = Boolean.parseBoolean(params["supportingInformation"]?:'true')
            publishDataSetConstraints = Boolean.parseBoolean(params["dataSetConstraints"]?:'true')
            publishDataSetFolders = Boolean.parseBoolean(params["dataSetFolders"]?:'true')

        }
    }

    boolean isPublishableComponent(NhsDataDictionaryComponent dataDictionaryComponent) {
        if(dataDictionaryComponent instanceof NhsDDAttribute) {
            return publishAttributes
        }
        if(dataDictionaryComponent instanceof NhsDDElement) {
            return publishElements
        }
        if(dataDictionaryComponent instanceof NhsDDClass) {
            return publishClasses
        }
        if(dataDictionaryComponent instanceof NhsDDDataSet) {
            return publishDataSets
        }
        if(dataDictionaryComponent instanceof NhsDDBusinessDefinition) {
            return publishBusinessDefinitions
        }
        if(dataDictionaryComponent instanceof NhsDDSupportingInformation) {
            return publishSupportingInformation
        }
        if(dataDictionaryComponent instanceof NhsDDDataSetConstraint) {
            return publishDataSetConstraints
        }
        if(dataDictionaryComponent instanceof NhsDDDataSetFolder) {
            return publishDataSetFolders
        }
        if(dataDictionaryComponent instanceof NhsDDWebPage) {
            return true
        }
    }


}
