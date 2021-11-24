package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles

import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService

import groovy.util.logging.Slf4j

@Slf4j
class DDCodeSetProfileProviderService extends JsonProfileProviderService {

    @Override
    String getMetadataNamespace() {
        'uk.nhs.datadictionary.codeset'
    }

    @Override
    String getDisplayName() {
        'NHS Data Dictionary - CodeSet'
    }

    @Override
    String getVersion() {
        '1.0.0'
    }

    @Override
    String getJsonResourceFile() {
        return 'codeSetProfile.json'
    }

    @Override
    List<String> profileApplicableForDomains() {
        return ['CodeSet']
    }



}
