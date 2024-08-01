package nhs.digital.maurodatamapper.datadictionary

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change

class ChangePaperChangesSpec extends Specification {
}

@Integration
@Rollback
class NhsDDAttributeChangePaperChangesSpec extends ChangePaperChangesSpec  {
    void "should return changes for a new component"() {
        given: "there is no previous component"
        NhsDDAttribute previousComponent = null

        and: "there is a current component"
        NhsDDAttribute currentComponent = new NhsDDAttribute(
            name: "DIAGNOSTIC TEST REQUEST RECEIVED TIME",
            definition: "The time the <a href=\"DIAGNOSTIC TEST REQUEST\">DIAGNOSTIC TEST REQUEST</a> was received.")

        when: "finding the changes"
        List<Change> changes = currentComponent.getChanges(previousComponent)

        then: "the expected change object is returned"
        Change change = changes.first()
        with {
            change
        }

        verifyAll(change) {
            changeType == NEW_TYPE
            stereotype == "Attribute"
            !oldItem
            newItem == currentComponent
            htmlDetail == "<div>\n" +
            "  <div class='new'>The time the <a href=\"DIAGNOSTIC TEST REQUEST\">DIAGNOSTIC TEST REQUEST</a> was received.</div>\n" +
            "</div>"
            ditaDetail.toXmlString() == "<div outputclass='new'>\n" +
            "  <div>The time the \n" +
            "\n" +
            "    <xref keyref='DIAGNOSTIC%20TEST%20REQUEST' scope='local'>DIAGNOSTIC TEST REQUEST</xref> was received.\n" +
            "\n" +
            "  </div>\n" +
            "</div>"
        }
    }

    void "should return changes for an updated description"() {
        given: "there is a previous component"
        NhsDDAttribute previousComponent = new NhsDDAttribute(
            name: "DIAGNOSTIC TEST REQUEST RECEIVED TIME",
            definition: "The time the <a href=\"DIAGNOSTIC TEST REQUEST\">DIAGNOSTIC TEST REQUEST</a> was received.")

        and: "there is a current component"
        NhsDDAttribute currentComponent = new NhsDDAttribute(
            name: "DIAGNOSTIC TEST REQUEST RECEIVED TIME",
            definition: "The time the <a href=\"DIAGNOSTIC TEST REQUEST\">DIAGNOSTIC TEST REQUEST</a> was obtained.")

        when: "finding the changes"
        List<Change> changes = currentComponent.getChanges(previousComponent)

        then: "the expected change object is returned"
        Change change = changes.first()
        with {
            change
        }

        String ditaXml = change.ditaDetail.toXmlString()

        verifyAll(change) {
            changeType == UPDATED_DESCRIPTION_TYPE
            stereotype == "Attribute"
            oldItem == previousComponent
            newItem == currentComponent
            htmlDetail == "<div>\n" +
            "  <div>The time the <a href=\"DIAGNOSTIC TEST REQUEST\">DIAGNOSTIC TEST REQUEST</a> was <span class=\"diff-html-removed\" id=\"removed-diff-0\" previous=\"first-diff\" changeId=\"removed-diff-0\" next=\"added-diff-0\">received</span><span class=\"diff-html-added\" id=\"added-diff-0\" previous=\"removed-diff-0\" changeId=\"added-diff-0\" next=\"last-diff\">obtained</span>.</div>\n" +
            "</div>"
            ditaXml == """<div>
  <div outputclass='deleted'>
    <div>The time the 

      <xref keyref='DIAGNOSTIC%20TEST%20REQUEST' scope='local'>DIAGNOSTIC TEST REQUEST</xref> was received.

    </div>
  </div>
  <div outputclass='new'>
    <div>The time the 

      <xref keyref='DIAGNOSTIC%20TEST%20REQUEST' scope='local'>DIAGNOSTIC TEST REQUEST</xref> was obtained.

    </div>
  </div>
</div>"""
        }
    }

