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
package nhs.digital.maurodatamapper.datadictionary

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDBusinessDefinition
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSet
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetConstraint
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetFolder
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDElement
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDSupportingInformation
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary

@Integration
@Rollback
class NhsDataDictionarySpec extends Specification {
    void "should process links from xml for classes and attributes"() {
        given: "the dictionary contains components with definitions containing links"
        NhsDataDictionary dataDictionary = new NhsDataDictionary()

        NhsDDClass eventDateTimeClass = new NhsDDClass()
        eventDateTimeClass.name = "EVENT DATE TIME"
        eventDateTimeClass.otherProperties["ddUrl"] = "https://datadictionary.nhs.uk/classes/event_date_time.html"
        eventDateTimeClass.definition = "Defines individual <a href=\"https://datadictionary.nhs.uk/attributes/event_date.html\">EVENT DATES</a> and <a href=\"https://datadictionary.nhs.uk/attributes/event_time.html\">EVENT TIMES</a>."
        dataDictionary.classes[eventDateTimeClass.name] = eventDateTimeClass

        NhsDDAttribute eventDateAttribute = new NhsDDAttribute()
        eventDateAttribute.name = "EVENT DATE"
        eventDateAttribute.otherProperties["ddUrl"] = "https://datadictionary.nhs.uk/attributes/event_date.html"
        eventDateAttribute.definition = "<p>The date, month, year and century, or any combination of these elements, of an <a href=\"https://datadictionary.nhs.uk/classes/event_date_time.html\">EVENT DATE TIME</a>.</p>"
        eventDateAttribute.parentClass = eventDateTimeClass // Required to produce the correct path
        dataDictionary.attributes[eventDateAttribute.name] = eventDateAttribute

        NhsDDAttribute eventTimeAttribute = new NhsDDAttribute()
        eventTimeAttribute.name = "EVENT TIME"
        eventTimeAttribute.otherProperties["ddUrl"] = "https://datadictionary.nhs.uk/attributes/event_time.html"
        eventTimeAttribute.definition = "<p>The time (using a 24 hour clock) at which an <a href=\"https://datadictionary.nhs.uk/classes/event_date_time.html\">EVENT DATE TIME</a>, or the action in an <a href=\"https://datadictionary.nhs.uk/classes/event_date_time.html\">EVENT DATE TIME</a>, takes place.</p><p>This may include representation of a time zone.</p>"
        eventTimeAttribute.parentClass = eventDateTimeClass // Required to produce the correct path
        dataDictionary.attributes[eventTimeAttribute.name] = eventTimeAttribute

        when: "links in definitions are replaced"
        dataDictionary.processLinksFromXml()

        then: "the modified definitions are correct"
        String expectedEventDateDefinition = "<p>The date, month, year and century, or any combination of these elements, of an <a href=\"#/catalogue/item/dataModels/dm%3AClasses%20and%20Attributes%7Cdc%3AEVENT%20DATE%20TIME\">EVENT DATE TIME</a>.</p>"
        String expectedEventTimeDefinition = "<p>The time (using a 24 hour clock) at which an <a href=\"#/catalogue/item/dataModels/dm%3AClasses%20and%20Attributes%7Cdc%3AEVENT%20DATE%20TIME\">EVENT DATE TIME</a>, or the action in an <a href=\"#/catalogue/item/dataModels/dm%3AClasses%20and%20Attributes%7Cdc%3AEVENT%20DATE%20TIME\">EVENT DATE TIME</a>, takes place.</p><p>This may include representation of a time zone.</p>"
        String expectedEventDateTimeDefinition = "Defines individual <a href=\"#/catalogue/item/dataModels/dm%3AClasses%20and%20Attributes%7Cdc%3AEVENT%20DATE%20TIME%7Cde%3AEVENT%20DATE\">EVENT DATES</a> and <a href=\"#/catalogue/item/dataModels/dm%3AClasses%20and%20Attributes%7Cdc%3AEVENT%20DATE%20TIME%7Cde%3AEVENT%20TIME\">EVENT TIMES</a>."

        verifyAll {
            eventDateAttribute.definition == expectedEventDateDefinition
            eventTimeAttribute.definition == expectedEventTimeDefinition
            eventDateTimeClass.definition == expectedEventDateTimeDefinition
        }
    }

