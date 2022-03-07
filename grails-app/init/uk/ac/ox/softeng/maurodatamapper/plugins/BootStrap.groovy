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
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.NhsDataDictionaryService

import grails.core.GrailsApplication
import org.springframework.context.MessageSource

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.ADMIN
import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getFUNCTIONAL_TEST

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


class BootStrap {

    def init = {servletContext ->
        ApiProperty.withNewTransaction {

            List<ApiProperty> existingProperties = ApiProperty.findAllByCategory(NhsDataDictionaryService.NHSDD_PROPERTY_CATEGORY)
            List<ApiProperty> newProperties = []


            NhsDataDictionaryService.KNOWN_KEYS.each {k, t ->
                if (!existingProperties.find {it.key == k}) {
                    newProperties.add(new ApiProperty(key: k, value: t,
                                               createdBy: ADMIN,
                                               category: NhsDataDictionaryService.NHSDD_PROPERTY_CATEGORY))
                }
            }
            ApiProperty.saveAll(newProperties)
        }

        environments {
            test {
            }
        }
    }

    def destroy = {
    }

}

