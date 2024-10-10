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
package nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDBusinessDefinition
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDElement
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.AliasesSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DescriptionSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItem
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.WhereUsedSection

@Integration
@Rollback
class NhsBusinessDefinitionStructureSpec extends Specification {
    UUID branchId
    NhsDDBusinessDefinition activeItem

    def setup() {
        branchId = UUID.fromString("782602d4-e153-45d8-a271-eb42396804da")

        activeItem = new NhsDDBusinessDefinition(
            catalogueItemId: UUID.fromString("901c2d3d-0111-41d1-acc9-5b501c1dc397"),
            branchId: branchId,
            name: "Baby First Feed",
            definition: """<p>
    A <a href="te:NHS Business Definitions|tm:Baby First Feed">Baby First Feed</a>
    is a <a href="dm:Classes and Attributes|dc:PERSON PROPERTY">PERSON PROPERTY</a>
    . </p>
<p>
    A <a href="te:NHS Business Definitions|tm:Baby First Feed">Baby First Feed</a>
    is the first feed given to a baby. </p>""",
            otherProperties: [
                'aliasPlural': 'Baby First Feeds'
            ])

        String whereUsedDescription = "references in description $activeItem.name"
        activeItem.addWhereUsed(
            new NhsDDElement(
                name: "BABY FIRST FEED TIME",
                catalogueItemId: UUID.fromString("b5170409-97aa-464e-9aad-657c8b2e00f8"),
                branchId: branchId),
            whereUsedDescription)

        activeItem.addWhereUsed(
            new NhsDDElement(
                name: "BABY FIRST FEED DATE",
                catalogueItemId: UUID.fromString("542a6963-cce2-4c8f-b60d-b86b13d43bbe"),
                branchId: branchId),
            whereUsedDescription)

        activeItem.addWhereUsed(
            new NhsDDAttribute(
                name: "BABY FIRST FEED BREAST MILK INDICATION CODE",
                catalogueItemId: UUID.fromString("cc9b5d18-12a0-4c53-b06e-fa5c649494f2"),
                branchId: branchId),
            whereUsedDescription)

        activeItem.addWhereUsed(activeItem, whereUsedDescription)
    }

    // TODO: verify correct structures are returned

