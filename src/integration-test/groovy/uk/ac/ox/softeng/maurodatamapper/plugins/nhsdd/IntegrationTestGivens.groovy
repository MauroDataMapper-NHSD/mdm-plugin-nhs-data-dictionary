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

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.test.unit.security.TestUser
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

    User "there is a user"(String emailAddress = FUNCTIONAL_TEST, String firstName = 'Test', String lastName = 'User') {
        User user = new TestUser(emailAddress: emailAddress, firstName: firstName, lastName: lastName, id: UUID.randomUUID())
        user
    }

    Folder "there is a folder"(String label, String description = "", Folder parentFolder = null) {
        Folder folder = Folder.findByLabel(label)
        if (folder) {
            return folder
        }

        log.debug("Creating folder '$label'")
        folder = new Folder(
            label: label,
            createdBy: FUNCTIONAL_TEST,
            description: description,
            parentFolder: parentFolder)

        if (!folder.metadata) {
            folder.metadata = new LinkedHashSet<>()
        }

        checkAndSave(folder)

        folder
    }

    VersionedFolder "there is a versioned folder"(String label) {
        VersionedFolder versionedFolder = VersionedFolder.findByLabel(label)
        if (versionedFolder) {
            return versionedFolder
        }

        log.debug("Creating versioned folder '$label'")
        Authority authority = Authority.findByDefaultAuthority(true)
        versionedFolder = new VersionedFolder(label: label, createdBy: FUNCTIONAL_TEST, authority: authority)

        checkAndSave(versionedFolder)

        versionedFolder
    }

    DataModel "there is a data model"(String label, Folder folder, String description = "") {
        DataModel dataModel = DataModel.findByLabel(label)
        if (dataModel) {
            return dataModel
        }

        log.debug("Creating datamodel '$label'")
        Authority authority = Authority.findByDefaultAuthority(true)

        dataModel = new DataModel(
            createdBy: FUNCTIONAL_TEST,
            label: label,
            description: description,
            folder: folder,
            authority: authority,
            aliasesString: label,
            modelType: DataModelType.DATA_ASSET)

        if (!dataModel.metadata) {
            dataModel.metadata = new LinkedHashSet<>()
        }

        checkAndSave(dataModel)

        dataModel
    }

    DataClass "there is a data class"(String label, DataModel dataModel, DataClass parentDataClass = null, String description = "") {
        DataClass dataClass = DataClass.findByDataModelAndParentDataClassAndLabel(dataModel, parentDataClass, label)
        if (dataClass) {
            return dataClass
        }

        log.debug("Creating dataclass '$label'")
        dataClass = new DataClass(
            createdBy: FUNCTIONAL_TEST,
            label: label,
            description: description,
            dataModel: dataModel,
            parentDataClass: parentDataClass)

        if (!dataModel.dataClasses) {
            dataModel.dataClasses = new LinkedHashSet<>()
        }

        dataModel.dataClasses.add(dataClass)

        if (!dataClass.metadata) {
            dataClass.metadata = new LinkedHashSet<>()
        }

        if (parentDataClass) {
            if (!parentDataClass.dataClasses) {
                parentDataClass.dataClasses = new LinkedHashSet<>()
            }

            parentDataClass.dataClasses.add(dataClass)
        }

        checkAndSave(dataClass)

        dataClass
    }

    DataElement "there is a data element"(String label, DataModel dataModel, DataClass dataClass, DataType dataType, String description = "") {
        DataElement dataElement = DataElement.findByDataClassAndLabel(dataClass, label)
        if (dataElement) {
            return dataElement
        }

        log.debug("Creating data element '$label'")
        dataElement = new DataElement(
            createdBy: FUNCTIONAL_TEST,
            label: label,
            description: description,
            dataModel: dataModel,
            dataClass: dataClass,
            dataType: dataType)

        if (!dataClass.dataElements) {
            dataClass.dataElements = new LinkedHashSet<>()
        }

        dataClass.dataElements.add(dataElement)

        if (!dataElement.metadata) {
            dataElement.metadata = new LinkedHashSet<>()
        }

        checkAndSave(dataElement)

        dataElement
    }

    DataType "there is a primitive data type"(String label, DataModel dataModel) {
        PrimitiveType dataType = PrimitiveType.findByDataModelAndLabel(dataModel, label)
        if (dataType) {
            return dataType
        }

        dataType = new PrimitiveType(createdBy: FUNCTIONAL_TEST, label: label, dataModel: dataModel)

        dataModel.addToDataTypes(dataType)

        checkAndSave(dataType)

        dataType
    }

    Terminology "there is a terminology"(String label, Folder folder, String description = "") {
        Terminology terminology = Terminology.findByLabel(label)
        if (terminology) {
            return terminology
        }

        log.debug("Creating terminology '$label'")
        Authority authority = Authority.findByDefaultAuthority(true)

        terminology = new Terminology(
            createdBy: FUNCTIONAL_TEST,
            label: label,
            description: description,
            folder: folder,
            authority: authority,
            aliasesString: label)

        if (!terminology.metadata) {
            terminology.metadata = new LinkedHashSet<>()
        }

        checkAndSave(terminology)

        terminology
    }

    Term "there is a term"(String code, String definition, Terminology terminology, String description = "") {
        Term term = Term.findByCodeAndDefinition(code, definition)
        if (term) {
            return term
        }

        log.debug("Creating term ['$code'] '$definition'")
        term = new Term(
            createdBy: FUNCTIONAL_TEST,
            code: code,
            definition: definition,
            description: description,
            terminology: terminology)

        if (!terminology.terms) {
            terminology.terms = new LinkedHashSet<>()
        }

        terminology.terms.add(term)

        if (!term.metadata) {
            term.metadata = new LinkedHashSet<>()
        }

        checkAndSave(term)

        term
    }

    void checkAndSave(GormEntity domainObj) {
        try {
            GormUtils.checkAndSave(messageSource, domainObj)
        } catch (ValidationException ex) {
            Assert.fail(ex.message)
        }
    }
}
