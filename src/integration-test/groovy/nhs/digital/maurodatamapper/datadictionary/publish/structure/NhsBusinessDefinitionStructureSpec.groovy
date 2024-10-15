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
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.NhsDataDictionaryService

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDBusinessDefinition
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDElement
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.publish.ItemLinkScanner
import uk.nhs.digital.maurodatamapper.datadictionary.publish.NhsDataDictionaryComponentPathResolver
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishTarget
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.AliasesSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DescriptionSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItem
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItemState
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.WhereUsedSection

@Integration
@Rollback
class NhsBusinessDefinitionStructureSpec extends Specification {
    @Autowired
    NhsDataDictionaryService dataDictionaryService

    NhsDataDictionary dataDictionary

    NhsDDClass relatedItem

    UUID branchId
    String definition
    NhsDataDictionaryComponentPathResolver componentPathResolver
    MockCatalogueItemPathResolver catalogueItemPathResolver

    NhsDDBusinessDefinition activeItem
    NhsDDBusinessDefinition retiredItem
    NhsDDBusinessDefinition preparatoryItem

    NhsDDBusinessDefinition previousItemDescriptionChange
    NhsDDBusinessDefinition previousItemAliasesChange
    NhsDDBusinessDefinition previousItemAllChange

    PublishContext websiteDitaPublishContext
    PublishContext websiteHtmlPublishContext
    PublishContext changePaperDitaPublishContext
    PublishContext changePaperHtmlPublishContext

    def setup() {
        dataDictionary = dataDictionaryService.newDataDictionary()

        branchId = UUID.fromString("782602d4-e153-45d8-a271-eb42396804da")

        definition = """<p>
    A <a href="te:NHS Business Definitions|tm:Baby First Feed">Baby First Feed</a>
    is a <a href="dm:Classes and Attributes|dc:PERSON PROPERTY">PERSON PROPERTY</a>
    . </p>
<p>
    A <a href="te:NHS Business Definitions|tm:Baby First Feed">Baby First Feed</a>
    is the first feed given to a baby. </p>"""

        setupRelatedItem()

        setupActiveItem()
        setupRetiredItem()
        setupPreparatoryItem()
        setupPreviousItems()

        setupComponentPathResolver()
        setupCatalogueItemPathResolver()
        setupPublishContexts()
    }

    private static <K, V> Map<K, V> copyMap(Map<K, V> properties) {
        Map<K, V> copy = [:]
        properties.each { key, value -> copy[key] = value }
        copy
    }

    private void setupRelatedItem() {
        relatedItem = new NhsDDClass(
            catalogueItemId: UUID.fromString("a57843dd-c1a7-4d37-996c-fcb67e496cb9"),
            branchId: branchId,
            name: "PERSON PROPERTY")
    }

    private void setupComponentPathResolver() {
        componentPathResolver = new NhsDataDictionaryComponentPathResolver()

        // Self-reference this item
        componentPathResolver.add(activeItem.getMauroPath(), activeItem)

        // Add other components
        componentPathResolver.add(relatedItem.getMauroPath(), relatedItem)
    }

    private void setupCatalogueItemPathResolver() {
        catalogueItemPathResolver = new MockCatalogueItemPathResolver()

        // Self-reference this item
        catalogueItemPathResolver.add(activeItem.getMauroPath(), activeItem.catalogueItemId)

        // Add other components
        catalogueItemPathResolver.add(relatedItem.getMauroPath(), relatedItem.catalogueItemId)
    }

    private void setupPublishContexts() {
        ItemLinkScanner ditaItemLinkScanner = ItemLinkScanner.createForDitaOutput(componentPathResolver)
        ItemLinkScanner htmlItemLinkScanner = ItemLinkScanner.createForHtmlPreview(branchId, catalogueItemPathResolver)

        websiteDitaPublishContext = new PublishContext(PublishTarget.WEBSITE)
        websiteDitaPublishContext.setItemLinkScanner(ditaItemLinkScanner)

        websiteHtmlPublishContext = new PublishContext(PublishTarget.WEBSITE)
        websiteHtmlPublishContext.setItemLinkScanner(htmlItemLinkScanner)

        changePaperDitaPublishContext = new PublishContext(PublishTarget.CHANGE_PAPER)
        changePaperDitaPublishContext.setItemLinkScanner(ditaItemLinkScanner)

        changePaperHtmlPublishContext = new PublishContext(PublishTarget.CHANGE_PAPER)
        changePaperHtmlPublishContext.setItemLinkScanner(htmlItemLinkScanner)
    }