    void "should return changes for a retired item"() {
        given: "there is a previous component"
        NhsDDAttribute previousComponent = new NhsDDAttribute(
            name: "DIAGNOSTIC TEST REQUEST RECEIVED TIME",
            definition: "The time the <a href=\"DIAGNOSTIC TEST REQUEST\">DIAGNOSTIC TEST REQUEST</a> was received.",)

        and: "there is a current component"
        NhsDDAttribute currentComponent = new NhsDDAttribute(
            name: "DIAGNOSTIC TEST REQUEST RECEIVED TIME",
            definition: "This item has been retired from the NHS Data Model and Dictionary.",
            otherProperties: ["isRetired": "true"])

        when: "finding the changes"
        List<Change> changes = currentComponent.getChanges(previousComponent)

        then: "the expected change object is returned"
        Change change = changes.first()
        with {
            change
        }

        verifyAll(change) {
            changeType == RETIRED_TYPE
            stereotype == "Attribute"
            oldItem == previousComponent
            newItem == currentComponent
            htmlDetail == "<div><span class=\"diff-html-removed\" id=\"removed-diff-0\" previous=\"first-diff\" changeId=\"removed-diff-0\" next=\"added-diff-0\">The time the </span><a href=\"DIAGNOSTIC TEST REQUEST\"><span class=\"diff-html-removed\" previous=\"first-diff\" changeId=\"removed-diff-0\" next=\"added-diff-0\">DIAGNOSTIC TEST REQUEST</span></a><span class=\"diff-html-removed\" previous=\"first-diff\" changeId=\"removed-diff-0\" next=\"added-diff-0\"> was received</span><span class=\"diff-html-added\" id=\"added-diff-0\" previous=\"removed-diff-0\" changeId=\"added-diff-0\" next=\"last-diff\">This item has been retired from the NHS Data Model and Dictionary</span>.</div>"
        }
    }
}

@Integration
@Rollback
class NhsDDClassChangePaperChangesSpec extends ChangePaperChangesSpec  {
    void "should return changes for changed attributes in a class"() {
        given: "there is a previous component"
        NhsDDClass previousComponent = new NhsDDClass(name: "SERVICE REPORT")
        previousComponent.keyAttributes.add(new NhsDDAttribute(name: "SERVICE REPORT IDENTIFIER", otherProperties: ["isKey": "true"]))
        previousComponent.otherAttributes.add(new NhsDDAttribute(name: "SERVICE REPORT ISSUE DATE"))
        previousComponent.otherAttributes.add(new NhsDDAttribute(name: "SERVICE REPORT STATUS"))
        previousComponent.otherAttributes.add(new NhsDDAttribute(name: "SERVICE REPORT STATUS CODE"))

        and: "there is a current component"
        NhsDDClass currentComponent = new NhsDDClass(name: "SERVICE REPORT")
        currentComponent.keyAttributes.add(new NhsDDAttribute(name: "SERVICE REPORT IDENTIFIER", otherProperties: ["isKey": "true"]))
        currentComponent.otherAttributes.add(new NhsDDAttribute(name: "SERVICE REPORT ISSUE DATE"))
        currentComponent.otherAttributes.add(new NhsDDAttribute(name: "SERVICE REPORT ISSUE TIME"))
        currentComponent.otherAttributes.add(new NhsDDAttribute(name: "SERVICE REPORT STATUS"))

        when: "finding the changes"
        List<Change> changes = currentComponent.getChanges(previousComponent)

        then: "the expected change object is returned"
        Change change = changes.first()
        with {
            change
        }

        String ditaXml = change.ditaDetail.toXmlString()

        verifyAll(change) {
            changeType == CHANGED_ATTRIBUTES_TYPE
            stereotype == "Class"
            oldItem == previousComponent
            newItem == currentComponent
            htmlDetail == """<div>
  <p>Attributes of this Class are:</p>
  <div>
    <span class='attribute-name'>SERVICE REPORT IDENTIFIER</span>
    <span class='attribute-key'>Key</span>
  </div>
  <div>
    <span class='attribute-name'>SERVICE REPORT ISSUE DATE</span>
  </div>
  <div class='new'>
    <span class='attribute-name'>SERVICE REPORT ISSUE TIME</span>
  </div>
  <div>
    <span class='attribute-name'>SERVICE REPORT STATUS</span>
  </div>
  <div class='deleted'>
    <span class='attribute-name'>SERVICE REPORT STATUS CODE</span>
  </div>
</div>"""
            ditaXml == """<div>
  <div>
    <p>Attributes of this Class are:</p>
    <div>
      <ph outputclass='attribute-name'>SERVICE REPORT IDENTIFIER</ph>
      <ph outputclass='attribute-key'>Key</ph>
    </div>
    <div>
      <ph outputclass='attribute-name'>SERVICE REPORT ISSUE DATE</ph>
    </div>
    <div outputclass='new'>
      <ph outputclass='attribute-name'>SERVICE REPORT ISSUE TIME</ph>
    </div>
    <div>
      <ph outputclass='attribute-name'>SERVICE REPORT STATUS</ph>
    </div>
    <div outputclass='deleted'>
      <ph outputclass='attribute-name'>SERVICE REPORT STATUS CODE</ph>
    </div>
  </div>
</div>"""
        }
    }
}