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
package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

@Deprecated
class NhsDataDictionary {



    DataModel coreDataModel = null
    User currentUser = null

    Map<String, Terminology> attributeTerminologiesByName = [:]
    Map<String, DataElement> attributeElementsByUin = [:]
    Map<String, DataElement> attributeElementsByName = [:]

    Map<String, DataElement> elementsByUrl = [:]
    Map<String, DataElement> elementsByName = [:]

    Map<String, DataClass> classesByUin = [:]
    Map<String, DataClass> classesByName = [:]

    List<ClassLink> classLinks = []
    Map<String, ReferenceType> classReferenceTypesByName = [:]

    Map<String, DataModel> dataSetsByName = [:]

    Map<String, Term> businessDefinitionsByName = [:]
    Map<String, Term> supportingInformationByName = [:]
    Map<String, Term> xmlSchemaConstraintsByName = [:]


    static XmlParser xmlParser = XmlParser.newInstance()


}
