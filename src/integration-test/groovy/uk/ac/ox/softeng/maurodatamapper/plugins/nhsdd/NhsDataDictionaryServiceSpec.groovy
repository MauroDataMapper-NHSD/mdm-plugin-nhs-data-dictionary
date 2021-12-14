package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @since 14/12/2021
 */
@Slf4j
@Integration
@Rollback
class NhsDataDictionaryServiceSpec extends BaseIntegrationSpec {

    @Shared
    Path resourcesPath

    NhsDataDictionaryService nhsDataDictionaryService

    @Shared
    User user

    @Override
    void preDomainDataSetup() {
        assert Authority.findByLabel('Test Authority')
    }

    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources')
        user = UnloggedUser.instance
    }

    @Override
    void setupDomainData() {

    }

    void 'I01 : test ingest of November 2021'() {

        given:
        setupData()
        def xml = loadXml('november2021.xml')
        assert xml

        when:
        VersionedFolder folder = nhsDataDictionaryService.ingest(user, xml, 'November 2021', null, null, null)

        then:
        folder
    }

    def loadXml(String filename) {
        Path testFilePath = resourcesPath.resolve(filename).toAbsolutePath()
        assert Files.exists(testFilePath)
        new XmlSlurper().parse(Files.newBufferedReader(testFilePath))

    }

    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve(filename).toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }
}
