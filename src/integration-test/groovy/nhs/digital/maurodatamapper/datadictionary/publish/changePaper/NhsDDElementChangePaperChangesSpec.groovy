package nhs.digital.maurodatamapper.datadictionary.publish.changePaper

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDElement
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change

@Integration
@Rollback
class NhsDDElementChangePaperChangesSpec extends ChangePaperChangesSpec {
    @Override
    def getExpectedStereotype() {
        "Data Element"
    }

    void "should return changes for a new element with a format length"() {
        given: "there is no previous component"
        NhsDDElement previousComponent = null

        and: "there is a current component"
        NhsDDElement currentComponent = new NhsDDElement(
            name: "SERVICE REPORT ISSUE TIME",
            definition: "<a href=\"SERVICE REPORT ISSUE TIME\">SERVICE REPORT ISSUE TIME</a> is the same as attribute SERVICE REPORT ISSUE TIME.",
            otherProperties: [
                'formatLength': 'an8 HH:MM:SS'
            ])

        when: "finding the changes"
        List<Change> changes = currentComponent.getChanges(previousComponent)

        then: "the expected change object is returned"
        with {
            changes.size() == 2
        }

        Change formatChange = changes.first()
        String formatChangeDitaXml = formatChange.ditaDetail.toXmlString()

        verifyAll(formatChange) {
            changeType == FORMAT_LENGTH_TYPE
            stereotype == expectedStereotype
            !oldItem
            newItem == currentComponent
            preferDitaDetail
            htmlDetail == """<div class='format-length-detail'>
  <p>
    <b>Format / Length</b>
  </p>
  <p class='new'>an8 HH:MM:SS</p>
</div>"""
            formatChangeDitaXml == """<div>
  <p>
    <b>Format / Length</b>
  </p>
  <p outputclass='new'>an8 HH:MM:SS</p>
</div>"""
        }

        Change newChange = changes.last()
        verifyAll(newChange) {
            changeType == NEW_TYPE
            stereotype == expectedStereotype
            !oldItem
            newItem == currentComponent
        }
    }

    void "should return changes for an element with updated format length"() {
        given: "there is a previous component"
        NhsDDElement previousComponent = new NhsDDElement(
            name: "SERVICE REPORT ISSUE TIME",
            definition: "<a href=\"SERVICE REPORT ISSUE TIME\">SERVICE REPORT ISSUE TIME</a> is the same as attribute SERVICE REPORT ISSUE TIME.",
            otherProperties: [
                'formatLength': 'an8 HH:MM:SS'
            ])

        and: "there is a current component"
        NhsDDElement currentComponent = new NhsDDElement(
            name: "SERVICE REPORT ISSUE TIME",
            definition: "Please note: <a href=\"SERVICE REPORT ISSUE TIME\">SERVICE REPORT ISSUE TIME</a> is the same as attribute SERVICE REPORT ISSUE TIME.",
            otherProperties: [
                'formatLength': 'min an5 max an9'
            ])

        when: "finding the changes"
        List<Change> changes = currentComponent.getChanges(previousComponent)

        then: "the expected change object is returned"
        with {
            changes.size() == 2
        }

        Change formatChange = changes.first()
        String formatChangeDitaXml = formatChange.ditaDetail.toXmlString()

        verifyAll(formatChange) {
            changeType == FORMAT_LENGTH_TYPE
            stereotype == expectedStereotype
            oldItem == previousComponent
            newItem == currentComponent
            preferDitaDetail
            htmlDetail == """<div class='format-length-detail'>
  <p>
    <b>Format / Length</b>
  </p>
  <p class='new'>min an5 max an9</p>
  <p class='deleted'>an8 HH:MM:SS</p>
</div>"""
            formatChangeDitaXml == """<div>
  <p>
    <b>Format / Length</b>
  </p>
  <p outputclass='new'>min an5 max an9</p>
  <p outputclass='deleted'>an8 HH:MM:SS</p>
</div>"""
        }

        Change newChange = changes.last()
        verifyAll(newChange) {
            changeType == UPDATED_DESCRIPTION_TYPE
            stereotype == expectedStereotype
            oldItem == previousComponent
            newItem == currentComponent
        }
    }

