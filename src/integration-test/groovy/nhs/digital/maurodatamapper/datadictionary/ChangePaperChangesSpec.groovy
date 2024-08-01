package nhs.digital.maurodatamapper.datadictionary

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change

abstract class ChangePaperChangesSpec extends Specification {
    abstract getExpectedStereotype()
}

@Integration
@Rollback
class NhsDDAttributeChangePaperChangesSpec extends ChangePaperChangesSpec  {
    @Override
    def getExpectedStereotype() {
        "Attribute"
    }

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
            stereotype == expectedStereotype
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
            stereotype == expectedStereotype
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
            stereotype == expectedStereotype
            oldItem == previousComponent
            newItem == currentComponent
            htmlDetail == "<div><span class=\"diff-html-removed\" id=\"removed-diff-0\" previous=\"first-diff\" changeId=\"removed-diff-0\" next=\"added-diff-0\">The time the </span><a href=\"DIAGNOSTIC TEST REQUEST\"><span class=\"diff-html-removed\" previous=\"first-diff\" changeId=\"removed-diff-0\" next=\"added-diff-0\">DIAGNOSTIC TEST REQUEST</span></a><span class=\"diff-html-removed\" previous=\"first-diff\" changeId=\"removed-diff-0\" next=\"added-diff-0\"> was received</span><span class=\"diff-html-added\" id=\"added-diff-0\" previous=\"removed-diff-0\" changeId=\"added-diff-0\" next=\"last-diff\">This item has been retired from the NHS Data Model and Dictionary</span>.</div>"
        }
    }

    void "should return changes for a new attribute with aliases"() {
        given: "there is no previous component"
        NhsDDAttribute previousComponent = null

        and: "there is a current component"
        NhsDDAttribute currentComponent = new NhsDDAttribute(
            name: "DIAGNOSTIC TEST REQUEST RECEIVED TIME",
            definition: "The time the <a href=\"DIAGNOSTIC TEST REQUEST\">DIAGNOSTIC TEST REQUEST</a> was received.",
            otherProperties: [
                'aliasAlsoKnownAs': 'DIAGNOSTIC TEST REQUEST RECEIVED TIMESTAMP',
                'aliasPlural': 'DIAGNOSTIC TEST REQUEST RECEIVED TIMES'
            ])

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

        Change aliasesChange = changes.last()
        String aliasDitaXml = aliasesChange.ditaDetail.toXmlString()
        verifyAll(aliasesChange) {
            changeType == ALIASES_TYPE
            stereotype == expectedStereotype
            !oldItem
            newItem == currentComponent
            htmlDetail == """<div>
  <p>This Attribute is also known by these names:</p>
  <table class='alias-table'>
    <thead>
      <th>Context</th>
      <th>Alias</th>
    </thead>
    <tbody>
      <tr>
        <td class='new'>Also known as</td>
        <td class='new'>DIAGNOSTIC TEST REQUEST RECEIVED TIMESTAMP</td>
      </tr>
      <tr>
        <td class='new'>Plural</td>
        <td class='new'>DIAGNOSTIC TEST REQUEST RECEIVED TIMES</td>
      </tr>
    </tbody>
  </table>
</div>"""
            aliasDitaXml == """<div>
  <p>This Attribute is also known by these names:</p>
  <simpletable relcolwidth='1* 2*'>
    <sthead>
      <stentry>Context</stentry>
      <stentry>Alias</stentry>
    </sthead>
    <strow>
      <stentry outputclass='new'>
        <ph>Also known as</ph>
      </stentry>
      <stentry outputclass='new'>
        <ph>DIAGNOSTIC TEST REQUEST RECEIVED TIMESTAMP</ph>
      </stentry>
    </strow>
    <strow>
      <stentry outputclass='new'>
        <ph>Plural</ph>
      </stentry>
      <stentry outputclass='new'>
        <ph>DIAGNOSTIC TEST REQUEST RECEIVED TIMES</ph>
      </stentry>
    </strow>
  </simpletable>
</div>"""
        }
    }

    void "should return changes for an attribute with updated aliases"() {
        given: "there is a previous component"
        NhsDDAttribute previousComponent = new NhsDDAttribute(
            name: "DIAGNOSTIC TEST REQUEST RECEIVED TIME",
            definition: "The time the <a href=\"DIAGNOSTIC TEST REQUEST\">DIAGNOSTIC TEST REQUEST</a> was received.",
            otherProperties: [
                'aliasAlsoKnownAs': 'DIAGNOSTIC TEST REQUEST RECEIVED TIMESTAMP',   // Change
                'aliasFormerly': 'DIAGNOSTIC ASSESSMENT REQUEST RECEIVED TIMES',    // Remove
                'aliasPlural': 'DIAGNOSTIC TEST REQUEST RECEIVED TIMES'             // Unchanged
            ])

        and: "there is a current component"
        NhsDDAttribute currentComponent = new NhsDDAttribute(
            name: "DIAGNOSTIC TEST REQUEST RECEIVED TIME",
            definition: "The time the <a href=\"DIAGNOSTIC TEST REQUEST\">DIAGNOSTIC TEST REQUEST</a> was accepted.",
            otherProperties: [
                'aliasAlsoKnownAs': 'DIAGNOSTIC TEST REQUEST RECEIVED TIME TRACKING',   // Change
                'aliasPlural': 'DIAGNOSTIC TEST REQUEST RECEIVED TIMES',                // Unchanged
                'aliasSchema': 'DIAGNOSTIC SCHEMA'                                      // New
            ])

        when: "finding the changes"
        List<Change> changes = currentComponent.getChanges(previousComponent)

        then: "the expected changes are returned"
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

        Change aliasesChange = changes.last()
        String aliasDitaXml = aliasesChange.ditaDetail.toXmlString()
        verifyAll(aliasesChange) {
            changeType == ALIASES_TYPE
            stereotype == expectedStereotype
            oldItem == previousComponent
            newItem == currentComponent
            htmlDetail == """<div>
  <p>This Attribute is also known by these names:</p>
  <table class='alias-table'>
    <thead>
      <th>Context</th>
      <th>Alias</th>
    </thead>
    <tbody>
      <tr>
        <td class='new'>Also known as</td>
        <td class='new'>DIAGNOSTIC TEST REQUEST RECEIVED TIME TRACKING</td>
      </tr>
      <tr>
        <td>Plural</td>
        <td>DIAGNOSTIC TEST REQUEST RECEIVED TIMES</td>
      </tr>
      <tr>
        <td class='new'>Schema</td>
        <td class='new'>DIAGNOSTIC SCHEMA</td>
      </tr>
      <tr>
        <td class='deleted'>Also known as</td>
        <td class='deleted'>DIAGNOSTIC TEST REQUEST RECEIVED TIMESTAMP</td>
      </tr>
      <tr>
        <td class='deleted'>Formerly</td>
        <td class='deleted'>DIAGNOSTIC ASSESSMENT REQUEST RECEIVED TIMES</td>
      </tr>
    </tbody>
  </table>
</div>"""
            aliasDitaXml == """<div>
  <p>This Attribute is also known by these names:</p>
  <simpletable relcolwidth='1* 2*'>
    <sthead>
      <stentry>Context</stentry>
      <stentry>Alias</stentry>
    </sthead>
    <strow>
      <stentry outputclass='new'>
        <ph>Also known as</ph>
      </stentry>
      <stentry outputclass='new'>
        <ph>DIAGNOSTIC TEST REQUEST RECEIVED TIME TRACKING</ph>
      </stentry>
    </strow>
    <strow>
      <stentry>
        <ph>Plural</ph>
      </stentry>
      <stentry>
        <ph>DIAGNOSTIC TEST REQUEST RECEIVED TIMES</ph>
      </stentry>
    </strow>
    <strow>
      <stentry outputclass='new'>
        <ph>Schema</ph>
      </stentry>
      <stentry outputclass='new'>
        <ph>DIAGNOSTIC SCHEMA</ph>
      </stentry>
    </strow>
    <strow>
      <stentry outputclass='deleted'>
        <ph>Also known as</ph>
      </stentry>
      <stentry outputclass='deleted'>
        <ph>Also known as</ph>
      </stentry>
    </strow>
    <strow>
      <stentry outputclass='deleted'>
        <ph>Formerly</ph>
      </stentry>
      <stentry outputclass='deleted'>
        <ph>Formerly</ph>
      </stentry>
    </strow>
  </simpletable>
</div>"""
        }
    }
}

@Integration
@Rollback
class NhsDDClassChangePaperChangesSpec extends ChangePaperChangesSpec  {
    @Override
    def getExpectedStereotype() {
        "Class"
    }

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
            stereotype == expectedStereotype
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