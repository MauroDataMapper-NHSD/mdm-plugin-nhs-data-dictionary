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
package nhs.digital.maurodatamapper.datadictionary.publish.changePaper

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClassRelationship
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change

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
  <table class='attribute-table'>
    <thead>
      <th width='5%'>Key</th>
      <th width='95%'>Attribute Name</th>
    </thead>
    <tbody>
      <tr>
        <td>K</td>
        <td>SERVICE REPORT IDENTIFIER</td>
      </tr>
      <tr>
        <td></td>
        <td>SERVICE REPORT ISSUE DATE</td>
      </tr>
      <tr>
        <td class='new'></td>
        <td class='new'>SERVICE REPORT ISSUE TIME</td>
      </tr>
      <tr>
        <td></td>
        <td>SERVICE REPORT STATUS</td>
      </tr>
      <tr>
        <td class='deleted'></td>
        <td class='deleted'>SERVICE REPORT STATUS CODE</td>
      </tr>
    </tbody>
  </table>
</div>"""
            ditaXml == """<div>
  <div>
    <p>Attributes of this Class are:</p>
    <table outputclass='attribute-table'>
      <tgroup cols='2'>
        <colspec colname='col0' colwidth='5*' />
        <colspec colname='col1' colwidth='95*' />
        <thead>
          <row>
            <entry scope='col'>Key</entry>
            <entry scope='col'>Attribute Name</entry>
          </row>
        </thead>
        <tbody>
          <row>
            <entry>K</entry>
            <entry>SERVICE REPORT IDENTIFIER</entry>
          </row>
          <row>
            <entry />
            <entry>SERVICE REPORT ISSUE DATE</entry>
          </row>
          <row>
            <entry outputclass='new' />
            <entry outputclass='new'>SERVICE REPORT ISSUE TIME</entry>
          </row>
          <row>
            <entry />
            <entry>SERVICE REPORT STATUS</entry>
          </row>
          <row>
            <entry outputclass='deleted' />
            <entry outputclass='deleted'>SERVICE REPORT STATUS CODE</entry>
          </row>
        </tbody>
      </tgroup>
    </table>
  </div>
</div>"""
        }
    }

    void "should return changed relationships in a class"() {
        given: "there is a previous component"
        NhsDDClass previousComponent = new NhsDDClass(name: "SERVICE REPORT", classRelationships: [
            new NhsDDClassRelationship(relationshipDescription: "requested by", targetClass: new NhsDDClass(name: "CARE PROFESSIONAL")),
            new NhsDDClassRelationship(relationshipDescription: "issued by", targetClass: new NhsDDClass(name: "ORGANISATION"))
        ])

        and: "there is a current component"
        NhsDDClass currentComponent = new NhsDDClass(name: "SERVICE REPORT", classRelationships: [
            new NhsDDClassRelationship(relationshipDescription: "requested by", targetClass: new NhsDDClass(name: "CARE PROFESSIONAL")),
            new NhsDDClassRelationship(relationshipDescription: "copied to", targetClass: new NhsDDClass(name: "ORGANISATION"))
        ])

        when: "finding the changes"
        List<Change> changes = currentComponent.getChanges(previousComponent)

        then: "the expected change object is returned"
        Change change = changes.first()
        with {
            change
        }

        String ditaXml = change.ditaDetail.toXmlString()

        verifyAll(change) {
            changeType == CHANGED_RELATIONSHIPS_TYPE
            stereotype == expectedStereotype
            oldItem == previousComponent
            newItem == currentComponent
            !preferDitaDetail
            htmlDetail == """<div>
  <p>Each SERVICE REPORT</p>
  <table class='relationship-table'>
    <thead>
      <th width='5%'>Key</th>
      <th width='55%'>Relationship</th>
      <th width='40%'>Class</th>
    </thead>
    <tbody>
      <tr>
        <td></td>
        <td>requested by</td>
        <td>CARE PROFESSIONAL</td>
      </tr>
      <tr>
        <td class='new'></td>
        <td class='new'>copied to</td>
        <td class='new'>ORGANISATION</td>
      </tr>
      <tr>
        <td class='deleted'></td>
        <td class='deleted'>issued by</td>
        <td class='deleted'>ORGANISATION</td>
      </tr>
    </tbody>
  </table>
</div>"""
            ditaXml == """<div>
  <div>
    <p>Each SERVICE REPORT</p>
    <table outputclass='relationship-table'>
      <tgroup cols='3'>
        <colspec colname='col0' colwidth='5*' />
        <colspec colname='col1' colwidth='55*' />
        <colspec colname='col2' colwidth='40*' />
        <thead>
          <row>
            <entry scope='col'>Key</entry>
            <entry scope='col'>Relationship</entry>
            <entry scope='col'>Class</entry>
          </row>
        </thead>
        <tbody>
          <row>
            <entry />
            <entry>requested by</entry>
            <entry>CARE PROFESSIONAL</entry>
          </row>
          <row>
            <entry outputclass='new' />
            <entry outputclass='new'>copied to</entry>
            <entry outputclass='new'>ORGANISATION</entry>
          </row>
          <row>
            <entry outputclass='deleted' />
            <entry outputclass='deleted'>issued by</entry>
            <entry outputclass='deleted'>ORGANISATION</entry>
          </row>
        </tbody>
      </tgroup>
    </table>
  </div>
</div>"""
        }
    }
}