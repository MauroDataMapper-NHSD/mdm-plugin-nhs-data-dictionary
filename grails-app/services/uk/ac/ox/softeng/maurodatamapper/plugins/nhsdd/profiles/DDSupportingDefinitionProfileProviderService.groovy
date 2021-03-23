package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles

import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService

import groovy.util.logging.Slf4j

@Slf4j
class DDSupportingDefinitionProfileProviderService extends JsonProfileProviderService {

    @Override
    String getMetadataNamespace() {
        'uk.nhs.datadictionary.supporting information'
    }

    @Override
    String getDisplayName() {
        'NHS Data Dictionary - Supporting Information'
    }

    @Override
    String getVersion() {
        '1.0.0'
    }

    @Override
    String getJsonResourceFile() {
        return 'supportingDefinitionProfile.json'
    }

    @Override
    List<String> profileApplicableForDomains() {
        return ['Term']
    }



}
