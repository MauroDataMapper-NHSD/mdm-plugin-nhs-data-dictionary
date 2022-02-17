package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser

import grails.gorm.transactions.Transactional
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @since 17/02/2022
 */
@Transactional
@Slf4j
class TestingService {

    NhsDataDictionaryService nhsDataDictionaryService
    VersionedFolderService versionedFolderService
    FolderService folderService

    @Autowired
    SessionFactory sessionFactory

    def buildTestData(User user) {
        // Ingest, finalise september
        UUID releaseId = cleanIngest(user, 'september2021.xml', 'September 2021')
        // Ingest november
        UUID novBranchId = cleanIngest(user, 'november2021.xml', 'November 2021', false)
        // Change the folder name and link to the sept release
        VersionedFolder release = versionedFolderService.get(releaseId)
        VersionedFolder novBranch = versionedFolderService.get(novBranchId)
        novBranch.label = 'NHS Data Dictionary (September 2021)'
        versionedFolderService.setFolderIsNewBranchModelVersionOfFolder(novBranch, release, UnloggedUser.instance)
        versionedFolderService.save(novBranch, validate: false, flush: true)
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        new Tuple2(releaseId.toString(), novBranchId.toString())
    }

    def branchTestData(User user, String releaseId) {
        // Create a september branch
        VersionedFolder release = versionedFolderService.get(releaseId)
        UUID septBranchId = createBranch(user, release, 'september_2021')
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        new Tuple2(releaseId, septBranchId.toString())

    }

    UUID cleanIngest(User user, String name, String releaseDate, boolean finalised = true, boolean deletePrevious = false) {
        log.info('---------- Ingesting {} ----------', name)
        def xml = loadXml(name)
        assert xml
        VersionedFolder dd = nhsDataDictionaryService.ingest(user, xml, releaseDate, finalised, null, null, deletePrevious)
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        log.info('---------- Finished Ingesting {} ----------', name)
        dd.id
    }

    def loadXml(String filename) {
        Path resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources')
        Path testFilePath = resourcesPath.resolve(filename).toAbsolutePath()
        assert Files.exists(testFilePath)
        def xml = new XmlSlurper().parse(Files.newBufferedReader(testFilePath))
        xml
    }


    UUID createBranch(User user, VersionedFolder release, String branchName) {
        log.info('---------- Starting {} branch ----------', branchName)
        VersionedFolder branch = versionedFolderService.createNewBranchModelVersion(branchName,
                                                                                    release, user, true,
                                                                                    PublicAccessSecurityPolicyManager.instance as UserSecurityPolicyManager)
        VersionedFolder validated = folderService.validate(branch) as VersionedFolder
        assert !validated.hasErrors()
        VersionedFolder saved = versionedFolderService.save(validated, validate: false, flush: true)
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        log.info('---------- Finished {} branch ----------', branchName)
        saved.id
    }
}
