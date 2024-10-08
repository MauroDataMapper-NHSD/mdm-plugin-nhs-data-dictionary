package nhs.digital.maurodatamapper.datadictionary.publish.changePaper

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSet
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetClass
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetElement
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDElement
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change

@Integration
@Rollback
class NhsDDDataSetChangePaperChangesSpec extends ChangePaperChangesSpec {
    @Override
    def getExpectedStereotype() {
        "Data Set"
    }

    static NhsDDDataSetElement createDataSetElement(String name, String mandation) {
        new NhsDDDataSetElement(
            name: name,
            mandation: mandation,
            reuseElement: new NhsDDElement(name: name)
        )
    }

    void "should return changes for a new data set"() {
        given: "there is no previous component"
        NhsDDDataSet previousComponent = null

        and: "there is a current component"
        NhsDDDataSet currentComponent = new NhsDDDataSet(
            name: "Diagnostic Imaging Data Set",
            definition: "This is a data set for <a href=\"Diagnostic Imaging Data Set\">Diagnostic Imaging</a>.",
            dataSetClasses: [
                new NhsDDDataSetClass(
                    name: "PERSONAL AND DEMOGRAPHIC",
                    description: "at least one of these data items must be present.",
                    dataSetElements: [
                        createDataSetElement("NHS NUMBER", "R"),
                        createDataSetElement("NHS NUMBER STATUS INDICATOR CODE", "R"),
                        createDataSetElement("PERSON BIRTH DATE", "R"),
                    ]
                ),
                new NhsDDDataSetClass(
                    name: "REFERRALS",
                    description: "One occurrence of this group is required.",
                    dataSetElements: [
                        createDataSetElement("PATIENT SOURCE SETTING TYPE (DIAGNOSTIC IMAGING)", "M"),
                        createDataSetElement("REFERRER CODE", "R"),
                        createDataSetElement("DIAGNOSTIC TEST REQUEST DATE", "R"),
                    ]
                )
            ])

        when: "finding the changes"
        List<Change> changes = currentComponent.getChanges(previousComponent)

        then: "the expected change object is returned"
        with {
            changes.size() == 2
        }

        Change newChange = changes.first()
        verifyAll(newChange) {
            changeType == NEW_TYPE
            stereotype == expectedStereotype
            !oldItem
            newItem == currentComponent
        }

        Change specificationChange = changes.last()
        String specificationDitaXml = specificationChange.ditaDetail.toXmlString()

        verifyAll(specificationChange) {
            changeType == SPECIFICATION_TYPE
            stereotype == expectedStereotype
            !oldItem
            newItem == currentComponent
            preferDitaDetail
            htmlDetail == "<table class=\"simpletable table table-sm\"><thead><tr><th colspan=\"2\" class=\"thead-light\" text-align=\"center\"><b>PERSONAL AND DEMOGRAPHIC</b><p>at least one of these data items must be present.</p></th></tr></thead><tbody><tr><td width=\"20%\" class=\"mandation-header\"><b>Mandation</b></td><td width=\"80%\" class=\"elements-header\"><b>Data Elements</b></td></tr><tr><td width=\"20%\" class=\"mandation\"><p>R</p></td><td width=\"80%\"><a href=\"dm:Data Elements|dc:N|de:NHS NUMBER\">NHS NUMBER</a></td></tr><tr><td width=\"20%\" class=\"mandation\"><p>R</p></td><td width=\"80%\"><a href=\"dm:Data Elements|dc:N|de:NHS NUMBER STATUS INDICATOR CODE\">NHS NUMBER STATUS INDICATOR CODE</a></td></tr><tr><td width=\"20%\" class=\"mandation\"><p>R</p></td><td width=\"80%\"><a href=\"dm:Data Elements|dc:P|de:PERSON BIRTH DATE\">PERSON BIRTH DATE</a></td></tr></tbody></table><table class=\"simpletable table table-sm\"><thead><tr><th colspan=\"2\" class=\"thead-light\" text-align=\"center\"><b>REFERRALS</b><p>One occurrence of this group is required.</p></th></tr></thead><tbody><tr><td width=\"20%\" class=\"mandation-header\"><b>Mandation</b></td><td width=\"80%\" class=\"elements-header\"><b>Data Elements</b></td></tr><tr><td width=\"20%\" class=\"mandation\"><p>M</p></td><td width=\"80%\"><a href=\"dm:Data Elements|dc:P|de:PATIENT SOURCE SETTING TYPE (DIAGNOSTIC IMAGING)\">PATIENT SOURCE SETTING TYPE (DIAGNOSTIC IMAGING)</a></td></tr><tr><td width=\"20%\" class=\"mandation\"><p>R</p></td><td width=\"80%\"><a href=\"dm:Data Elements|dc:R|de:REFERRER CODE\">REFERRER CODE</a></td></tr><tr><td width=\"20%\" class=\"mandation\"><p>R</p></td><td width=\"80%\"><a href=\"dm:Data Elements|dc:D|de:DIAGNOSTIC TEST REQUEST DATE\">DIAGNOSTIC TEST REQUEST DATE</a></td></tr></tbody></table>"
            // Uncomment the below to test locally, doesn't work on Jenkins
            //            specificationDitaXml == """<div>
            //  <div>
            //    <table outputclass='table table-striped table-bordered table-sm'>
            //      <tgroup cols='2'>
            //        <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
            //        <colspec colnum='2' colname='col2' colwidth='8*' />
            //        <thead>
            //          <row outputclass='thead-light'>
            //            <entry namest='col1' nameend='col2'>
            //              <b>PERSONAL AND DEMOGRAPHIC</b>
            //              <p>at least one of these data items must be present.</p>
            //            </entry>
            //          </row>
            //          <row>
            //            <entry>
            //              <p>Mandation</p>
            //            </entry>
            //            <entry>
            //              <p>Data Elements</p>
            //            </entry>
            //          </row>
            //        </thead>
            //        <tbody>
            //          <row>
            //            <entry>
            //              <p>R</p>
            //            </entry>
            //            <entry>
            //              <p>
            //                <xref outputclass='element' keyref='data_element_nhs_number' scope='local' />
            //              </p>
            //            </entry>
            //          </row>
            //          <row>
            //            <entry>
            //              <p>R</p>
            //            </entry>
            //            <entry>
            //              <p>
            //                <xref outputclass='element' keyref='data_element_nhs_number_status_indicator_code' scope='local' />
            //              </p>
            //            </entry>
            //          </row>
            //          <row>
            //            <entry>
            //              <p>R</p>
            //            </entry>
            //            <entry>
            //              <p>
            //                <xref outputclass='element' keyref='data_element_person_birth_date' scope='local' />
            //              </p>
            //            </entry>
            //          </row>
            //        </tbody>
            //      </tgroup>
            //    </table>
            //  </div>
            //  <div>
            //    <table outputclass='table table-striped table-bordered table-sm'>
            //      <tgroup cols='2'>
            //        <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
            //        <colspec colnum='2' colname='col2' colwidth='8*' />
            //        <thead>
            //          <row outputclass='thead-light'>
            //            <entry namest='col1' nameend='col2'>
            //              <b>REFERRALS</b>
            //              <p>One occurrence of this group is required.</p>
            //            </entry>
            //          </row>
            //          <row>
            //            <entry>
            //              <p>Mandation</p>
            //            </entry>
            //            <entry>
            //              <p>Data Elements</p>
            //            </entry>
            //          </row>
            //        </thead>
            //        <tbody>
            //          <row>
            //            <entry>
            //              <p>M</p>
            //            </entry>
            //            <entry>
            //              <p>
            //                <xref outputclass='element' keyref='data_element_patient_source_setting_type_diagnostic_imaging' scope='local' />
            //              </p>
            //            </entry>
            //          </row>
            //          <row>
            //            <entry>
            //              <p>R</p>
            //            </entry>
            //            <entry>
            //              <p>
            //                <xref outputclass='element' keyref='data_element_referrer_code' scope='local' />
            //              </p>
            //            </entry>
            //          </row>
            //          <row>
            //            <entry>
            //              <p>R</p>
            //            </entry>
            //            <entry>
            //              <p>
            //                <xref outputclass='element' keyref='data_element_diagnostic_test_request_date' scope='local' />
            //              </p>
            //            </entry>
            //          </row>
            //        </tbody>
            //      </tgroup>
            //    </table>
            //  </div>
            //</div>"""
        }
    }

