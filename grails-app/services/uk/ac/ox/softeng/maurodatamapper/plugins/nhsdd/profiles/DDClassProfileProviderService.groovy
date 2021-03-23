package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles

import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService

import groovy.util.logging.Slf4j

@Slf4j
class DDClassProfileProviderService extends JsonProfileProviderService {

    @Override
    String getMetadataNamespace() {
        'uk.nhs.datadictionary.class'
    }

    @Override
    String getDisplayName() {
        'NHS Data Dictionary - Class'
    }

    @Override
    String getVersion() {
        '1.0.0'
    }

    @Override
    String getJsonResourceFile() {
        return 'classProfile.json'
    }

    @Override
    List<String> profileApplicableForDomains() {
        return ['DataClass']
    }



}