    void "should render an active item to dita"() {
        given: "the publish structure is built"
        DictionaryItem structure = activeItem.getPublishStructure()
        verifyAll {
            structure
        }

        when: "the publish structure is converted to dita"
        Topic dita = structure.generateDita()
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='nhs_business_definition_baby_first_feed'>
  <title outputclass='businessDefinition'>
    <text>Baby First Feed</text>
  </title>
  <shortdesc>A Baby First Feed is the first feed given to a baby.</shortdesc>
  <topic id='nhs_business_definition_baby_first_feed_description'>
    <title>Description</title>
    <body>
      <div>
        <p>A 

          <xref keyref='te:NHS%20Business%20Definitions|tm:Baby%20First%20Feed' scope='local'>Baby First Feed</xref> is a 

          <xref keyref='dm:Classes%20and%20Attributes|dc:PERSON%20PROPERTY' scope='local'>PERSON PROPERTY</xref> .
        </p>
        <p>A 

          <xref keyref='te:NHS%20Business%20Definitions|tm:Baby%20First%20Feed' scope='local'>Baby First Feed</xref> is the first feed given to a baby.
        </p>
      </div>
    </body>
  </topic>
  <topic id='nhs_business_definition_baby_first_feed_aliases'>
    <title>Also Known As</title>
    <body>
      <p>This NHS Business Definition is also known by these names:</p>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='1* 2*'>
        <sthead outputclass='thead-light'>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry>Plural</stentry>
          <stentry>Baby First Feeds</stentry>
        </strow>
      </simpletable>
    </body>
  </topic>
  <topic id='nhs_business_definition_baby_first_feed_whereUsed'>
    <title>Where Used</title>
    <body>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='1* 3* 2*'>
        <sthead outputclass='thead-light'>
          <stentry>Type</stentry>
          <stentry>Link</stentry>
          <stentry>How used</stentry>
        </sthead>
        <strow>
          <stentry>Attribute</stentry>
          <stentry>
            <xref outputclass='attribute' keyref='attribute_baby_first_feed_breast_milk_indication_code' format='html'>BABY FIRST FEED BREAST MILK INDICATION CODE</xref>
          </stentry>
          <stentry>references in description Baby First Feed</stentry>
        </strow>
        <strow>
          <stentry>Data Element</stentry>
          <stentry>
            <xref outputclass='element' keyref='data_element_baby_first_feed_date' format='html'>BABY FIRST FEED DATE</xref>
          </stentry>
          <stentry>references in description Baby First Feed</stentry>
        </strow>
        <strow>
          <stentry>Data Element</stentry>
          <stentry>
            <xref outputclass='element' keyref='data_element_baby_first_feed_time' format='html'>BABY FIRST FEED TIME</xref>
          </stentry>
          <stentry>references in description Baby First Feed</stentry>
        </strow>
        <strow>
          <stentry>NHS Business Definition</stentry>
          <stentry>
            <xref outputclass='businessDefinition' keyref='nhs_business_definition_baby_first_feed' format='html'>Baby First Feed</xref>
          </stentry>
          <stentry>references in description Baby First Feed</stentry>
        </strow>
      </simpletable>
    </body>
  </topic>
</topic>"""
        }
    }

    void "should render an active item to html"() {
        given: "the publish structure is built"
        DictionaryItem structure = activeItem.getPublishStructure()
        verifyAll {
            structure
        }

        when: "the publish structure is converted to html"
        String titleHtml = structure.generateHtml()

        then: "the title is published"
        verifyAll {
            titleHtml
            titleHtml == """<h1 class='title topictitle1 businessDefinition'>Baby First Feed</h1>
<div class='- topic/body body'>
  <p class='- topic/shortdesc shortdesc'>A Baby First Feed is the first feed given to a baby.</p>
</div>"""
        }

        when: "the description is converted to html"
        DescriptionSection descriptionSection = structure.sections.find { it instanceof DescriptionSection } as DescriptionSection
        String descriptionHtml = descriptionSection.generateHtml()

        then: "the description is published"
        verifyAll {
            descriptionHtml
            descriptionHtml == """<div class='- topic/body body'>
  <div class='- topic/div div'>
    <p class='- topic/p p'><p>
    A <a href="te:NHS Business Definitions|tm:Baby First Feed">Baby First Feed</a>
    is a <a href="dm:Classes and Attributes|dc:PERSON PROPERTY">PERSON PROPERTY</a>
    . </p>
<p>
    A <a href="te:NHS Business Definitions|tm:Baby First Feed">Baby First Feed</a>
    is the first feed given to a baby. </p></p>
  </div>
</div>"""
        }

        when: "the aliases are converted to html"
        AliasesSection aliasesSection = structure.sections.find { it instanceof AliasesSection } as AliasesSection
        String aliasesHtml = aliasesSection.generateHtml()

        then: "the aliases are published"
        verifyAll {
            aliasesHtml
            aliasesHtml == """<div class='- topic/body body'>
  <p class='- topic/p p'>This NHS Business Definition is also known by these names:</p>
  <div class='simpletable-container'>
    <table class='- topic/simpletable simpletable table table-sm table-striped'>
      <colgroup>
        <col style='width: 34%' />
        <col style='width: 66%' />
      </colgroup>
      <thead>
        <tr class='- topic/sthead sthead thead-light'>
          <th class='- topic/stentry stentry'>Context</th>
          <th class='- topic/stentry stentry'>Alias</th>
        </tr>
      </thead>
      <tbody>
        <tr class='- topic/strow strow'>
          <td class='- topic/stentry stentry'>Plural</td>
          <td class='- topic/stentry stentry'>Baby First Feeds</td>
        </tr>
      </tbody>
    </table>
  </div>
</div>"""

            when: "the where used are converted to html"
            WhereUsedSection whereUsedSection = structure.sections.find { it instanceof WhereUsedSection } as WhereUsedSection
            String whereUsedHtml = whereUsedSection.generateHtml()

            then: "the where used are published"
            verifyAll {
                whereUsedHtml
                whereUsedHtml == """<div class='- topic/body body'>
  <div class='simpletable-container'>
    <table class='- topic/simpletable simpletable table table-sm table-striped'>
      <colgroup>
        <col style='width: 15%' />
        <col style='width: 45%' />
        <col style='width: 40%' />
      </colgroup>
      <thead>
        <tr class='- topic/sthead sthead thead-light'>
          <th class='- topic/stentry stentry'>Type</th>
          <th class='- topic/stentry stentry'>Link</th>
          <th class='- topic/stentry stentry'>How used</th>
        </tr>
      </thead>
      <tbody>
        <tr class='- topic/strow strow'>
          <td class='- topic/stentry stentry'>Attribute</td>
          <td class='- topic/stentry stentry'>
            <a class='attribute' title='BABY FIRST FEED BREAST MILK INDICATION CODE' href='#/preview/782602d4-e153-45d8-a271-eb42396804da/attribute/cc9b5d18-12a0-4c53-b06e-fa5c649494f2'>BABY FIRST FEED BREAST MILK INDICATION CODE</a>
          </td>
          <td class='- topic/stentry stentry'>references in description Baby First Feed</td>
        </tr>
        <tr class='- topic/strow strow'>
          <td class='- topic/stentry stentry'>Data Element</td>
          <td class='- topic/stentry stentry'>
            <a class='element' title='BABY FIRST FEED DATE' href='#/preview/782602d4-e153-45d8-a271-eb42396804da/element/542a6963-cce2-4c8f-b60d-b86b13d43bbe'>BABY FIRST FEED DATE</a>
          </td>
          <td class='- topic/stentry stentry'>references in description Baby First Feed</td>
        </tr>
        <tr class='- topic/strow strow'>
          <td class='- topic/stentry stentry'>Data Element</td>
          <td class='- topic/stentry stentry'>
            <a class='element' title='BABY FIRST FEED TIME' href='#/preview/782602d4-e153-45d8-a271-eb42396804da/element/b5170409-97aa-464e-9aad-657c8b2e00f8'>BABY FIRST FEED TIME</a>
          </td>
          <td class='- topic/stentry stentry'>references in description Baby First Feed</td>
        </tr>
        <tr class='- topic/strow strow'>
          <td class='- topic/stentry stentry'>NHS Business Definition</td>
          <td class='- topic/stentry stentry'>
            <a class='businessDefinition' title='Baby First Feed' href='#/preview/782602d4-e153-45d8-a271-eb42396804da/businessDefinition/901c2d3d-0111-41d1-acc9-5b501c1dc397'>Baby First Feed</a>
          </td>
          <td class='- topic/stentry stentry'>references in description Baby First Feed</td>
        </tr>
      </tbody>
    </table>
  </div>
</div>"""
            }
        }
    }
}
