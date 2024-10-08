package nhs.digital.maurodatamapper.datadictionary.publish.changePaper

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDCode
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDElement
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change

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

        String ditaXml = change.ditaDetail.toXmlString()

        verifyAll(change) {
            changeType == NEW_TYPE
            stereotype == expectedStereotype
            !oldItem
            newItem == currentComponent
            preferDitaDetail
            htmlDetail == """<div>
  <div class='new'>The time the <a href="DIAGNOSTIC TEST REQUEST">DIAGNOSTIC TEST REQUEST</a> was received.</div>
</div>"""
            ditaXml == """<div outputclass='new'>
  <div>The time the 

    <xref keyref='DIAGNOSTIC%20TEST%20REQUEST' scope='local'>DIAGNOSTIC TEST REQUEST</xref> was received.

  </div>
</div>"""
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
            htmlDetail == """<div>
  <div>The time the <a href="DIAGNOSTIC TEST REQUEST">DIAGNOSTIC TEST REQUEST</a> was <span class="diff-html-removed" id="removed-diff-0" previous="first-diff" changeId="removed-diff-0" next="added-diff-0">received</span><span class="diff-html-added" id="added-diff-0" previous="removed-diff-0" changeId="added-diff-0" next="last-diff">obtained</span>.</div>
</div>"""
            ditaXml == """<div>
  <div>The time the 

    <xref keyref='DIAGNOSTIC%20TEST%20REQUEST' scope='local'>DIAGNOSTIC TEST REQUEST</xref> was 

    <ph id='removed-diff-0' outputclass='diff-html-removed'>received</ph>
    <ph id='added-diff-0' outputclass='diff-html-added'>obtained</ph>.
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
            htmlDetail == """<div>
  <div><span class="diff-html-removed" id="removed-diff-0" previous="first-diff" changeId="removed-diff-0" next="added-diff-0">The time the </span><a href="DIAGNOSTIC TEST REQUEST"><span class="diff-html-removed" previous="first-diff" changeId="removed-diff-0" next="added-diff-0">DIAGNOSTIC TEST REQUEST</span></a><span class="diff-html-removed" previous="first-diff" changeId="removed-diff-0" next="added-diff-0"> was received</span><span class="diff-html-added" id="added-diff-0" previous="removed-diff-0" changeId="added-diff-0" next="last-diff">This item has been retired from the NHS Data Model and Dictionary</span>.</div>
</div>"""
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
      <th width='34%'>Context</th>
      <th width='66%'>Alias</th>
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
  <div>
    <p>This Attribute is also known by these names:</p>
    <table outputclass='alias-table'>
      <tgroup cols='2'>
        <colspec colname='col0' colwidth='34*' />
        <colspec colname='col1' colwidth='66*' />
        <thead>
          <row>
            <entry scope='col'>Context</entry>
            <entry scope='col'>Alias</entry>
          </row>
        </thead>
        <tbody>
          <row>
            <entry outputclass='new'>Also known as</entry>
            <entry outputclass='new'>DIAGNOSTIC TEST REQUEST RECEIVED TIMESTAMP</entry>
          </row>
          <row>
            <entry outputclass='new'>Plural</entry>
            <entry outputclass='new'>DIAGNOSTIC TEST REQUEST RECEIVED TIMES</entry>
          </row>
        </tbody>
      </tgroup>
    </table>
  </div>
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
            htmlDetail == """<div>
  <p>This Attribute is also known by these names:</p>
  <table class='alias-table'>
    <thead>
      <th width='34%'>Context</th>
      <th width='66%'>Alias</th>
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
  <div>
    <p>This Attribute is also known by these names:</p>
    <table outputclass='alias-table'>
      <tgroup cols='2'>
        <colspec colname='col0' colwidth='34*' />
        <colspec colname='col1' colwidth='66*' />
        <thead>
          <row>
            <entry scope='col'>Context</entry>
            <entry scope='col'>Alias</entry>
          </row>
        </thead>
        <tbody>
          <row>
            <entry outputclass='new'>Also known as</entry>
            <entry outputclass='new'>DIAGNOSTIC TEST REQUEST RECEIVED TIME TRACKING</entry>
          </row>
          <row>
            <entry>Plural</entry>
            <entry>DIAGNOSTIC TEST REQUEST RECEIVED TIMES</entry>
          </row>
          <row>
            <entry outputclass='new'>Schema</entry>
            <entry outputclass='new'>DIAGNOSTIC SCHEMA</entry>
          </row>
          <row>
            <entry outputclass='deleted'>Also known as</entry>
            <entry outputclass='deleted'>DIAGNOSTIC TEST REQUEST RECEIVED TIMESTAMP</entry>
          </row>
          <row>
            <entry outputclass='deleted'>Formerly</entry>
            <entry outputclass='deleted'>DIAGNOSTIC ASSESSMENT REQUEST RECEIVED TIMES</entry>
          </row>
        </tbody>
      </tgroup>
    </table>
  </div>
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
            htmlDetail == """<div>
  <p>
    <b>National Codes</b>
  </p>
  <table class='codes-table'>
    <thead>
      <th width='20%'>Code</th>
      <th width='80%'>Description</th>
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
  <div>
    <p>
      <b>National Codes</b>
    </p>
    <table outputclass='codes-table'>
      <tgroup cols='2'>
        <colspec colname='col0' colwidth='20*' />
        <colspec colname='col1' colwidth='80*' />
        <thead>
          <row>
            <entry scope='col'>Code</entry>
            <entry scope='col'>Description</entry>
          </row>
        </thead>
        <tbody>
          <row>
            <entry outputclass='new'>9</entry>
            <entry outputclass='new'>Not Known (Not Recorded)</entry>
          </row>
          <row>
            <entry outputclass='new'>M</entry>
            <entry outputclass='new'>Microwave Ablation</entry>
          </row>
          <row>
            <entry outputclass='new'>7</entry>
            <entry outputclass='new'>Other Ablative Therapy (not listed)</entry>
          </row>
          <row>
            <entry outputclass='new'>8</entry>
            <entry outputclass='new'>Other Ablative Treatment (not listed) (Retired 1 April 2024)</entry>
          </row>
        </tbody>
      </tgroup>
    </table>
  </div>
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
            htmlDetail == """<div>
  <p>
    <b>National Codes</b>
  </p>
  <table class='codes-table'>
    <thead>
      <th width='20%'>Code</th>
      <th width='80%'>Description</th>
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
  <div>
    <p>
      <b>National Codes</b>
    </p>
    <table outputclass='codes-table'>
      <tgroup cols='2'>
        <colspec colname='col0' colwidth='20*' />
        <colspec colname='col1' colwidth='80*' />
        <thead>
          <row>
            <entry scope='col'>Code</entry>
            <entry scope='col'>Description</entry>
          </row>
        </thead>
        <tbody>
          <row>
            <entry>9</entry>
            <entry>Not Known (Not Recorded)</entry>
          </row>
          <row>
            <entry>M</entry>
            <entry>Microwave Ablation</entry>
          </row>
          <row>
            <entry outputclass='new'>R</entry>
            <entry outputclass='new'>
              <p>
                <xref keyref='te:NHS%20Business%20Definitions|tm:Radiofrequency%20Ablation' scope='local'>Radiofrequency Ablation</xref>
              </p>
            </entry>
          </row>
          <row>
            <entry outputclass='new'>8</entry>
            <entry outputclass='new'>Other Ablative Treatment (not listed) (Retired 1 May 2024)</entry>
          </row>
          <row>
            <entry outputclass='deleted'>7</entry>
            <entry outputclass='deleted'>Other Ablative Therapy (not listed)</entry>
          </row>
          <row>
            <entry outputclass='deleted'>8</entry>
            <entry outputclass='deleted'>Other Ablative Treatment (not listed) (Retired 1 April 2024)</entry>
          </row>
        </tbody>
      </tgroup>
    </table>
  </div>
</div>"""
        }
    }

    void "should return changes for a new attribute with linked elements"() {
        given: "there is no previous component"
        NhsDDAttribute previousComponent = null

        and: "there is a current component"
        NhsDDAttribute currentComponent = new NhsDDAttribute(
            name: "ABLATIVE THERAPY TYPE",
            definition: "The type of <a href=\"ABLATIVE THERAPY TYPE\">ABLATIVE THERAPY TYPE</a>.")
        currentComponent.instantiatedByElements.add(new NhsDDElement(name: "ABLATIVE THERAPY TYPE"))
        currentComponent.instantiatedByElements.add(new NhsDDElement(name: "ACTIVITY OFFER DATE"))

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
            changeType == CHANGED_ELEMENTS_TYPE
            stereotype == expectedStereotype
            !oldItem
            newItem == currentComponent
            htmlDetail == """<div>
  <p>
    <b>Data Elements</b>
  </p>
  <ul>
    <li class='new'>ABLATIVE THERAPY TYPE</li>
    <li class='new'>ACTIVITY OFFER DATE</li>
  </ul>
</div>"""
            ditaXml == """<div>
  <div>
    <p>
      <b>Data Elements</b>
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
        NhsDDAttribute previousComponent = new NhsDDAttribute(
            name: "ABLATIVE THERAPY TYPE",
            definition: "The type of <a href=\"ABLATIVE THERAPY TYPE\">ABLATIVE THERAPY TYPE</a>.")
        previousComponent.instantiatedByElements.add(new NhsDDElement(name: "ABLATIVE THERAPY TYPE"))
        previousComponent.instantiatedByElements.add(new NhsDDElement(name: "ACTIVITY OFFER DATE"))

        and: "there is a current component"
        NhsDDAttribute currentComponent = new NhsDDAttribute(
            name: "ABLATIVE THERAPY TYPE",
            definition: "The form of <a href=\"ABLATIVE THERAPY TYPE\">ABLATIVE THERAPY TYPE</a>.")
        currentComponent.instantiatedByElements.add(new NhsDDElement(name: "ABLATIVE THERAPY TYPE"))    // Unchanged
        currentComponent.instantiatedByElements.add(new NhsDDElement(name: "ACTIVITY UNIT PRICE"))      // New

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
            changeType == CHANGED_ELEMENTS_TYPE
            stereotype == expectedStereotype
            oldItem == previousComponent
            newItem == currentComponent
            htmlDetail == """<div>
  <p>
    <b>Data Elements</b>
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
      <b>Data Elements</b>
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
