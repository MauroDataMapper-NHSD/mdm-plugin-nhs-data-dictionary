package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import org.springframework.context.MessageSource
import org.springframework.test.annotation.Rollback
import spock.lang.Specification
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.DataSetParser
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary


@Slf4j
@Integration
@Rollback
class DataSetParserSpec extends BaseIntegrationSpec {

    static List<String> testFiles = [
        "Inter-provider Transfer Administrative Minimum Data Set",
    ]

    DataModel parseDataSet(String filename) {
        log.debug('Ingesting {}', filename)

        String testFileContents = getClass().classLoader.getResourceAsStream (filename + ".xml").text
        XmlSlurper xmlSlurper = new XmlSlurper()

        System.err.println()

        NhsDataDictionary newDataDictionary = new NhsDataDictionary()
        DataModel dataSetDataModel = new DataModel(
            label: filename,
            description: testFileContents,
            //createdBy: currentUser.emailAddress,
            type: DataModelType.DATA_STANDARD,
            //authority: authorityService.defaultAuthority,
            //folder: folder,
            //branchName: newDataDictionary.branchName
        )
        DataSetParser.parseDataSet(xmlSlurper.parseText(testFileContents), dataSetDataModel, newDataDictionary)
        return dataSetDataModel
    }


    void ingestOtherDataSet() {

        expect:
            DataModel dm = parseDataSet(filename)
            dm.allDataElements.size() == elements

        where:
            filename | elements | classes
            testFiles[0] | 100 | 100

    }

    @Override
    void setupDomainData() {

    }

    @Override
    MessageSource getMessageSource() {
        return null
    }
}
