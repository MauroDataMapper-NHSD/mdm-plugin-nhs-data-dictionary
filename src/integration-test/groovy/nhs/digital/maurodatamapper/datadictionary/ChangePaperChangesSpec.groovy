/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package nhs.digital.maurodatamapper.datadictionary

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDCode
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSet
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetClass
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetElement
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDElement
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
            !preferDitaDetail
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
            !preferDitaDetail
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

    void "should return changes for an updated description that contains a html table"() {
        given: "there is a previous component"
        NhsDDAttribute previousComponent = new NhsDDAttribute(
            name: "DIAGNOSTIC TEST REQUEST RECEIVED TIME",
            definition: "The time the <a href=\"DIAGNOSTIC TEST REQUEST\">DIAGNOSTIC TEST REQUEST</a> was received.")

        and: "there is a current component"
        NhsDDAttribute currentComponent = new NhsDDAttribute(
            name: "DIAGNOSTIC TEST REQUEST RECEIVED TIME",
            definition: "The time the <a href=\"DIAGNOSTIC TEST REQUEST\">DIAGNOSTIC TEST REQUEST</a> was obtained.<table border='0'></table>")

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
            !preferDitaDetail
            htmlDetail == """<div>
  <p class='info-message'>Contains content that cannot be directly compared. This is the current content.</p>
  <div>The time the <a href="DIAGNOSTIC TEST REQUEST">DIAGNOSTIC TEST REQUEST</a> was obtained.<table border='0'></table></div>
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
            !preferDitaDetail
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
            preferDitaDetail
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

        Change updatedChange = changes.first()
        verifyAll(updatedChange) {
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
            preferDitaDetail
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

    void "should return changes for a new attribute with national codes"() {
        given: "there is no previous component"
        NhsDDAttribute previousComponent = null

        and: "there is a current component"
        NhsDDAttribute currentComponent = new NhsDDAttribute(
            name: "ABLATIVE THERAPY TYPE",
            definition: "The type of <a href=\"ABLATIVE THERAPY TYPE\">ABLATIVE THERAPY TYPE</a>.",
            codes: [
                new NhsDDCode(code: "7", definition: "Other Ablative Therapy (not listed)", webOrder: 2),
                new NhsDDCode(code: "8", definition: "Other Ablative Treatment (not listed) (Retired 1 April 2024)", webOrder: 3),
                new NhsDDCode(code: "9", definition: "Not Known (Not Recorded)", webOrder: 0),
                new NhsDDCode(code: "M", definition: "Microwave Ablation", webOrder: 1),
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

        Change nationalCodesChange = changes.last()
        String ditaXml = nationalCodesChange.ditaDetail.toXmlString()
        verifyAll(nationalCodesChange) {
            changeType == NATIONAL_CODES_TYPE
            stereotype == expectedStereotype
            !oldItem
            newItem == currentComponent
            preferDitaDetail
            htmlDetail == """<div>
  <table class='codes-table'>
    <thead>
      <th>Code</th>
      <th>Description</th>
    </thead>
    <tbody>
      <tr>
        <td class='new'>9</td>
        <td class='new'>Not Known (Not Recorded)</td>
      </tr>
      <tr>
        <td class='new'>M</td>
        <td class='new'>Microwave Ablation</td>
      </tr>
      <tr>
        <td class='new'>7</td>
        <td class='new'>Other Ablative Therapy (not listed)</td>
      </tr>
      <tr>
        <td class='new'>8</td>
        <td class='new'>Other Ablative Treatment (not listed) (Retired 1 April 2024)</td>
      </tr>
    </tbody>
  </table>
</div>"""
            ditaXml == """<div>
  <simpletable relcolwidth='1* 4*'>
    <sthead>
      <stentry>Code</stentry>
      <stentry>Description</stentry>
    </sthead>
    <strow>
      <stentry outputclass='new'>
        <ph>9</ph>
      </stentry>
      <stentry outputclass='new'>
        <ph>Not Known (Not Recorded)</ph>
      </stentry>
    </strow>
    <strow>
      <stentry outputclass='new'>
        <ph>M</ph>
      </stentry>
      <stentry outputclass='new'>
        <ph>Microwave Ablation</ph>
      </stentry>
    </strow>
    <strow>
      <stentry outputclass='new'>
        <ph>7</ph>
      </stentry>
      <stentry outputclass='new'>
        <ph>Other Ablative Therapy (not listed)</ph>
      </stentry>
    </strow>
    <strow>
      <stentry outputclass='new'>
        <ph>8</ph>
      </stentry>
      <stentry outputclass='new'>
        <ph>Other Ablative Treatment (not listed) (Retired 1 April 2024)</ph>
      </stentry>
    </strow>
  </simpletable>
</div>"""
        }
    }

    void "should return changes for an attribute with updated national codes"() {
        given: "there is a previous component"
        NhsDDAttribute previousComponent = new NhsDDAttribute(
            name: "ABLATIVE THERAPY TYPE",
            definition: "The type of <a href=\"ABLATIVE THERAPY TYPE\">ABLATIVE THERAPY TYPE</a>.",
            codes: [
                new NhsDDCode(code: "7", definition: "Other Ablative Therapy (not listed)", webOrder: 2), // Remove
                new NhsDDCode(code: "8", definition: "Other Ablative Treatment (not listed) (Retired 1 April 2024)", webOrder: 3), // Changed
                new NhsDDCode(code: "9", definition: "Not Known (Not Recorded)", webOrder: 0), // Unchanged
                new NhsDDCode(code: "M", definition: "Microwave Ablation", webOrder: 1), // Unchanged
            ])

        and: "there is a current component"
        NhsDDAttribute currentComponent = new NhsDDAttribute(
            name: "ABLATIVE THERAPY TYPE",
            definition: "The form of <a href=\"ABLATIVE THERAPY TYPE\">ABLATIVE THERAPY TYPE</a>.",
            codes: [
                new NhsDDCode(code: "8", definition: "Other Ablative Treatment (not listed) (Retired 1 May 2024)", webOrder: 3), // Changed
                new NhsDDCode(code: "9", definition: "Not Known (Not Recorded)", webOrder: 0), // Unchanged
                new NhsDDCode(code: "M", definition: "Microwave Ablation", webOrder: 1),    // Unchanged
                new NhsDDCode(code: "R", definition: "Radiofrequency Ablation (RFA)", webOrder: 2, webPresentation: "<p><a href=\"te:NHS Business Definitions|tm:Radiofrequency Ablation\">Radiofrequency Ablation</a></p>"),    // Added
            ])

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
            changeType == NATIONAL_CODES_TYPE
            stereotype == expectedStereotype
            oldItem == previousComponent
            newItem == currentComponent
            preferDitaDetail
            htmlDetail == """<div>
  <table class='codes-table'>
    <thead>
      <th>Code</th>
      <th>Description</th>
    </thead>
    <tbody>
      <tr>
        <td>9</td>
        <td>Not Known (Not Recorded)</td>
      </tr>
      <tr>
        <td>M</td>
        <td>Microwave Ablation</td>
      </tr>
      <tr>
        <td class='new'>R</td>
        <td class='new'><p><a href="te:NHS Business Definitions|tm:Radiofrequency Ablation">Radiofrequency Ablation</a></p></td>
      </tr>
      <tr>
        <td class='new'>8</td>
        <td class='new'>Other Ablative Treatment (not listed) (Retired 1 May 2024)</td>
      </tr>
      <tr>
        <td class='deleted'>7</td>
        <td class='deleted'>Other Ablative Therapy (not listed)</td>
      </tr>
      <tr>
        <td class='deleted'>8</td>
        <td class='deleted'>Other Ablative Treatment (not listed) (Retired 1 April 2024)</td>
      </tr>
    </tbody>
  </table>
</div>"""
            ditaXml == """<div>
  <simpletable relcolwidth='1* 4*'>
    <sthead>
      <stentry>Code</stentry>
      <stentry>Description</stentry>
    </sthead>
    <strow>
      <stentry>
        <ph>9</ph>
      </stentry>
      <stentry>
        <ph>Not Known (Not Recorded)</ph>
      </stentry>
    </strow>
    <strow>
      <stentry>
        <ph>M</ph>
      </stentry>
      <stentry>
        <ph>Microwave Ablation</ph>
      </stentry>
    </strow>
    <strow>
      <stentry outputclass='new'>
        <ph>R</ph>
      </stentry>
      <stentry outputclass='new'>
        <div>
          <p>
            <xref keyref='te:NHS%20Business%20Definitions|tm:Radiofrequency%20Ablation' scope='local'>Radiofrequency Ablation</xref>
          </p>
        </div>
      </stentry>
    </strow>
    <strow>
      <stentry outputclass='new'>
        <ph>8</ph>
      </stentry>
      <stentry outputclass='new'>
        <ph>Other Ablative Treatment (not listed) (Retired 1 May 2024)</ph>
      </stentry>
    </strow>
    <strow>
      <stentry outputclass='deleted'>
        <ph>7</ph>
      </stentry>
      <stentry outputclass='deleted'>
        <ph>Other Ablative Therapy (not listed)</ph>
      </stentry>
    </strow>
    <strow>
      <stentry outputclass='deleted'>
        <ph>8</ph>
      </stentry>
      <stentry outputclass='deleted'>
        <ph>Other Ablative Treatment (not listed) (Retired 1 April 2024)</ph>
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
            !preferDitaDetail
            htmlDetail == """<div>
  <p>Attributes of this Class are:</p>
  <div>
    <span class='attribute-name'>SERVICE REPORT IDENTIFIER</span>
    <span class='attribute-key'>Key</span>
  </div>
  <div>
    <span class='attribute-name'>SERVICE REPORT ISSUE DATE</span>
  </div>
  <div>
    <span class='attribute-name new'>SERVICE REPORT ISSUE TIME</span>
  </div>
  <div>
    <span class='attribute-name'>SERVICE REPORT STATUS</span>
  </div>
  <div>
    <span class='attribute-name deleted'>SERVICE REPORT STATUS CODE</span>
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
    <div>
      <ph outputclass='attribute-name new'>SERVICE REPORT ISSUE TIME</ph>
    </div>
    <div>
      <ph outputclass='attribute-name'>SERVICE REPORT STATUS</ph>
    </div>
    <div>
      <ph outputclass='attribute-name deleted'>SERVICE REPORT STATUS CODE</ph>
    </div>
  </div>
</div>"""
        }
    }
}

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
  <dl>
    <dt>Format / Length</dt>
    <dd>
      <p class='new'>an8 HH:MM:SS</p>
    </dd>
  </dl>
