package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import spock.lang.Shared
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

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
    TerminologyService terminologyService
    CodeSetService codeSetService
    DataModelService dataModelService
    VersionedFolderService versionedFolderService
    FolderService folderService

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
        VersionedFolder dd = nhsDataDictionaryService.ingest(user, xml, 'November 2021', false, null, null)

        then:
        dd
        checkNovember2021(dd, false)
    }

    void 'I02 : test double ingest of November 2021'() {
        // This is to test that ingesting again doesnt error and also to test the batch deletion code
        given:
        setupData()
        def xml = loadXml('november2021.xml')
        assert xml

        when: 'ingest once'
        VersionedFolder dd = nhsDataDictionaryService.ingest(user, xml, 'November 2021', false, null, null)

        then:
        noExceptionThrown()
        dd

        when: 'ingest again'
        log.info('---------- 2nd Ingest --------')
        // Delete old folder complete in 21 secs 547 ms with the batching
        dd = nhsDataDictionaryService.ingest(user, xml, 'November 2021', false, null, null)

        then:
        noExceptionThrown()
        dd
        checkNovember2021(dd, false)
    }

    void 'F01 : Finalise Nov 2021 ingest'() {
        given:
        setupData()
        def xml = loadXml('november2021.xml')
        assert xml

        when: 'finalise'
        // finalise dictionary complete in 39 secs 787 ms
        VersionedFolder dd = nhsDataDictionaryService.ingest(user, xml, 'November 2021', true, null, null)

        then:
        noExceptionThrown()
        dd
        checkNovember2021(dd, true)
    }

    void 'B01 : Branch Nov 2021 ingest'() {
        given:
        setupData()
        def xml = loadXml('november2021.xml')
        assert xml

        when: 'finalise'
        // finalise dictionary complete in 39 secs 787 ms
        VersionedFolder dd = nhsDataDictionaryService.ingest(user, xml, 'November 2021', true, null, null)

        then:
        noExceptionThrown()
        dd

        when:
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        VersionedFolder release = versionedFolderService.get(dd.id)
        long start = System.currentTimeMillis()
        VersionedFolder mainBranch = versionedFolderService.createNewBranchModelVersion(VersionAwareConstraints.DEFAULT_BRANCH_NAME,
                                                                                        release, user, true,
                                                                                        PublicAccessSecurityPolicyManager.instance as UserSecurityPolicyManager)
        log.info('New branch creation took {}', Utils.timeTaken(start))

        if (mainBranch && mainBranch.hasErrors()) {
            GormUtils.outputDomainErrors(messageSource, mainBranch)
        }

        then:
        mainBranch
        !mainBranch.hasErrors()

        when:
        VersionedFolder validated = folderService.validate(mainBranch) as VersionedFolder

        then:
        !validated.hasErrors()

        when:
        versionedFolderService.save(validated, validate: false, flush: true)

        then:
        noExceptionThrown()
        checkNovember2021(mainBranch, false)
    }

    void outputChildFolderContents(Folder parentFolder, String variableName) {
        log.warn '\n{}', parentFolder.childFolders.collect {cf ->
            outputFolderContents(variableName, cf)
        }.join('\n')
    }

    String outputFolderContents(String parent, Folder check) {
        List<Terminology> terminologies = terminologyService.findAllByFolderId(check.id)
        List<CodeSet> codeSets = codeSetService.findAllByFolderId(check.id)
        List<DataModel> dataModels = dataModelService.findAllByFolderId(check.id)
        if (check.childFolders) {
            return "checkFolderContentsWithChildren(${parent}.childFolders.find{ it.label == '${check.label}' }, ${check.childFolders?.size() ?: 0}, " +
                   "${terminologies.size()}, ${codeSets.size()}, " +
                   "${dataModels.size()})"
        }
        "checkFolderContents(${parent}.childFolders.find{ it.label == '${check.label}' }, ${terminologies.size()}, ${codeSets.size()}, " +
        "${dataModels.size()})"
    }

    def loadXml(String filename) {
        Path testFilePath = resourcesPath.resolve(filename).toAbsolutePath()
        assert Files.exists(testFilePath)
        def xml = new XmlSlurper().parse(Files.newBufferedReader(testFilePath))
        xml
    }

    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve(filename).toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    private void checkFolderContentsWithChildren(Folder check, int childFolderCount, int terminologyCount, int codeSetCount, int dataModelCount, boolean finalised) {
        assertEquals "ChildFolders in ${check.label}", childFolderCount, check.childFolders?.size() ?: 0
        checkFolderContents(check, terminologyCount, codeSetCount, dataModelCount, finalised)
    }

    private void checkFolderContentsWithChildrenOnly(Folder check, int childFolderCount, boolean finalised) {
        assertEquals "ChildFolders in ${check.label}", childFolderCount, check.childFolders?.size() ?: 0
        checkFolderContents(check, 0, 0, 0, finalised)
    }

    private void checkFolderWithTerminologiesOnly(Folder check, int terminologyCount, boolean finalised) {
        checkFolderContents(check, terminologyCount, 0, 0, finalised)
    }

    private void checkFolderWithCodeSetsOnly(Folder check, int codeSetCount, boolean finalised) {
        checkFolderContents(check, 0, codeSetCount, 0, finalised)
    }

    private void checkFolderWithDataModelsOnly(Folder check, int dataModelCount, boolean finalised) {
        checkFolderContents(check, 0, 0, dataModelCount, finalised)
    }

    private void checkFolderContents(Folder check, int terminologyCount, int codeSetCount, int dataModelCount, boolean finalised) {
        List<Terminology> terminologies = terminologyService.findAllByFolderId(check.id)
        List<CodeSet> codeSets = codeSetService.findAllByFolderId(check.id)
        List<DataModel> dataModels = dataModelService.findAllByFolderId(check.id)

        checkModels(check.label, 'Terminologies', terminologyCount, finalised, terminologies)
        checkModels(check.label, 'CodeSets', codeSetCount, finalised, codeSets)
        checkModels(check.label, 'DataModels', dataModelCount, finalised, dataModels)
    }

    void checkModels(String label, String name, int count, boolean finalised, List<Model> models) {
        if (count) {
            assertEquals "${name} in ${label}", count, models.size()
            assertTrue "${name} in ${label} are finalised ${finalised}", models.every {it.finalised == finalised}
        } else {
            assertTrue("No ${name} in ${label}", models.isEmpty())
        }
    }

    void checkModelItemIndexes(Collection<ModelItem> modelItems, String path) {
        if (!modelItems) return
        modelItems.sort().eachWithIndex {mi, i ->
            assertEquals("${path} >> ${mi.domainType} ${mi.label} idx", i, mi.idx)
        }
        if (modelItems.first() instanceof DataClass) {
            modelItems.each {
                checkModelItemIndexes(it.dataClasses, "${path}|${it.label}")
                checkModelItemIndexes(it.dataElements, "${path}|${it.label}")
            }
        }
    }


    void checkNovember2021(VersionedFolder nhsdd, boolean finalised) {
        assertEquals 'NHSDD Folder', finalised, nhsdd.finalised
        checkFolderContentsWithChildren(nhsdd, 3, 3, 0, 1, finalised)

        DataModel coreDataModel = dataModelService.findByLabel(NhsDataDictionary.CORE_MODEL_NAME)
        assertNotNull(NhsDataDictionary.CORE_MODEL_NAME, coreDataModel)
        assertEquals("NhsDataDictionary.CORE_MODEL_NAME dataclasses", 369, coreDataModel.dataClasses.size())
        assertEquals("NhsDataDictionary.CORE_MODEL_NAME child dataclasses", 3, coreDataModel.childDataClasses.size())
        checkModelItemIndexes(coreDataModel.childDataClasses, NhsDataDictionary.CORE_MODEL_NAME)
        checkModelItemIndexes(coreDataModel.dataTypes, NhsDataDictionary.CORE_MODEL_NAME)

        // 'direct children'
        //        outputChildFolderContents(dd, 'dd')
        checkFolderContentsWithChildrenOnly(nhsdd.childFolders.find {it.label == 'Attribute Terminologies'}, 24, finalised)
        checkFolderContentsWithChildrenOnly(nhsdd.childFolders.find {it.label == 'Data Element CodeSets'}, 24, finalised)
        checkFolderContentsWithChildrenOnly(nhsdd.childFolders.find {it.label == 'Data Sets'}, 8, finalised)


        Folder dataSets = nhsdd.childFolders.find {it.label == 'Data Sets'}
        Folder attributes = nhsdd.childFolders.find {it.label == 'Attribute Terminologies'}
        Folder elements = nhsdd.childFolders.find {it.label == 'Data Element CodeSets'}

        // 'children of attributes'
        //        outputChildFolderContents(attributes, 'attributes')
        //        outputChildFolderContents(elements, 'elements')
        //        outputChildFolderContents(dataSets, 'dataSets')
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'A'}, 67, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'B'}, 32, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'C'}, 130, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'D'}, 31, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'E'}, 46, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'F'}, 24, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'G'}, 12, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'H'}, 18, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'I'}, 28, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'J'}, 7, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'K'}, 1, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'L'}, 29, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'M'}, 66, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'N'}, 33, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'O'}, 25, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'P'}, 138, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'Q'}, 4, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'R'}, 66, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'S'}, 88, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'T'}, 36, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'U'}, 12, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'V'}, 9, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'W'}, 15, finalised)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'Y'}, 1, finalised)

        // 'children of elements'
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'A'}, 74, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'B'}, 49, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'C'}, 169, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'D'}, 37, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'E'}, 52, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'F'}, 25, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'G'}, 14, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'H'}, 18, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'I'}, 35, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'J'}, 6, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'K'}, 1, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'L'}, 30, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'M'}, 81, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'N'}, 49, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'O'}, 35, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'P'}, 163, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'Q'}, 5, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'R'}, 74, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'S'}, 107, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'T'}, 48, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'U'}, 14, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'V'}, 11, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'W'}, 18, finalised)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'Y'}, 1, finalised)

        // 'children of datasets'
        checkFolderWithDataModelsOnly(dataSets.childFolders.find {it.label == 'Administrative Data Sets'}, 2, finalised)
        checkFolderWithDataModelsOnly(dataSets.childFolders.find {it.label == 'CDS V6-2'}, 47, finalised)
        checkFolderWithDataModelsOnly(dataSets.childFolders.find {it.label == 'CDS V6-3'}, 15, finalised)
        checkFolderWithDataModelsOnly(dataSets.childFolders.find {it.label == 'Central Return Data Sets'}, 6, finalised)
        checkFolderContentsWithChildren(dataSets.childFolders.find {it.label == 'Clinical Content'}, 1, 0, 0, 1, finalised)
        checkFolderContentsWithChildren(dataSets.childFolders.find {it.label == 'Clinical Data Sets'}, 2, 0, 0, 13, finalised)
        checkFolderContentsWithChildren(dataSets.childFolders.find {it.label == 'Supporting Data Sets'}, 1, 0, 0, 8, finalised)
        checkFolderContentsWithChildrenOnly(dataSets.childFolders.find {it.label == 'Retired'}, 9, finalised)

        Folder clinicalContent = dataSets.childFolders.find {it.label == 'Clinical Content'}
        Folder clinicalDataSets = dataSets.childFolders.find {it.label == 'Clinical Data Sets'}
        Folder supportingDataSets = dataSets.childFolders.find {it.label == 'Supporting Data Sets'}
        Folder retired = dataSets.childFolders.find {it.label == 'Retired'}

        // 'children if clinical content'
        //        outputChildFolderContents(clinicalContent, 'clinicalContent')
        //        outputChildFolderContents(clinicalDataSets, 'clinicalDataSets')
        //        outputChildFolderContents(supportingDataSets, 'supportingDataSets')
        //        outputChildFolderContents(retired, 'retired')
        checkFolderContents(clinicalContent.childFolders.find {it.label == 'National Joint Registry Data Set'}, 0, 0, 6, finalised)

        // 'children of clinical datasets'
        checkFolderContents(clinicalDataSets.childFolders.find {it.label == 'COSDS'}, 0, 0, 15, finalised)
        checkFolderContents(clinicalDataSets.childFolders.find {it.label == 'National Neonatal Data Set'}, 0, 0, 2, finalised)

        // 'children of supporting datasets'
        checkFolderContents(supportingDataSets.childFolders.find {it.label == 'PLICS Data Set'}, 0, 0, 10, finalised)

        // 'children of retired'
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'CDS V6-1'}, 27, finalised)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'CDS V6-2'}, 1, finalised)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'CDS V6 Old Layout'}, 27, finalised)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'Central Returns Data Sets'}, 29, finalised)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'Clinical Content'}, 2, finalised)
        checkFolderContentsWithChildren(retired.childFolders.find {it.label == 'Clinical Data Sets'}, 1, 0, 0, 13, finalised)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'Commissioning Data Set V5'}, 26, finalised)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'Supporting Data Sets'}, 1, finalised)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'CDS Supporting Information'}, 2, finalised)


        Folder retiredClinicalDataSets = retired.childFolders.find {it.label == 'Clinical Data Sets'}

        // 'children of retired clinical datasets'
        //        outputChildFolderContents(retiredClinicalDataSets, 'retiredClinicalDataSets')
        checkFolderWithDataModelsOnly(retiredClinicalDataSets.childFolders.find {it.label == 'National Renal Data Set'}, 8, finalised)
    }

    /**
     * Code to minify any XML
     *
     * you will need to add to the dependencies.gradle file
     implementation 'org.dom4j:dom4j:2.1.3'
     implementation 'jaxen:jaxen:1.2.0'
     *
     * Then run the below in a groovy console
     *
     import groovy.xml.XmlUtil
     import org.dom4j.Document
     import org.dom4j.DocumentHelper
     import org.dom4j.io.OutputFormat
     import org.dom4j.io.XMLWriter

     import java.nio.file.Files
     import java.nio.file.Path
     import java.nio.file.Paths

     String minify(String absolutePathToDirectory, String xmlFileName) {Path directory = Paths.get(absolutePathToDirectory)
     Path testFilePath = directory.resolve(xmlFileName)
     assert Files.exists(testFilePath)
     def xml = new XmlSlurper().parse(Files.newBufferedReader(testFilePath))
     String xmlStr = XmlUtil.serialize(xml)
     Document document = DocumentHelper.parseText(xmlStr)
     OutputFormat format = OutputFormat.createCompactFormat()
     StringWriter stringWriter = new StringWriter()
     XMLWriter writer = new XMLWriter(stringWriter, format)
     writer.write(document)
     String resultStr = stringWriter.toString()
     Files.write(directory.resolve('minified.xml'), resultStr.bytes)}minify('<ABSOLUTE_FILE_PATH_TO_DIRECTORY>', 'XML_FILE_NAME')
     */
}
