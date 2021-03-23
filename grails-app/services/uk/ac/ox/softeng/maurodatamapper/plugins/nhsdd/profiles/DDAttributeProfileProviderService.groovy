package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles

import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService

import groovy.util.logging.Slf4j

@Slf4j
class DDAttributeProfileProviderService extends JsonProfileProviderService {

    @Override
    String getMetadataNamespace() {
        'uk.nhs.datadictionary.attribute'
    }

    @Override
    String getDisplayName() {
        'NHS Data Dictionary - Attribute'
    }

    @Override
    String getVersion() {
        '1.0.0'
    }

    @Override
    String getJsonResourceFile() {
        return 'attributeProfile.json'
    }

    @Override
    List<String> profileApplicableForDomains() {
        return ['DataElement']
    }



}
