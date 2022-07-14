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
package uk.ac.ox.softeng.maurodatamapper.plugins

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.NhsDataDictionaryService
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary

import static uk.ac.ox.softeng.maurodatamapper.core.BootStrap.BootStrapUser

class BootStrap {

    @Autowired
    MessageSource messageSource

    def init = {servletContext ->
        ApiProperty.withNewTransaction {

            List<ApiProperty> existingProperties = ApiProperty.findAllByCategory(NhsDataDictionaryService.NHSDD_PROPERTY_CATEGORY)

            NhsDataDictionaryService.KNOWN_KEYS.each {k, t ->
                if (!existingProperties.find {it.key == k}) {
                    ApiProperty apiProperty = new ApiProperty(key: k, value: t,
                                                              createdBy: BootStrapUser.instance.emailAddress,
                                                              category: NhsDataDictionaryService.NHSDD_PROPERTY_CATEGORY)
                    GormUtils.checkAndSave(messageSource, apiProperty)
                }
            }

            ApiProperty defaultProfile = ApiProperty.findByKey('ui.default.profile.namespace')
            if(!defaultProfile) {
                defaultProfile = new ApiProperty(key: 'ui.default.profile.namespace', value: NhsDataDictionary.DEFAULT_PROFILE_NAMESPACE,
                                                 createdBy: BootStrapUser.instance.emailAddress,
                                                 category: 'UI',
                                                 publiclyVisible: true)
                GormUtils.checkAndSave(messageSource, defaultProfile)
            }
        }

        environments {
            test {
            }
        }
    }

    def destroy = {
    }

}

