package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles

class DDMetaPropertiesProfileProviderService {
    String getMetadataNamespace() {
        'uk.nhs.datadictionary.class'
    }

    String getDisplayName() {
        'NHS Data Dictionary - Meta Properties'
    }

    String getVersion() {
        '1.0.0'
    }

    String getJsonResourceFile() {
        return 'metaPropertyProfile.json'
    }

    List<String> profileApplicableForDomains() {
        return ['Class']
    }


}
