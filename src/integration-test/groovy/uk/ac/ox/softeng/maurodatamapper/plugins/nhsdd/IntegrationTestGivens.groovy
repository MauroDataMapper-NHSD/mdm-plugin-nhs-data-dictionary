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

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.validation.ValidationException
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.spockframework.util.Assert
import org.springframework.context.MessageSource

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getFUNCTIONAL_TEST

@Slf4j
class IntegrationTestGivens {
    private MessageSource messageSource

    IntegrationTestGivens(MessageSource messageSource) {
        this.messageSource = messageSource
    }

    Folder "there is a folder"(String label) {
        Folder folder = Folder.findByLabel(label)
        if (folder) {
            return folder
        }

        log.debug("Creating folder '$label'")
        folder = new Folder(label: label, createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)

        folder
    }

    void checkAndSave(GormEntity domainObj) {
        try {
            GormUtils.checkAndSave(messageSource, domainObj)
        } catch (ValidationException ex) {
            Assert.fail(ex.message)
        }
    }
}
