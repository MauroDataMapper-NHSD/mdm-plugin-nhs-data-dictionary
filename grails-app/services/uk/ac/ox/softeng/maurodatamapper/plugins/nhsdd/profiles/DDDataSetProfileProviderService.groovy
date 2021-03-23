package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles

import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService

import groovy.util.logging.Slf4j

@Slf4j
class DDDataSetProfileProviderService extends JsonProfileProviderService {

    @Override
    String getMetadataNamespace() {
        'uk.nhs.datadictionary.data set'
    }

    @Override
    String getDisplayName() {
        'NHS Data Dictionary - Data Set'
    }

    @Override
    String getVersion() {
        '1.0.0'
    }

    @Override
    String getJsonResourceFile() {
        return 'dataSetProfile.json'
    }

    @Override
    List<String> profileApplicableForDomains() {
        return ['DataModel']
    }



}
