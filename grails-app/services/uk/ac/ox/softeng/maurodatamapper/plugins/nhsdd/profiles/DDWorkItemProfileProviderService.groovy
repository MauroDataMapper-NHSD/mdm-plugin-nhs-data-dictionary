package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles

import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService

import groovy.util.logging.Slf4j

@Slf4j
class DDWorkItemProfileProviderService extends JsonProfileProviderService {

    @Override
    String getMetadataNamespace() {
        'uk.nhs.datadictionary.workItem'
    }

    @Override
    String getDisplayName() {
        'NHS Data Dictionary - Work Item'
    }

    @Override
    String getVersion() {
        '1.0.0'
    }

    @Override
    String getJsonResourceFile() {
        return 'workItemProfile.json'
    }

    @Override
    List<String> profileApplicableForDomains() {
        return ['VersionedFolder']
    }



}
