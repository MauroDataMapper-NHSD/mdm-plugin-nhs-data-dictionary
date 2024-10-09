package nhs.digital.maurodatamapper.datadictionary.publish.structure


import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDBusinessDefinition
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItem

@Integration
@Rollback
class BusinessDefinitionStructureSpec extends Specification {
    void "should render a basic structure to dita"() {
        def item = new NhsDDBusinessDefinition(
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

        when: "the publish structure is built"
        DictionaryItem structure = item.getPublishStructure()
        verifyAll {
            structure
        }

        and: "the publish structure is converted to dita"
        Topic dita = structure.generateDita()
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == "foo"
        }
    }
}