    void "should return changes for an updated data set"() {
        given: "there is a previous component"
        NhsDDDataSet previousComponent = new NhsDDDataSet(
            name: "Diagnostic Imaging Data Set",
            definition: "This is a data set for <a href=\"Diagnostic Imaging Data Set\">Diagnostic Imaging</a>.",
            dataSetClasses: [
                new NhsDDDataSetClass(
                    name: "PERSONAL AND DEMOGRAPHIC",
                    description: "at least one of these data items must be present.",
                    dataSetElements: [
                        createDataSetElement("NHS NUMBER", "R"),
                        createDataSetElement("NHS NUMBER STATUS INDICATOR CODE", "R"),
                        createDataSetElement("PERSON BIRTH DATE", "R"),
                    ]
                ),
                new NhsDDDataSetClass(
                    name: "REFERRALS",
                    description: "One occurrence of this group is required.",
                    dataSetElements: [
                        createDataSetElement("PATIENT SOURCE SETTING TYPE (DIAGNOSTIC IMAGING)", "M"),
                        createDataSetElement("REFERRER CODE", "R"),
                        createDataSetElement("DIAGNOSTIC TEST REQUEST DATE", "R"),
                    ]
                )
            ])

        and: "there is a current component"
        NhsDDDataSet currentComponent = new NhsDDDataSet(
            name: "Diagnostic Imaging Data Set",
            definition: "This is an updated data set for <a href=\"Diagnostic Imaging Data Set\">Diagnostic Imaging</a>.",
            dataSetClasses: [
                new NhsDDDataSetClass(
                    name: "PERSONAL AND DEMOGRAPHIC",
                    description: "at least one of these data items must be present.",
                    dataSetElements: [
                        createDataSetElement("NHS NUMBER", "R"),
                        createDataSetElement("NHS NUMBER STATUS INDICATOR CODE", "R"),
                        createDataSetElement("PERSON BIRTH DATE", "R"),
                        createDataSetElement("ETHNIC CATEGORY 2021", "P"),
                    ]
                ),
                new NhsDDDataSetClass(
                    name: "REFERRALS",
                    description: "One occurrence of this group is required.",
                    dataSetElements: [
                        createDataSetElement("PATIENT SOURCE SETTING TYPE (DIAGNOSTIC IMAGING)", "M"),
                        createDataSetElement("REFERRER CODE", "R"),
                        createDataSetElement("DIAGNOSTIC TEST REQUEST DATE", "R"),
                        createDataSetElement("DIAGNOSTIC TEST REQUEST TIME", "R"),
                    ]
                )
            ])

        when: "finding the changes"
        List<Change> changes = currentComponent.getChanges(previousComponent)

        then: "the expected change object is returned"
        with {
            changes.size() == 2
        }

        Change newChange = changes.first()
        verifyAll(newChange) {
            changeType == UPDATED_DESCRIPTION_TYPE
            stereotype == expectedStereotype
            oldItem == previousComponent
            newItem == currentComponent
        }

        Change specificationChange = changes.last()
        String specificationDitaXml = specificationChange.ditaDetail.toXmlString()

        verifyAll(specificationChange) {
            changeType == SPECIFICATION_TYPE
            stereotype == expectedStereotype
            oldItem == previousComponent
            newItem == currentComponent
            preferDitaDetail
            htmlDetail == "<table class=\"simpletable table table-sm\"><thead><tr><th colspan=\"2\" class=\"thead-light\" text-align=\"center\"><b>PERSONAL AND DEMOGRAPHIC</b><p>at least one of these data items must be present.</p></th></tr></thead><tbody><tr><td width=\"20%\" class=\"mandation-header\"><b>Mandation</b></td><td width=\"80%\" class=\"elements-header\"><b>Data Elements</b></td></tr><tr><td width=\"20%\" class=\"mandation\"><p>R</p></td><td width=\"80%\"><a href=\"dm:Data Elements|dc:N|de:NHS NUMBER\">NHS NUMBER</a></td></tr><tr><td width=\"20%\" class=\"mandation\"><p>R</p></td><td width=\"80%\"><a href=\"dm:Data Elements|dc:N|de:NHS NUMBER STATUS INDICATOR CODE\">NHS NUMBER STATUS INDICATOR CODE</a></td></tr><tr><td width=\"20%\" class=\"mandation\"><p>R</p></td><td width=\"80%\"><a href=\"dm:Data Elements|dc:P|de:PERSON BIRTH DATE\">PERSON BIRTH DATE</a></td></tr><tr><td width=\"20%\" class=\"mandation\"><p>P</p></td><td width=\"80%\"><a href=\"dm:Data Elements|dc:E|de:ETHNIC CATEGORY 2021\">ETHNIC CATEGORY 2021</a></td></tr></tbody></table><table class=\"simpletable table table-sm\"><thead><tr><th colspan=\"2\" class=\"thead-light\" text-align=\"center\"><b>REFERRALS</b><p>One occurrence of this group is required.</p></th></tr></thead><tbody><tr><td width=\"20%\" class=\"mandation-header\"><b>Mandation</b></td><td width=\"80%\" class=\"elements-header\"><b>Data Elements</b></td></tr><tr><td width=\"20%\" class=\"mandation\"><p>M</p></td><td width=\"80%\"><a href=\"dm:Data Elements|dc:P|de:PATIENT SOURCE SETTING TYPE (DIAGNOSTIC IMAGING)\">PATIENT SOURCE SETTING TYPE (DIAGNOSTIC IMAGING)</a></td></tr><tr><td width=\"20%\" class=\"mandation\"><p>R</p></td><td width=\"80%\"><a href=\"dm:Data Elements|dc:R|de:REFERRER CODE\">REFERRER CODE</a></td></tr><tr><td width=\"20%\" class=\"mandation\"><p>R</p></td><td width=\"80%\"><a href=\"dm:Data Elements|dc:D|de:DIAGNOSTIC TEST REQUEST DATE\">DIAGNOSTIC TEST REQUEST DATE</a></td></tr><tr><td width=\"20%\" class=\"mandation\"><p>R</p></td><td width=\"80%\"><a href=\"dm:Data Elements|dc:D|de:DIAGNOSTIC TEST REQUEST TIME\">DIAGNOSTIC TEST REQUEST TIME</a></td></tr></tbody></table>"
            // Uncomment the below to test locally, doesn't work on Jenkins
            //            specificationDitaXml == """<div>
            //  <div>
            //    <table outputclass='table table-striped table-bordered table-sm'>
            //      <tgroup cols='2'>
            //        <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
            //        <colspec colnum='2' colname='col2' colwidth='8*' />
            //        <thead>
            //          <row outputclass='thead-light'>
            //            <entry namest='col1' nameend='col2'>
            //              <b>PERSONAL AND DEMOGRAPHIC</b>
            //              <p>at least one of these data items must be present.</p>
            //            </entry>
            //          </row>
            //          <row>
            //            <entry>
            //              <p>Mandation</p>
            //            </entry>
            //            <entry>
            //              <p>Data Elements</p>
            //            </entry>
            //          </row>
            //        </thead>
            //        <tbody>
            //          <row>
            //            <entry>
            //              <p>R</p>
            //            </entry>
            //            <entry>
            //              <p>
            //                <xref outputclass='element' keyref='data_element_nhs_number' scope='local' />
            //              </p>
            //            </entry>
            //          </row>
            //          <row>
            //            <entry>
            //              <p>R</p>
            //            </entry>
            //            <entry>
            //              <p>
            //                <xref outputclass='element' keyref='data_element_nhs_number_status_indicator_code' scope='local' />
            //              </p>
            //            </entry>
            //          </row>
            //          <row>
            //            <entry>
            //              <p>R</p>
            //            </entry>
            //            <entry>
            //              <p>
            //                <xref outputclass='element' keyref='data_element_person_birth_date' scope='local' />
            //              </p>
            //            </entry>
            //          </row>
            //          <row>
            //            <entry>
            //              <p>P</p>
            //            </entry>
            //            <entry>
            //              <p>
            //                <xref outputclass='element' keyref='data_element_ethnic_category_2021' scope='local' />
            //              </p>
            //            </entry>
            //          </row>
            //        </tbody>
            //      </tgroup>
            //    </table>
            //  </div>
            //  <div>
            //    <table outputclass='table table-striped table-bordered table-sm'>
            //      <tgroup cols='2'>
            //        <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
            //        <colspec colnum='2' colname='col2' colwidth='8*' />
            //        <thead>
            //          <row outputclass='thead-light'>
            //            <entry namest='col1' nameend='col2'>
            //              <b>REFERRALS</b>
            //              <p>One occurrence of this group is required.</p>
            //            </entry>
            //          </row>
            //          <row>
            //            <entry>
            //              <p>Mandation</p>
            //            </entry>
            //            <entry>
            //              <p>Data Elements</p>
            //            </entry>
            //          </row>
            //        </thead>
            //        <tbody>
            //          <row>
            //            <entry>
            //              <p>M</p>
            //            </entry>
            //            <entry>
            //              <p>
            //                <xref outputclass='element' keyref='data_element_patient_source_setting_type_diagnostic_imaging' scope='local' />
            //              </p>
            //            </entry>
            //          </row>
            //          <row>
            //            <entry>
            //              <p>R</p>
            //            </entry>
            //            <entry>
            //              <p>
            //                <xref outputclass='element' keyref='data_element_referrer_code' scope='local' />
            //              </p>
            //            </entry>
            //          </row>
            //          <row>
            //            <entry>
            //              <p>R</p>
            //            </entry>
            //            <entry>
            //              <p>
            //                <xref outputclass='element' keyref='data_element_diagnostic_test_request_date' scope='local' />
            //              </p>
            //            </entry>
            //          </row>
            //          <row>
            //            <entry>
            //              <p>R</p>
            //            </entry>
            //            <entry>
            //              <p>
            //                <xref outputclass='element' keyref='data_element_diagnostic_test_request_time' scope='local' />
            //              </p>
            //            </entry>
            //          </row>
            //        </tbody>
            //      </tgroup>
            //    </table>
            //  </div>
            //</div>"""
        }
    }
}