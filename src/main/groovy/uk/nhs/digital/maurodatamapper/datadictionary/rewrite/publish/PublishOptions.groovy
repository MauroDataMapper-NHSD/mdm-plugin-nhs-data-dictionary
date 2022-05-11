package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish

import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDBusinessDefinition
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDDataSet
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDDataSetConstraint
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDElement
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDSupportingInformation
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

class PublishOptions {

    // Default for all options is true
    boolean publishAttributes = true
    boolean publishElements = true
    boolean publishClasses = true
    boolean publishDataSets = true
    boolean publishBusinessDefinitions = true
    boolean publishSupportingInformation = true
    boolean publishDataSetConstraints = true

    static PublishOptions fromParameters(Map<String, String> params = [:]) {

        new PublishOptions().tap {
            publishAttributes = Boolean.parseBoolean(params["attributes"]?:'true')
            publishElements = Boolean.parseBoolean(params["elements"]?:'true')
            publishClasses = Boolean.parseBoolean(params["classes"]?:'true')
            publishDataSets = Boolean.parseBoolean(params["dataSets"]?:'true')
            publishBusinessDefinitions = Boolean.parseBoolean(params["businessDefinitions"]?:'true')
            publishSupportingInformation = Boolean.parseBoolean(params["supportingInformation"]?:'true')
            publishDataSetConstraints = Boolean.parseBoolean(params["dataSetConstraints"]?:'true')

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
    }


}
