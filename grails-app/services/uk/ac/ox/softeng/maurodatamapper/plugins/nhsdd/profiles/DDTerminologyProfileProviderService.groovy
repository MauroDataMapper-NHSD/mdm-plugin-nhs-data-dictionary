package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles

import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService

import groovy.util.logging.Slf4j

@Slf4j
class DDTerminologyProfileProviderService extends JsonProfileProviderService {

    @Override
    String getMetadataNamespace() {
        'uk.nhs.datadictionary.terminology'
    }

    @Override
    String getDisplayName() {
        'NHS Data Dictionary - Terminology'
    }

    @Override
    String getVersion() {
        '1.0.0'
    }

    @Override
    String getJsonResourceFile() {
        return 'terminologyProfile.json'
    }

    @Override
    List<String> profileApplicableForDomains() {
        return ['Terminology']
    }



}
