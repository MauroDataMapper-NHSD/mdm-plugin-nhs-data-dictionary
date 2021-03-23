package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles

import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService

import groovy.util.logging.Slf4j

@Slf4j
class DDBusinessDefinitionProfileProviderService extends JsonProfileProviderService {

    @Override
    String getMetadataNamespace() {
        'uk.nhs.datadictionary.NHS business definition'
    }

    @Override
    String getDisplayName() {
        'NHS Data Dictionary - NHS Business Definition'
    }

    @Override
    String getVersion() {
        '1.0.0'
    }

    @Override
    String getJsonResourceFile() {
        return 'businessDefinitionProfile.json'
    }

    @Override
    List<String> profileApplicableForDomains() {
        return ['Term']
    }



}
