package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles

import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService

class DDClassRelationshipPropertiesProfileProviderService extends JsonProfileProviderService{

    String getMetadataNamespace() {
        'uk.nhs.datadictionary.class relationship'
    }

    String getDisplayName() {
        'NHS Data Dictionary - Class relationship Properties'
    }

    String getVersion() {
        '1.0.0'
    }

    String getJsonResourceFile() {
        return 'classRelationshipProfile.json'
    }

    List<String> profileApplicableForDomains() {
        return ['DataClass', 'DataElement']
    }

}