    void "should process links from xml for terminology definitions"() {
        given: "the dictionary contains components with definitions containing links"
        NhsDataDictionary dataDictionary = new NhsDataDictionary()

        NhsDDBusinessDefinition businessDefinition = new NhsDDBusinessDefinition()
        businessDefinition.name = "Abbreviated Mental Test Score"
        businessDefinition.otherProperties["ddUrl"] = "https://datadictionary.nhs.uk/nhs_business_definitions/abbreviated_mental_test_score.html"
        businessDefinition.definition = "<p>The <a href=\"https://datadictionary.nhs.uk/nhs_business_definitions/abbreviated_mental_test_score.html\">Abbreviated_Mental_Test_Score</a> is an <a href=\"https://datadictionary.nhs.uk/classes/assessment_tool.html\">ASSESSMENT_TOOL</a>.</p>"
        dataDictionary.businessDefinitions[businessDefinition.name] = businessDefinition

        NhsDDSupportingInformation supportingInformation = new NhsDDSupportingInformation()
        supportingInformation.name = "Accessible Information"
        supportingInformation.otherProperties["ddUrl"] = "https://datadictionary.nhs.uk/supporting_information/accessible_information.html"
        supportingInformation.definition = "<a href=\"https://datadictionary.nhs.uk/supporting_information/accessible_information.html\">Accessible Information</a> is information which is able to be read or received and understood by the individual or group for which it is intended.</p>"
        dataDictionary.supportingInformation[supportingInformation.name] = supportingInformation

        NhsDDDataSetConstraint dataSetConstraint = new NhsDDDataSetConstraint()
        dataSetConstraint.name = "Community Services Data Set Constraints"
        dataSetConstraint.otherProperties["ddUrl"] = "https://datadictionary.nhs.uk/data_sets/message_documentation/data_set_constraints/community_services_data_set_constraints.html"
        dataSetConstraint.definition = "<p>The <a href=\"https://datadictionary.nhs.uk/data_sets/message_documentation/data_set_constraints/community_services_data_set_constraints.html\">constraints</a> applied to the <a href=\"https://datadictionary.nhs.uk/data_sets/clinical_data_sets/community_services_data_set.html\">Community Services Data Set</a>.</p>"
        dataDictionary.dataSetConstraints[dataSetConstraint.name] = dataSetConstraint

        when: "links in definitions are replaced"
        dataDictionary.processLinksFromXml()

        then: "the modified definitions are correct"
        String expectedBusinessDefinition = "<p>The <a href=\"#/catalogue/item/terminologies/te%3ANHS%20Business%20Definitions%7Ctm%3AAbbreviated%20Mental%20Test%20Score\">Abbreviated Mental Test Score</a> is an <a href=\"https://datadictionary.nhs.uk/classes/assessment_tool.html\">ASSESSMENT_TOOL</a>.</p>"
        String expectedSupportingInformation = "<a href=\"#/catalogue/item/terminologies/te%3ASupporting%20Information%7Ctm%3AAccessible%20Information\">Accessible Information</a> is information which is able to be read or received and understood by the individual or group for which it is intended.</p>"
        String expectedDataSetConstraint = "<p>The <a href=\"#/catalogue/item/terminologies/te%3AData%20Set%20Constraints%7Ctm%3ACommunity%20Services%20Data%20Set%20Constraints\">constraints</a> applied to the <a href=\"https://datadictionary.nhs.uk/data_sets/clinical_data_sets/community_services_data_set.html\">Community Services Data Set</a>.</p>"
        verifyAll {
            businessDefinition.definition == expectedBusinessDefinition
            supportingInformation.definition == expectedSupportingInformation
            dataSetConstraint.definition == expectedDataSetConstraint
        }
    }

