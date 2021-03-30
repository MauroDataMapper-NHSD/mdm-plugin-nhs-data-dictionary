package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles

import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService

import groovy.util.logging.Slf4j

@Slf4j
class DDChangeNoticeProfileProviderService extends JsonProfileProviderService {

    @Override
    String getMetadataNamespace() {
        'uk.nhs.datadictionary.changeNotice'
    }

    @Override
    String getDisplayName() {
        'NHS Data Dictionary - Change Notice'
    }

    @Override
    String getVersion() {
        '1.0.0'
    }

    @Override
    String getJsonResourceFile() {
        return 'changeNoticeProfile.json'
    }

    @Override
    List<String> profileApplicableForDomains() {
        return ['DataModel']
    }



}
