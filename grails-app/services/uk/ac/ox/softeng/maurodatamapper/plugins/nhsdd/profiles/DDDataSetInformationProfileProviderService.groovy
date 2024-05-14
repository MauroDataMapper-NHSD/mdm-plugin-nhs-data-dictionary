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
package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles

import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService

import groovy.util.logging.Slf4j

@Slf4j
class DDDataSetInformationProfileProviderService extends JsonProfileProviderService {
    @Override
    String getDisplayName() {
        log.info('Getting display name')
        'NHS Data Dictionary - DataSet information'
    }

    @Override
    String getMetadataNamespace() {
        log.info('getting namespace')
        'uk.nhs.datadictionary.dataset'
    }

    @Override
    String getJsonResourceFile() {
        log.info('getting resource file')
        return 'datasetInformationProfile.json'
    }

    @Override
    String getVersion() {
        log.info('Getting version')
        '1.0.0'
    }

    @Override
    List<String> profileApplicableForDomains() {
        log.info('returning applicable types')
        return ['DataClass', 'DataElement']
    }
}