    void "should process links from xml for data sets"() {
        given: "the dictionary contains components with definitions containing links"
        NhsDataDictionary dataDictionary = new NhsDataDictionary()

        NhsDDDataSet dataSet = new NhsDDDataSet()
        dataSet.name = "Inter-Provider Transfer Administrative Minimum Data Set"
        dataSet.otherProperties["ddUrl"] = "https://datadictionary.nhs.uk/data_sets/administrative_data_sets/inter-provider_transfer_administrative_minimum_data_set.html"
        dataSet.definition = "<p>The <a href=\"https://datadictionary.nhs.uk/data_sets/administrative_data_sets/inter-provider_transfer_administrative_minimum_data_set.html\">constraints</a> applied to the <a href=\"https://datadictionary.nhs.uk/data_sets/clinical_data_sets/community_services_data_set.html\">Inter-Provider Transfer Administrative Minimum Data Set</a>.</p>"
        dataDictionary.dataSets[dataSet.name] = dataSet

        NhsDDDataSetFolder dataSetFolder = new NhsDDDataSetFolder()
        dataSetFolder.name = "Inter-Provider Transfer Administrative Minimum Data Set Overview"
        dataSetFolder.folderPath = ["Administrative Data Sets", "overviews"]
        dataSetFolder.otherProperties["ddUrl"] = "https://datadictionary.nhs.uk/data_sets/administrative_data_sets/overviews/inter-provider_transfer_administrative_minimum_data_set_overview.html"
        dataSetFolder.definition = "<p>This <a href=\"https://datadictionary.nhs.uk/data_sets/administrative_data_sets/overviews/inter-provider_transfer_administrative_minimum_data_set_overview.html\">Inter-Provider_Transfer_Administrative_Minimum_Data_Set</a> specifies the data necessary to permit the receiving <a href=\"https://datadictionary.nhs.uk/nhs_business_definitions/health_care_provider.html\">Health_Care_Provider</a> to be able to report the <a href=\"https://datadictionary.nhs.uk/classes/patient.html\">PATIENT</a>'s progress along their <a href=\"https://datadictionary.nhs.uk/classes/patient_pathway.html\">PATIENT_PATHWAY</a> and, in particular, their <a href=\"https://datadictionary.nhs.uk/classes/referral_to_treatment_period.html\">REFERRAL_TO_TREATMENT_PERIOD</a>.</p>"
        dataDictionary.dataSetFolders[[dataSetFolder.name]] = [dataSetFolder]

        NhsDDElement dataElement = new NhsDDElement()
        dataElement.name = "ABBREVIATED MENTAL TEST SCORE"
        dataElement.otherProperties["ddUrl"] = "https://datadictionary.nhs.uk/data_elements/abbreviated_mental_test_score.html"
        dataElement.definition = "<a href=\"https://datadictionary.nhs.uk/data_elements/abbreviated_mental_test_score.html\">ABBREVIATED_MENTAL_TEST_SCORE</a> is the <a href=\"https://datadictionary.nhs.uk/attributes/person_score.html\">PERSON_SCORE</a> where the <a href=\"https://datadictionary.nhs.uk/classes/assessment_tool.html\">ASSESSMENT_TOOL</a> is <em>'<a href=\"https://datadictionary.nhs.uk/nhs_business_definitions/abbreviated_mental_test_score.html\">Abbreviated_Mental_Test_Score</a>'</em>.<p>The score is in the range 0 to 10.</p>"
        dataDictionary.elements[dataElement.name] = dataElement

        when: "links in definitions are replaced"
        dataDictionary.processLinksFromXml()

        then: "the modified definitions are correct"
        String expectedDataSetDefinition = "<p>The <a href=\"#/catalogue/item/dataModels/dm%3AInter-Provider%20Transfer%20Administrative%20Minimum%20Data%20Set\">constraints</a> applied to the <a href=\"https://datadictionary.nhs.uk/data_sets/clinical_data_sets/community_services_data_set.html\">Inter-Provider Transfer Administrative Minimum Data Set</a>.</p>"
        String expectedDataSetFolderDefinition = "<p>This <a href=\"#/catalogue/item/folders/fo%3AAdministrative%20Data%20Sets%7Cfo%3Aoverviews\">Inter-Provider Transfer Administrative Minimum Data Set</a> specifies the data necessary to permit the receiving <a href=\"https://datadictionary.nhs.uk/nhs_business_definitions/health_care_provider.html\">Health_Care_Provider</a> to be able to report the <a href=\"https://datadictionary.nhs.uk/classes/patient.html\">PATIENT</a>'s progress along their <a href=\"https://datadictionary.nhs.uk/classes/patient_pathway.html\">PATIENT_PATHWAY</a> and, in particular, their <a href=\"https://datadictionary.nhs.uk/classes/referral_to_treatment_period.html\">REFERRAL_TO_TREATMENT_PERIOD</a>.</p>"
        String expectedDataElementDefinition = "<a href=\"#/catalogue/item/dataModels/dm%3AData%20Elements%7Cdc%3AA%7Cde%3AABBREVIATED%20MENTAL%20TEST%20SCORE\">ABBREVIATED MENTAL TEST SCORE</a> is the <a href=\"https://datadictionary.nhs.uk/attributes/person_score.html\">PERSON_SCORE</a> where the <a href=\"https://datadictionary.nhs.uk/classes/assessment_tool.html\">ASSESSMENT_TOOL</a> is <em>'<a href=\"https://datadictionary.nhs.uk/nhs_business_definitions/abbreviated_mental_test_score.html\">Abbreviated_Mental_Test_Score</a>'</em>.<p>The score is in the range 0 to 10.</p>"
        verifyAll {
            dataSet.definition == expectedDataSetDefinition
            dataSetFolder.definition == expectedDataSetFolderDefinition
            dataElement.definition == expectedDataElementDefinition
        }
    }
}