</div>"""
            formatChangeDitaXml == """<div>
  <dl>
    <dlentry>
      <dt>Format / Length</dt>
      <dd>
        <p outputclass='new'>an8 HH:MM:SS</p>
      </dd>
    </dlentry>
  </dl>
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
  <dl>
    <dt>Format / Length</dt>
    <dd>
      <p class='new'>min an5 max an9</p>
      <p class='deleted'>an8 HH:MM:SS</p>
    </dd>
  </dl>
</div>"""
            formatChangeDitaXml == """<div>
  <dl>
    <dlentry>
      <dt>Format / Length</dt>
      <dd>
        <p outputclass='new'>min an5 max an9</p>
        <p outputclass='deleted'>an8 HH:MM:SS</p>
      </dd>
    </dlentry>
  </dl>
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
}

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
        List<Change> changes = currentComponent.getChanges(previousComponent, true)

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
            specificationDitaXml == """<div>
  <div>
    <table outputclass='table table-striped table-bordered table-sm'>
      <tgroup cols='2'>
        <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
        <colspec colnum='2' colname='col2' colwidth='8*' />
        <thead>
          <row outputclass='thead-light'>
            <entry namest='col1' nameend='col2'>
              <b>PERSONAL AND DEMOGRAPHIC</b>
              <p>at least one of these data items must be present.</p>
            </entry>
          </row>
          <row>
            <entry>
              <p>Mandation</p>
            </entry>
            <entry>
              <p>Data Elements</p>
            </entry>
          </row>
        </thead>
        <tbody>
          <row>
            <entry>
              <p>R</p>
            </entry>
            <entry>
              <p>
                <xref outputclass='element' keyref='data_element_nhs_number' scope='local' />
              </p>
            </entry>
          </row>
          <row>
            <entry>
              <p>R</p>
            </entry>
            <entry>
              <p>
                <xref outputclass='element' keyref='data_element_nhs_number_status_indicator_code' scope='local' />
              </p>
            </entry>
          </row>
          <row>
            <entry>
              <p>R</p>
            </entry>
            <entry>
              <p>
                <xref outputclass='element' keyref='data_element_person_birth_date' scope='local' />
              </p>
            </entry>
          </row>
        </tbody>
      </tgroup>
    </table>
  </div>
  <div>
    <table outputclass='table table-striped table-bordered table-sm'>
      <tgroup cols='2'>
        <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
        <colspec colnum='2' colname='col2' colwidth='8*' />
        <thead>
          <row outputclass='thead-light'>
            <entry namest='col1' nameend='col2'>
              <b>REFERRALS</b>
              <p>One occurrence of this group is required.</p>
            </entry>
          </row>
          <row>
            <entry>
              <p>Mandation</p>
            </entry>
            <entry>
              <p>Data Elements</p>
            </entry>
          </row>
        </thead>
        <tbody>
          <row>
            <entry>
              <p>M</p>
            </entry>
            <entry>
              <p>
                <xref outputclass='element' keyref='data_element_patient_source_setting_type_diagnostic_imaging' scope='local' />
              </p>
            </entry>
          </row>
          <row>
            <entry>
              <p>R</p>
            </entry>
            <entry>
              <p>
                <xref outputclass='element' keyref='data_element_referrer_code' scope='local' />
              </p>
            </entry>
          </row>
          <row>
            <entry>
              <p>R</p>
            </entry>
            <entry>
              <p>
                <xref outputclass='element' keyref='data_element_diagnostic_test_request_date' scope='local' />
              </p>
            </entry>
          </row>
        </tbody>
      </tgroup>
    </table>
  </div>
</div>"""
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
        List<Change> changes = currentComponent.getChanges(previousComponent, true)

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
            specificationDitaXml == """<div>
  <div>
    <table outputclass='table table-striped table-bordered table-sm'>
      <tgroup cols='2'>
        <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
        <colspec colnum='2' colname='col2' colwidth='8*' />
        <thead>
          <row outputclass='thead-light'>
            <entry namest='col1' nameend='col2'>
              <b>PERSONAL AND DEMOGRAPHIC</b>
              <p>at least one of these data items must be present.</p>
            </entry>
          </row>
          <row>
            <entry>
              <p>Mandation</p>
            </entry>
            <entry>
              <p>Data Elements</p>
            </entry>
          </row>
        </thead>
        <tbody>
          <row>
            <entry>
              <p>R</p>
            </entry>
            <entry>
              <p>
                <xref outputclass='element' keyref='data_element_nhs_number' scope='local' />
              </p>
            </entry>
          </row>
          <row>
            <entry>
              <p>R</p>
            </entry>
            <entry>
              <p>
                <xref outputclass='element' keyref='data_element_nhs_number_status_indicator_code' scope='local' />
              </p>
            </entry>
          </row>
          <row>
            <entry>
              <p>R</p>
            </entry>
            <entry>
              <p>
                <xref outputclass='element' keyref='data_element_person_birth_date' scope='local' />
              </p>
            </entry>
          </row>
          <row>
            <entry>
              <p>P</p>
            </entry>
            <entry>
              <p>
                <xref outputclass='element' keyref='data_element_ethnic_category_2021' scope='local' />
              </p>
            </entry>
          </row>
        </tbody>
      </tgroup>
    </table>
  </div>
  <div>
    <table outputclass='table table-striped table-bordered table-sm'>
      <tgroup cols='2'>
        <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
        <colspec colnum='2' colname='col2' colwidth='8*' />
        <thead>
          <row outputclass='thead-light'>
            <entry namest='col1' nameend='col2'>
              <b>REFERRALS</b>
              <p>One occurrence of this group is required.</p>
            </entry>
          </row>
          <row>
            <entry>
              <p>Mandation</p>
            </entry>
            <entry>
              <p>Data Elements</p>
            </entry>
          </row>
        </thead>
        <tbody>
          <row>
            <entry>
              <p>M</p>
            </entry>
            <entry>
              <p>
                <xref outputclass='element' keyref='data_element_patient_source_setting_type_diagnostic_imaging' scope='local' />
              </p>
            </entry>
          </row>
          <row>
            <entry>
              <p>R</p>
            </entry>
            <entry>
              <p>
                <xref outputclass='element' keyref='data_element_referrer_code' scope='local' />
              </p>
            </entry>
          </row>
          <row>
            <entry>
              <p>R</p>
            </entry>
            <entry>
              <p>
                <xref outputclass='element' keyref='data_element_diagnostic_test_request_date' scope='local' />
              </p>
            </entry>
          </row>
          <row>
            <entry>
              <p>R</p>
            </entry>
            <entry>
              <p>
                <xref outputclass='element' keyref='data_element_diagnostic_test_request_time' scope='local' />
              </p>
            </entry>
          </row>
        </tbody>
      </tgroup>
    </table>
  </div>
</div>"""
        }
    }

    void "should return changes for a new data set without data set specification"() {
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
        List<Change> changes = currentComponent.getChanges(previousComponent, false)

        then: "the expected change object is returned"
        with {
            changes.size() == 1
        }

        Change newChange = changes.first()
        verifyAll(newChange) {
            changeType == NEW_TYPE
            stereotype == expectedStereotype
            !oldItem
            newItem == currentComponent
        }
    }

    void "should return changes for an updated data set without data set specification"() {
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
        List<Change> changes = currentComponent.getChanges(previousComponent, false)

        then: "the expected change object is returned"
        with {
            changes.size() == 1
        }

        Change newChange = changes.first()
        verifyAll(newChange) {
            changeType == UPDATED_DESCRIPTION_TYPE
            stereotype == expectedStereotype
            oldItem == previousComponent
            newItem == currentComponent
        }
    }
}