    void "should return changes for a new element with linked attributes"() {
        given: "there is no previous component"
        NhsDDElement previousComponent = null

        and: "there is a current component"
        NhsDDElement currentComponent = new NhsDDElement(
            name: "ABLATIVE THERAPY TYPE",
            definition: "The type of <a href=\"ABLATIVE THERAPY TYPE\">ABLATIVE THERAPY TYPE</a>.")
        currentComponent.instantiatesAttributes.add(new NhsDDAttribute(name: "ABLATIVE THERAPY TYPE"))
        currentComponent.instantiatesAttributes.add(new NhsDDAttribute(name: "ACTIVITY OFFER DATE"))

        when: "finding the changes"
        List<Change> changes = currentComponent.getChanges(previousComponent)

        then: "the expected changes are returned"
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

        Change nationalCodesChange = changes.last()
        String ditaXml = nationalCodesChange.ditaDetail.toXmlString()
        verifyAll(nationalCodesChange) {
            changeType == CHANGED_ATTRIBUTES_TYPE
            stereotype == expectedStereotype
            !oldItem
            newItem == currentComponent
            htmlDetail == """<div>
  <p>
    <b>Attributes</b>
  </p>
  <ul>
    <li class='new'>ABLATIVE THERAPY TYPE</li>
    <li class='new'>ACTIVITY OFFER DATE</li>
  </ul>
</div>"""
            ditaXml == """<div>
  <div>
    <p>
      <b>Attributes</b>
    </p>
    <ul>
      <li outputclass='new'>ABLATIVE THERAPY TYPE</li>
      <li outputclass='new'>ACTIVITY OFFER DATE</li>
    </ul>
  </div>
</div>"""
        }
    }

    void "should return changes for an attribute with updated linked elements"() {
        given: "there is a previous component"
        NhsDDElement previousComponent = new NhsDDElement(
            name: "ABLATIVE THERAPY TYPE",
            definition: "The type of <a href=\"ABLATIVE THERAPY TYPE\">ABLATIVE THERAPY TYPE</a>.")
        previousComponent.instantiatesAttributes.add(new NhsDDAttribute(name: "ABLATIVE THERAPY TYPE"))
        previousComponent.instantiatesAttributes.add(new NhsDDAttribute(name: "ACTIVITY OFFER DATE"))

        and: "there is a current component"
        NhsDDElement currentComponent = new NhsDDElement(
            name: "ABLATIVE THERAPY TYPE",
            definition: "The form of <a href=\"ABLATIVE THERAPY TYPE\">ABLATIVE THERAPY TYPE</a>.")
        currentComponent.instantiatesAttributes.add(new NhsDDAttribute(name: "ABLATIVE THERAPY TYPE"))    // Unchanged
        currentComponent.instantiatesAttributes.add(new NhsDDAttribute(name: "ACTIVITY UNIT PRICE"))      // New

        when: "finding the changes"
        List<Change> changes = currentComponent.getChanges(previousComponent)

        then: "the expected changes are returned"
        with {
            changes.size() == 2
        }

        Change updatedChange = changes.first()
        verifyAll(updatedChange) {
            changeType == UPDATED_DESCRIPTION_TYPE
            stereotype == expectedStereotype
            oldItem == previousComponent
            newItem == currentComponent
        }

        Change nationalCodesChange = changes.last()
        String ditaXml = nationalCodesChange.ditaDetail.toXmlString()
        verifyAll(nationalCodesChange) {
            changeType == CHANGED_ATTRIBUTES_TYPE
            stereotype == expectedStereotype
            oldItem == previousComponent
            newItem == currentComponent
            htmlDetail == """<div>
  <p>
    <b>Attributes</b>
  </p>
  <ul>
    <li>ABLATIVE THERAPY TYPE</li>
    <li class='new'>ACTIVITY UNIT PRICE</li>
    <li class='deleted'>ACTIVITY OFFER DATE</li>
  </ul>
</div>"""
            ditaXml == """<div>
  <div>
    <p>
      <b>Attributes</b>
    </p>
    <ul>
      <li>ABLATIVE THERAPY TYPE</li>
      <li outputclass='new'>ACTIVITY UNIT PRICE</li>
      <li outputclass='deleted'>ACTIVITY OFFER DATE</li>
    </ul>
  </div>
</div>"""
        }
    }
}