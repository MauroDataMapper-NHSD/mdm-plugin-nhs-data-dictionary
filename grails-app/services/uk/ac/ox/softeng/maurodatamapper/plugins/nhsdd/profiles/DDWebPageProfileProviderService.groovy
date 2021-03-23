package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles

import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService

import groovy.util.logging.Slf4j

@Slf4j
class DDWebPageProfileProviderService extends JsonProfileProviderService {

    @Override
    String getMetadataNamespace() {
        'uk.nhs.datadictionary.Supporting Information'
    }

    @Override
    String getDisplayName() {
        'NHS Data Dictionary - Web Page'
    }

    @Override
    String getVersion() {
        '1.0.0'
    }

    @Override
    String getJsonResourceFile() {
        return 'webPageProfile.json'
    }

    @Override
    List<String> profileApplicableForDomains() {
        return ['Term']
    }



}
