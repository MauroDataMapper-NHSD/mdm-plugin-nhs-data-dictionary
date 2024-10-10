package nhs.digital.maurodatamapper.datadictionary.publish.structure


import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDBusinessDefinition
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.AliasesSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DescriptionSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItem

@Integration
@Rollback
class NhsBusinessDefinitionStructureSpec extends Specification {
    NhsDDBusinessDefinition activeItem

    def setup() {
        activeItem = new NhsDDBusinessDefinition(
            catalogueItemId: UUID.fromString("901c2d3d-0111-41d1-acc9-5b501c1dc397"),
            branchId: UUID.fromString("782602d4-e153-45d8-a271-eb42396804da"),
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

        when: "the aliases are published"
        AliasesSection aliasesSection = structure.sections.find { it instanceof AliasesSection } as AliasesSection
        String aliasesHtml = aliasesSection.generateHtml()

        then: "the aliases are published"
        verifyAll {
            aliasesHtml
            aliasesHtml == """<div class='- topic/body body'>
  <p class='- topic/p p'>This NHS Business Definition is also known by these names:</p>
  <div class='simpletable-container'>
    <table class='- topic/simpletable simpletable table table-striped table-sm'>
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
        }
    }
}