    private void setupActiveItem() {
        activeItem = new NhsDDBusinessDefinition(
            catalogueItemId: UUID.fromString("901c2d3d-0111-41d1-acc9-5b501c1dc397"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: "Baby First Feed",
            definition: definition,
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

    private void setupRetiredItem() {
        retiredItem = new NhsDDBusinessDefinition(
            catalogueItemId: UUID.fromString("fb096f90-3273-4c66-8023-c1e32ac5b795"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: this.activeItem.definition,
            otherProperties: copyMap(this.activeItem.otherProperties),
            whereUsed: copyMap(this.activeItem.whereUsed))

        retiredItem.otherProperties["isRetired"] = true.toString()
    }

    private void setupPreparatoryItem() {
        preparatoryItem = new NhsDDBusinessDefinition(
            catalogueItemId: UUID.fromString("f5276a0c-5458-4fa5-9bd3-ff786aef932f"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: this.activeItem.definition,
            otherProperties: copyMap(this.activeItem.otherProperties),
            whereUsed: copyMap(this.activeItem.whereUsed))

        preparatoryItem.otherProperties["isPreparatory"] = true.toString()
    }

    private void setupPreviousItems() {
        previousItemDescriptionChange = new NhsDDBusinessDefinition(
            catalogueItemId: UUID.fromString("22710e00-7c41-4335-97da-2cafe9728804"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: "The previous description",
            otherProperties: copyMap(this.activeItem.otherProperties))

        previousItemAliasesChange = new NhsDDBusinessDefinition(
            catalogueItemId: UUID.fromString("8049682f-761f-4eab-b533-c00781615207"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: this.activeItem.definition,
            otherProperties: [
                'aliasPlural': "Baby's First Feeds",
                'aliasAlsoKnownAs': 'Baby Food',
            ])

        previousItemAllChange = new NhsDDBusinessDefinition(
            catalogueItemId: UUID.fromString("eeb8929f-819a-4cfe-9209-1e9867fa2b68"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: previousItemDescriptionChange.definition,
            otherProperties: copyMap(previousItemAliasesChange.otherProperties))
    }

    void "should have the correct active item structure"() {
        when: "the publish structure is built"
        DictionaryItem structure = activeItem.getPublishStructure()

        then: "the expected structure is returned"
        verifyAll {
            structure.state == DictionaryItemState.ACTIVE
            structure.name == activeItem.name
            structure.sections.size() == 3
            structure.sections[0] instanceof DescriptionSection
            structure.sections[1] instanceof AliasesSection
            structure.sections[2] instanceof WhereUsedSection
        }
    }

    void "should have the correct retired item structure"() {
        when: "the publish structure is built"
        DictionaryItem structure = retiredItem.getPublishStructure()

        then: "the expected structure is returned"
        verifyAll {
            structure.state == DictionaryItemState.RETIRED
            structure.name == retiredItem.name
            structure.sections.size() == 1
            structure.sections[0] instanceof DescriptionSection
        }
    }

    void "should have the correct preparatory item structure"() {
        when: "the publish structure is built"
        DictionaryItem structure = preparatoryItem.getPublishStructure()

        then: "the expected structure is returned"
        verifyAll {
            structure.state == DictionaryItemState.PREPARATORY
            structure.name == preparatoryItem.name
            structure.sections.size() == 1
            structure.sections[0] instanceof DescriptionSection
        }
    }

    void "should render an active item to website dita"() {
        given: "the publish structure is built"
        DictionaryItem structure = activeItem.getPublishStructure()
        verifyAll {
            structure
        }

        when: "the publish structure is converted to dita"
        Topic dita = structure.generateDita(websiteDitaPublishContext)
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

          <xref outputclass='businessDefinition' keyref='nhs_business_definition_baby_first_feed' scope='local'>Baby First Feed</xref> is a 

          <xref outputclass='class' keyref='class_person_property' scope='local'>PERSON PROPERTY</xref> .
        </p>
        <p>A 

          <xref outputclass='businessDefinition' keyref='nhs_business_definition_baby_first_feed' scope='local'>Baby First Feed</xref> is the first feed given to a baby.
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

    void "should render a retired item to website dita"() {
        given: "the publish structure is built"
        DictionaryItem structure = retiredItem.getPublishStructure()
        verifyAll {
            structure
        }

        when: "the publish structure is converted to dita"
        Topic dita = structure.generateDita(websiteDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='nhs_business_definition_baby_first_feed_retired'>
  <title outputclass='businessDefinition retired'>
    <text>Baby First Feed (Retired)</text>
  </title>
  <shortdesc>This item has been retired from the NHS Data Model and Dictionary.</shortdesc>
  <topic id='nhs_business_definition_baby_first_feed_retired_description'>
    <title>Description</title>
    <body>
      <div>
        <p>This item has been retired from the NHS Data Model and Dictionary.</p>
        <p>The last version of this item is available in the ?????? release of the NHS Data Model and Dictionary.</p>
        <p>Access to the last live version of this item can be obtained by emailing 

          <xref href='mailto:information.standards@nhs.net' format='html' scope='external'>information.standards@nhs.net</xref> with "NHS Data Model and Dictionary - Archive Request" in the email subject line.
        </p>
      </div>
    </body>
  </topic>
</topic>"""
        }
    }

    void "should render a preparatory item to website dita"() {
        given: "the publish structure is built"
        DictionaryItem structure = preparatoryItem.getPublishStructure()
        verifyAll {
            structure
        }

        when: "the publish structure is converted to dita"
        Topic dita = structure.generateDita(websiteDitaPublishContext)
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
  <shortdesc>This item is being used for development purposes and has not yet been approved.</shortdesc>
  <topic id='nhs_business_definition_baby_first_feed_description'>
    <title>Description</title>
    <body>
      <div>
        <p>This item is being used for development purposes and has not yet been approved.</p>
      </div>
    </body>
  </topic>
</topic>"""
        }
    }

    void "should render an active item to website html"() {
        given: "the publish structure is built"
        DictionaryItem structure = activeItem.getPublishStructure()
        verifyAll {
            structure
        }

        when: "the publish structure is converted to html"
        String titleHtml = structure.generateHtml(websiteHtmlPublishContext)

        then: "the title is published"
        verifyAll {
            titleHtml
            titleHtml == """<h1 class="title topictitle1 businessDefinition">Baby First Feed</h1>
<div class="- topic/body body">
  <p class="- topic/shortdesc shortdesc">A Baby First Feed is the first feed given to a baby.</p>
</div>"""
        }

        when: "the description is converted to html"
        DescriptionSection descriptionSection = structure.sections.find { it instanceof DescriptionSection } as DescriptionSection
        String descriptionHtml = descriptionSection.generateHtml(websiteHtmlPublishContext)

        then: "the description is published"
        verifyAll {
            descriptionHtml
            descriptionHtml == """<div class="- topic/body body">
  <div class="- topic/div div">
    <p class="- topic/p p"><p>
    A <a class="businessDefinition" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/businessDefinition/901c2d3d-0111-41d1-acc9-5b501c1dc397">Baby First Feed</a>
    is a <a class="class" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/a57843dd-c1a7-4d37-996c-fcb67e496cb9">PERSON PROPERTY</a>
    . </p>
<p>
    A <a class="businessDefinition" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/businessDefinition/901c2d3d-0111-41d1-acc9-5b501c1dc397">Baby First Feed</a>
    is the first feed given to a baby. </p></p>
  </div>
</div>"""
        }

        when: "the aliases are converted to html"
        AliasesSection aliasesSection = structure.sections.find { it instanceof AliasesSection } as AliasesSection
        String aliasesHtml = aliasesSection.generateHtml(websiteHtmlPublishContext)

        then: "the aliases are published"
        verifyAll {
            aliasesHtml
            aliasesHtml == """<div class="- topic/body body">
  <p class="- topic/p p">This NHS Business Definition is also known by these names:</p>
  <div class="simpletable-container">
    <table class="- topic/simpletable simpletable table table-sm table-striped">
      <colgroup>
        <col style="width: 34%" />
        <col style="width: 66%" />
      </colgroup>
      <thead>
        <tr class="- topic/sthead sthead thead-light">
          <th class="- topic/stentry stentry">Context</th>
          <th class="- topic/stentry stentry">Alias</th>
        </tr>
      </thead>
      <tbody>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">Plural</td>
          <td class="- topic/stentry stentry">Baby First Feeds</td>
        </tr>
      </tbody>
    </table>
  </div>
</div>"""

            when: "the where used are converted to html"
            WhereUsedSection whereUsedSection = structure.sections.find { it instanceof WhereUsedSection } as WhereUsedSection
            String whereUsedHtml = whereUsedSection.generateHtml(websiteHtmlPublishContext)

            then: "the where used are published"
            verifyAll {
                whereUsedHtml
                whereUsedHtml == """<div class="- topic/body body">
  <div class="simpletable-container">
    <table class="- topic/simpletable simpletable table table-sm table-striped">
      <colgroup>
        <col style="width: 15%" />
        <col style="width: 45%" />
        <col style="width: 40%" />
      </colgroup>
      <thead>
        <tr class="- topic/sthead sthead thead-light">
          <th class="- topic/stentry stentry">Type</th>
          <th class="- topic/stentry stentry">Link</th>
          <th class="- topic/stentry stentry">How used</th>
        </tr>
      </thead>
      <tbody>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">Attribute</td>
          <td class="- topic/stentry stentry">
            <a class="attribute" title="BABY FIRST FEED BREAST MILK INDICATION CODE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/attribute/cc9b5d18-12a0-4c53-b06e-fa5c649494f2">BABY FIRST FEED BREAST MILK INDICATION CODE</a>
          </td>
          <td class="- topic/stentry stentry">references in description Baby First Feed</td>
        </tr>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">Data Element</td>
          <td class="- topic/stentry stentry">
            <a class="element" title="BABY FIRST FEED DATE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/542a6963-cce2-4c8f-b60d-b86b13d43bbe">BABY FIRST FEED DATE</a>
          </td>
          <td class="- topic/stentry stentry">references in description Baby First Feed</td>
        </tr>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">Data Element</td>
          <td class="- topic/stentry stentry">
            <a class="element" title="BABY FIRST FEED TIME" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/b5170409-97aa-464e-9aad-657c8b2e00f8">BABY FIRST FEED TIME</a>
          </td>
          <td class="- topic/stentry stentry">references in description Baby First Feed</td>
        </tr>
        <tr class="- topic/strow strow">
          <td class="- topic/stentry stentry">NHS Business Definition</td>
          <td class="- topic/stentry stentry">
            <a class="businessDefinition" title="Baby First Feed" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/businessDefinition/901c2d3d-0111-41d1-acc9-5b501c1dc397">Baby First Feed</a>
          </td>
          <td class="- topic/stentry stentry">references in description Baby First Feed</td>
        </tr>
      </tbody>
    </table>
  </div>
</div>"""
            }
        }
    }

    void "should produce a diff for a new item to change paper dita"() {
        given: "the publish structure is built"
        DictionaryItem structure = activeItem.getPublishStructure()

        when: "a diff is produced against no previous item"
        DictionaryItem diff = structure.produceDiff(null)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        Topic dita = diff.generateDita(changePaperDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='nhs_business_definition_baby_first_feed'>
  <title>
    <text>Baby First Feed</text>
  </title>
  <shortdesc>Change to NHS Business Definition: New</shortdesc>
  <body>
    <div outputclass='new'>
      <div>
        <p>A 

          <xref outputclass='businessDefinition' keyref='nhs_business_definition_baby_first_feed' scope='local'>Baby First Feed</xref> is a 

          <xref outputclass='class' keyref='class_person_property' scope='local'>PERSON PROPERTY</xref> .
        </p>
        <p>A 

          <xref outputclass='businessDefinition' keyref='nhs_business_definition_baby_first_feed' scope='local'>Baby First Feed</xref> is the first feed given to a baby.
        </p>
      </div>
    </div>
    <div>
      <p>This NHS Business Definition is also known by these names:</p>
      <simpletable relcolwidth='1* 2*'>
        <sthead>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Plural</stentry>
          <stentry outputclass='new'>Baby First Feeds</stentry>
        </strow>
      </simpletable>
    </div>
  </body>
</topic>"""
        }
    }

    void "should produce a diff for an updated item description to change paper dita"() {
        given: "the publish structures are built"
        activeItem.definition = "The current description"
        DictionaryItem previousStructure = previousItemDescriptionChange.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        Topic dita = diff.generateDita(changePaperDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='nhs_business_definition_baby_first_feed'>
  <title>
    <text>Baby First Feed</text>
  </title>
  <shortdesc>Change to NHS Business Definition: Updated description</shortdesc>
  <body>
    <div>
      <div>The 

        <ph id='removed-diff-0' outputclass='diff-html-removed'>previous</ph>
        <ph id='added-diff-0' outputclass='diff-html-added'>current</ph> description

      </div>
    </div>
  </body>
</topic>"""
        }
    }

    void "should produce a diff for an updated item aliases to change paper dita"() {
        given: "the publish structures are built"
        DictionaryItem previousStructure = previousItemAliasesChange.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        Topic dita = diff.generateDita(changePaperDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='nhs_business_definition_baby_first_feed'>
  <title>
    <text>Baby First Feed</text>
  </title>
  <shortdesc>Change to NHS Business Definition: Aliases</shortdesc>
  <body>
    <div>
      <p>This NHS Business Definition is also known by these names:</p>
      <simpletable relcolwidth='1* 2*'>
        <sthead>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Plural</stentry>
          <stentry outputclass='new'>Baby First Feeds</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>Also known as</stentry>
          <stentry outputclass='deleted'>Baby Food</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>Plural</stentry>
          <stentry outputclass='deleted'>Baby's First Feeds</stentry>
        </strow>
      </simpletable>
    </div>
  </body>
</topic>"""
        }
    }

    void "should produce a diff for an updated item description and aliases to change paper dita"() {
        given: "the publish structures are built"
        activeItem.definition = "The current description"
        DictionaryItem previousStructure = previousItemAllChange.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        Topic dita = diff.generateDita(changePaperDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='nhs_business_definition_baby_first_feed'>
  <title>
    <text>Baby First Feed</text>
  </title>
  <shortdesc>Change to NHS Business Definition: Updated description, Aliases</shortdesc>
  <body>
    <div>
      <div>The 

        <ph id='removed-diff-0' outputclass='diff-html-removed'>previous</ph>
        <ph id='added-diff-0' outputclass='diff-html-added'>current</ph> description

      </div>
    </div>
    <div>
      <p>This NHS Business Definition is also known by these names:</p>
      <simpletable relcolwidth='1* 2*'>
        <sthead>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Plural</stentry>
          <stentry outputclass='new'>Baby First Feeds</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>Also known as</stentry>
          <stentry outputclass='deleted'>Baby Food</stentry>
        </strow>
        <strow>
          <stentry outputclass='deleted'>Plural</stentry>
          <stentry outputclass='deleted'>Baby's First Feeds</stentry>
        </strow>
      </simpletable>
    </div>
  </body>
</topic>"""
        }
    }

    void "should produce a diff for a retired item to change paper dita"() {
        given: "the publish structures are built"
        DictionaryItem previousStructure = previousItemAllChange.getPublishStructure()
        DictionaryItem currentStructure = retiredItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        Topic dita = diff.generateDita(changePaperDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='nhs_business_definition_baby_first_feed_retired'>
  <title>
    <text>Baby First Feed</text>
  </title>
  <shortdesc>Change to NHS Business Definition: Retired</shortdesc>
  <body>
    <div>
      <div>
        <ph id='removed-diff-0' outputclass='diff-html-removed'>The previous description</ph>
        <p>
          <ph id='added-diff-0' outputclass='diff-html-added'>This item has been retired from the NHS Data Model and Dictionary.</ph>
        </p>
        <p>
          <ph outputclass='diff-html-added'>The last version of this item is available in the ?????? release of the NHS Data Model and Dictionary.</ph>
        </p>
        <p>
          <ph outputclass='diff-html-added'>Access to the last live version of this item can be obtained by emailing</ph>
          <xref href='mailto:information.standards@nhs.net' format='html' scope='external'>
            <ph outputclass='diff-html-added'>information.standards@nhs.net</ph>
          </xref>
          <ph outputclass='diff-html-added'>with "NHS Data Model and Dictionary - Archive Request" in the email subject line.</ph>
        </p>
      </div>
    </div>
  </body>
</topic>"""
        }
    }

    void "should produce a diff for an updated item description to change paper html"() {
        given: "the publish structures are built"
        activeItem.definition = "The current description"

        DictionaryItem previousStructure = previousItemDescriptionChange.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        String html = diff.generateHtml(changePaperHtmlPublishContext)

        then: "the expected output is published"
        verifyAll {
            html
            html == """<div>
  <h3>Baby First Feed</h3>
  <h4>Change to NHS Business Definition: Updated description</h4>
  <div>
    <div>
      <p>The <span class="diff-html-removed" id="removed-diff-0" previous="first-diff" changeId="removed-diff-0" next="added-diff-0">previous </span><span class="diff-html-added" id="added-diff-0" previous="removed-diff-0" changeId="added-diff-0" next="last-diff">current </span>description</p>
    </div>
  </div>
</div>"""
        }
    }

    void "should produce a diff for an updated item aliases to change paper html"() {
        given: "the publish structures are built"
        DictionaryItem previousStructure = previousItemAliasesChange.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        String html = diff.generateHtml(changePaperHtmlPublishContext)

        then: "the expected output is published"
        verifyAll {
            html
            html == """<div>
  <h3>Baby First Feed</h3>
  <h4>Change to NHS Business Definition: Aliases</h4>
  <div>
    <p>This NHS Business Definition is also known by these names:</p>
    <div>
      <table>
        <colgroup>
          <col style="width: 34%" />
          <col style="width: 66%" />
        </colgroup>
        <thead>
          <tr>
            <th>Context</th>
            <th>Alias</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">Plural</td>
            <td class="new">Baby First Feeds</td>
          </tr>
          <tr>
            <td class="deleted">Also known as</td>
            <td class="deleted">Baby Food</td>
          </tr>
          <tr>
            <td class="deleted">Plural</td>
            <td class="deleted">Baby's First Feeds</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</div>"""
        }
    }

    void "should produce a diff for an updated item description and aliases to change paper html"() {
        given: "the publish structures are built"
        activeItem.definition = "The current description"

        DictionaryItem previousStructure = previousItemAllChange.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted to dita"
        String html = diff.generateHtml(changePaperHtmlPublishContext)

        then: "the expected output is published"
        verifyAll {
            html
            html == """<div>
  <h3>Baby First Feed</h3>
  <h4>Change to NHS Business Definition: Updated description, Aliases</h4>
  <div>
    <div>
      <p>The <span class="diff-html-removed" id="removed-diff-0" previous="first-diff" changeId="removed-diff-0" next="added-diff-0">previous </span><span class="diff-html-added" id="added-diff-0" previous="removed-diff-0" changeId="added-diff-0" next="last-diff">current </span>description</p>
    </div>
    <p>This NHS Business Definition is also known by these names:</p>
    <div>
      <table>
        <colgroup>
          <col style="width: 34%" />
          <col style="width: 66%" />
        </colgroup>
        <thead>
          <tr>
            <th>Context</th>
            <th>Alias</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="new">Plural</td>
            <td class="new">Baby First Feeds</td>
          </tr>
          <tr>
            <td class="deleted">Also known as</td>
            <td class="deleted">Baby Food</td>
          </tr>
          <tr>
            <td class="deleted">Plural</td>
            <td class="deleted">Baby's First Feeds</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</div>"""
        }
    }
}
