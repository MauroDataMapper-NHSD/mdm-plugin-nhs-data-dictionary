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

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import groovy.xml.XmlParser
import org.springframework.context.MessageSource
import org.springframework.test.annotation.Rollback
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.parser.CDSDataSetParser
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.parser.DataSetParser


@Slf4j
@Integration
@Rollback
class DataSetParserSpec extends BaseIntegrationSpec {


    static XmlParser xmlParser = new XmlParser(false, false)

    static List<String> testFiles = [
        "Cover of Vaccination Evaluated Rapidly (Cover) Data Set",
        "AIDC for Patient Identification Data Set",
        "Inter-provider Transfer Administrative Minimum Data Set",
        "CDS V6-3 Type 180 - Admitted Patient Care - Unfinished Birth Episode CDS",
        "Venous Thromboembolism Risk Assessment Data Set",
        "Cancer Outcomes and Services Data Set - Central Nervous System"
    ]

    DataModel parseDataSet(String filename) {
        log.debug('Ingesting {}', filename)

        String testFileContents = getClass().classLoader.getResourceAsStream ("testDataSets/" + filename + ".xml").text

        NhsDataDictionary newDataDictionary = new NhsDataDictionary()
        DataModel dataSetDataModel = new DataModel(
            label: filename,
            description: testFileContents,
            //createdBy: currentUser.emailAddress,
            type: DataModelType.DATA_STANDARD,
            //authority: authorityService.defaultAuthority,
            //folder: folder,
            //branchName: newDataDictionary.branchName
        )

        if (filename.startsWith("CDS")) {
            CDSDataSetParser.parseCDSDataSet(xmlParser.parseText(testFileContents), dataSetDataModel, newDataDictionary)
        } else {
            DataSetParser.parseDataSet(xmlParser.parseText(testFileContents), dataSetDataModel, newDataDictionary)
        }
        return dataSetDataModel
    }


    void ingestOtherDataSet() {

        expect:
            DataModel dm = parseDataSet(filename)
            dm.allDataElements.size() == elements

        where:
            filename << testFiles
            elements << [24,19, 36, 244, 5, 14]

    }

    @Override
    void setupDomainData() {

    }

    @Override
    MessageSource getMessageSource() {
        return null
    }
}
