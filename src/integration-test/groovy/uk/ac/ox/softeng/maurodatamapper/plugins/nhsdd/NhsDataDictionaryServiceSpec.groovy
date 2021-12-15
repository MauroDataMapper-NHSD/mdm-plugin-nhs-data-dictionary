package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import org.junit.Assert
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
    TerminologyService terminologyService
    CodeSetService codeSetService
    DataModelService dataModelService

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
        checkFolderContentsWithChildren(dd, 3, 3, 0, 1)

        and: 'direct children'
        //        outputChildFolderContents(dd, 'dd')
        checkFolderContentsWithChildrenOnly(dd.childFolders.find {it.label == 'Attribute Terminologies'}, 24)
        checkFolderContentsWithChildrenOnly(dd.childFolders.find {it.label == 'Data Element CodeSets'}, 24)
        checkFolderContentsWithChildrenOnly(dd.childFolders.find {it.label == 'Data Sets'}, 8)

        when:
        Folder dataSets = dd.childFolders.find {it.label == 'Data Sets'}
        Folder attributes = dd.childFolders.find {it.label == 'Attribute Terminologies'}
        Folder elements = dd.childFolders.find {it.label == 'Data Element CodeSets'}

        then: 'children of attributes'
        //        outputChildFolderContents(attributes, 'attributes')
        //        outputChildFolderContents(elements, 'elements')
        //        outputChildFolderContents(dataSets, 'dataSets')
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'A'}, 67)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'B'}, 32)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'C'}, 130)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'D'}, 31)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'E'}, 46)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'F'}, 24)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'G'}, 12)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'H'}, 18)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'I'}, 28)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'J'}, 7)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'K'}, 1)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'L'}, 29)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'M'}, 66)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'N'}, 33)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'O'}, 25)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'P'}, 138)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'Q'}, 4)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'R'}, 66)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'S'}, 88)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'T'}, 36)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'U'}, 12)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'V'}, 9)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'W'}, 15)
        checkFolderWithTerminologiesOnly(attributes.childFolders.find {it.label == 'Y'}, 1)

        and: 'children of elements'
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'A'}, 74)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'B'}, 49)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'C'}, 169)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'D'}, 37)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'E'}, 52)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'F'}, 25)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'G'}, 14)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'H'}, 18)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'I'}, 35)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'J'}, 6)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'K'}, 1)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'L'}, 30)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'M'}, 81)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'N'}, 49)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'O'}, 35)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'P'}, 163)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'Q'}, 5)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'R'}, 74)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'S'}, 107)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'T'}, 48)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'U'}, 14)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'V'}, 11)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'W'}, 18)
        checkFolderWithCodeSetsOnly(elements.childFolders.find {it.label == 'Y'}, 1)

        and: 'children of datasets'
        checkFolderWithDataModelsOnly(dataSets.childFolders.find {it.label == 'Administrative Data Sets'}, 2)
        checkFolderWithDataModelsOnly(dataSets.childFolders.find {it.label == 'CDS V6-2'}, 47)
        checkFolderWithDataModelsOnly(dataSets.childFolders.find {it.label == 'CDS V6-3'}, 15)
        checkFolderWithDataModelsOnly(dataSets.childFolders.find {it.label == 'Central Return Data Sets'}, 6)
        checkFolderContentsWithChildren(dataSets.childFolders.find {it.label == 'Clinical Content'}, 1, 0, 0, 1)
        checkFolderContentsWithChildren(dataSets.childFolders.find {it.label == 'Clinical Data Sets'}, 2, 0, 0, 13)
        checkFolderContentsWithChildren(dataSets.childFolders.find {it.label == 'Supporting Data Sets'}, 1, 0, 0, 8)
        checkFolderContentsWithChildrenOnly(dataSets.childFolders.find {it.label == 'Retired'}, 9)

        when:
        Folder clinicalContent = dataSets.childFolders.find {it.label == 'Clinical Content'}
        Folder clinicalDataSets = dataSets.childFolders.find {it.label == 'Clinical Data Sets'}
        Folder supportingDataSets = dataSets.childFolders.find {it.label == 'Supporting Data Sets'}
        Folder retired = dataSets.childFolders.find {it.label == 'Retired'}

        then: 'children if clinical content'
        //        outputChildFolderContents(clinicalContent, 'clinicalContent')
        //        outputChildFolderContents(clinicalDataSets, 'clinicalDataSets')
        //        outputChildFolderContents(supportingDataSets, 'supportingDataSets')
        //        outputChildFolderContents(retired, 'retired')
        checkFolderContents(clinicalContent.childFolders.find {it.label == 'National Joint Registry Data Set'}, 0, 0, 6)

        and: 'children of clinical datasets'
        checkFolderContents(clinicalDataSets.childFolders.find {it.label == 'COSDS'}, 0, 0, 15)
        checkFolderContents(clinicalDataSets.childFolders.find {it.label == 'National Neonatal Data Set'}, 0, 0, 2)

        and: 'children of supporting datasets'
        checkFolderContents(supportingDataSets.childFolders.find {it.label == 'PLICS Data Set'}, 0, 0, 10)

        and: 'children of retired'
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'CDS V6-1'}, 27)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'CDS V6-2'}, 1)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'CDS V6 Old Layout'}, 27)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'Central Returns Data Sets'}, 29)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'Clinical Content'}, 2)
        checkFolderContentsWithChildren(retired.childFolders.find {it.label == 'Clinical Data Sets'}, 1, 0, 0, 13)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'Commissioning Data Set V5'}, 26)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'Supporting Data Sets'}, 1)
        checkFolderWithDataModelsOnly(retired.childFolders.find {it.label == 'CDS Supporting Information'}, 2)

        when:
        Folder retiredClinicalDataSets = retired.childFolders.find {it.label == 'Clinical Data Sets'}

        then: 'children of retired clinical datasets'
        //        outputChildFolderContents(retiredClinicalDataSets, 'retiredClinicalDataSets')
        checkFolderWithDataModelsOnly(retiredClinicalDataSets.childFolders.find {it.label == 'National Renal Data Set'}, 8)
    }

    void checkFolderContentsWithChildren(Folder check, int childFolderCount, int terminologyCount, int codeSetCount, int dataModelCount) {
        Assert.assertEquals "ChildFolders in ${check.label}", childFolderCount, check.childFolders?.size() ?: 0
        checkFolderContents(check, terminologyCount, codeSetCount, dataModelCount)
    }

    void checkFolderContentsWithChildrenOnly(Folder check, int childFolderCount) {
        Assert.assertEquals "ChildFolders in ${check.label}", childFolderCount, check.childFolders?.size() ?: 0
        checkFolderContents(check, 0, 0, 0)
    }

    void checkFolderWithTerminologiesOnly(Folder check, int terminologyCount) {
        checkFolderContents(check, terminologyCount, 0, 0)
    }

    void checkFolderWithCodeSetsOnly(Folder check, int codeSetCount) {
        checkFolderContents(check, 0, codeSetCount, 0)
    }

    void checkFolderWithDataModelsOnly(Folder check, int dataModelCount) {
        checkFolderContents(check, 0, 0, dataModelCount)
    }

    void checkFolderContents(Folder check, int terminologyCount, int codeSetCount, int dataModelCount) {
        List<Terminology> terminologies = terminologyService.findAllByFolderId(check.id)
        List<CodeSet> codeSets = codeSetService.findAllByFolderId(check.id)
        List<DataModel> dataModels = dataModelService.findAllByFolderId(check.id)

        if (terminologyCount) {
            Assert.assertEquals "Terminologies in ${check.label}", terminologyCount, terminologies.size()
        } else {
            Assert.assertTrue("No Terminologies in ${check.label}", terminologies.isEmpty())
        }
        if (codeSetCount) {
            Assert.assertEquals "CodeSets in ${check.label}", codeSetCount, codeSets.size()
        } else {
            Assert.assertTrue("No CodeSets in ${check.label}", codeSets.isEmpty())
        }
        if (dataModelCount) {
            Assert.assertEquals "DataModels in ${check.label}", dataModelCount, dataModels.size()
        } else {
            Assert.assertTrue("No DataModels in ${check.label}", dataModels.isEmpty())
        }

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
