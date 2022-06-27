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
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.CDSDataSetParser
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.DataSetParser
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.OtherDataSetParser
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary


@Slf4j
@Integration
@Rollback
class DataSetParserSpec extends BaseIntegrationSpec {

    static XmlSlurper xmlSlurper = new XmlSlurper()

    static List<String> testFiles = [
        "CDS V6-3 Type 180 - Admitted Patient Care - Unfinished Birth Episode CDS",
        "Inter-provider Transfer Administrative Minimum Data Set",
    ]

    DataModel parseDataSet(String filename) {
        log.debug('Ingesting {}', filename)

        String testFileContents = getClass().classLoader.getResourceAsStream ("testDataSets/" + filename + ".xml").text

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

        if (filename.startsWith("CDS")) {
            CDSDataSetParser.parseCDSDataSet(xmlSlurper.parseText(testFileContents), dataSetDataModel, newDataDictionary)
        } else {
            DataSetParser.parseDataSet(xmlSlurper.parseText(testFileContents), dataSetDataModel, newDataDictionary)
        }
        return dataSetDataModel
    }


    void ingestOtherDataSet() {

        expect:
            DataModel dm = parseDataSet(filename)
            System.err.println(dm.childDataClasses)
            dm.allDataElements.size() == elements

        where:
            filename << testFiles
            elements << [36, 100]

    }

    @Override
    void setupDomainData() {

    }

    @Override
    MessageSource getMessageSource() {
        return null
    }
}
