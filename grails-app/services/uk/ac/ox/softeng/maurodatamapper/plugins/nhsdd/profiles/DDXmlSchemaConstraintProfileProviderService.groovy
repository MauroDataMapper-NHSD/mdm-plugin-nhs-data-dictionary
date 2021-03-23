package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles

import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService

import groovy.util.logging.Slf4j

@Slf4j
class DDXmlSchemaConstraintProfileProviderService extends JsonProfileProviderService {

    @Override
    String getMetadataNamespace() {
        'uk.nhs.datadictionary.XML schema constraint'
    }

    @Override
    String getDisplayName() {
        'NHS Data Dictionary - XML Schema Constraint'
    }

    @Override
    String getVersion() {
        '1.0.0'
    }

    @Override
    String getJsonResourceFile() {
        return 'xmlSchemaConstraintProfile.json'
    }

    @Override
    List<String> profileApplicableForDomains() {
        return ['Term']
    }



}
