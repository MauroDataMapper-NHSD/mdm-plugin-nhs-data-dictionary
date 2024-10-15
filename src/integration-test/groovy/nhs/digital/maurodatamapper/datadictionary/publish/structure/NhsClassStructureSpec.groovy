package nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.NhsDataDictionaryService

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDChangeLog
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClassRelationship
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDElement
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.AliasesSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.ChangeLogSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.ClassAttributeSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.ClassRelationshipSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DescriptionSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItem
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItemState
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.WhereUsedSection

@Integration
@Rollback
class NhsClassStructureSpec extends DataDictionaryComponentStructureSpec<NhsDDClass> {
    @Autowired
    NhsDataDictionaryService dataDictionaryService

    NhsDDClass relatedServiceClass
    NhsDDClass relatedPatientClass
    NhsDDClass relatedCareProfessionalClass

    @Override
    protected NhsDataDictionary createDataDictionary() {
        dataDictionaryService.newDataDictionary()
    }

    @Override
    String getDefinition() {
        """<p>A provision of <a href="dm:Classes and Attributes|dc:SERVICE">SERVICES</a> 
to a <a href="dm:Classes and Attributes|dc:PATIENT">PATIENT</a> by one or more 
<a href="dm:Classes and Attributes|dc:CARE PROFESSIONAL">CARE PROFESSIONALS</a>.</p>"""
    }

    @Override
    void setupRelatedItems() {
        relatedServiceClass = new NhsDDClass(
            catalogueItemId: UUID.fromString("79ab1e21-4407-4aae-b777-7b7920fa1963"),
            branchId: branchId,
            name: "SERVICE")

        relatedPatientClass = new NhsDDClass(
            catalogueItemId: UUID.fromString("ae2f2b7b-c136-4cc7-9b71-872ee4efb3a6"),
            branchId: branchId,
            name: "PATIENT")

        relatedCareProfessionalClass = new NhsDDClass(
            catalogueItemId: UUID.fromString("9ac6a0b2-b4bf-48af-ad4d-0ecfe268df57"),
            branchId: branchId,
            name: "CARE PROFESSIONAL")
    }

    @Override
    protected void setupComponentPathResolver() {
        super.setupComponentPathResolver()

        componentPathResolver.add(relatedServiceClass.getMauroPath(), relatedServiceClass)
        componentPathResolver.add(relatedPatientClass.getMauroPath(), relatedPatientClass)
        componentPathResolver.add(relatedCareProfessionalClass.getMauroPath(), relatedCareProfessionalClass)
    }

    @Override
    protected void setupCatalogueItemPathResolver() {
        super.setupCatalogueItemPathResolver()

        catalogueItemPathResolver.add(relatedServiceClass.getMauroPath(), relatedServiceClass.catalogueItemId)
        catalogueItemPathResolver.add(relatedPatientClass.getMauroPath(), relatedPatientClass.catalogueItemId)
        catalogueItemPathResolver.add(relatedCareProfessionalClass.getMauroPath(), relatedCareProfessionalClass.catalogueItemId)
    }

    @Override
    void setupActiveItem() {
        activeItem = new NhsDDClass(
            catalogueItemId: UUID.fromString("901c2d3d-0111-41d1-acc9-5b501c1dc397"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: "ACTIVITY",
            definition: definition,
            otherProperties: [
                'aliasPlural': 'ACTIVITIES'
            ])

        addWhereUsed(activeItem, activeItem)    // Self reference
        addWhereUsed(
            activeItem,
            new NhsDDAttribute(name: "ACTIVITY COUNT", catalogueItemId: UUID.fromString("b5170409-97aa-464e-9aad-657c8b2e00f8"), branchId: branchId))
        addWhereUsed(
            activeItem,
            new NhsDDElement(name: "ACTIVITY COUNT (POINT OF DELIVERY)", catalogueItemId: UUID.fromString("542a6963-cce2-4c8f-b60d-b86b13d43bbe"), branchId: branchId))

        addChangeLog(
            activeItem,
            new NhsDDChangeLog(reference: "CR1000", referenceUrl: "https://test.nhs.uk/change/cr1000", description: "Change 1000", implementationDate: "01 April 2024"),
            new NhsDDChangeLog(reference: "CR2000", referenceUrl: "https://test.nhs.uk/change/cr2000", description: "Change 2000", implementationDate: "01 September 2024"))

        activeItem.keyAttributes.add(new NhsDDAttribute(name: "ACTIVITY IDENTIFIER", otherProperties: ["isKey": "true"]))
        activeItem.otherAttributes.add(new NhsDDAttribute(name: "ACTIVITY COUNT"))
        activeItem.otherAttributes.add(new NhsDDAttribute(name: "ACTIVITY DURATION"))

        activeItem.classRelationships.add(new NhsDDClassRelationship(isKey: true, relationshipDescription: "supplied by", targetClass: new NhsDDClass(name: "ORGANISATION")))
        activeItem.classRelationships.add(new NhsDDClassRelationship(isKey: true, relationshipDescription: "located at", targetClass: new NhsDDClass(name: "ADDRESS")))
    }

    @Override
    void setupRetiredItem() {
        retiredItem = new NhsDDClass(
            catalogueItemId: UUID.fromString("fb096f90-3273-4c66-8023-c1e32ac5b795"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: this.activeItem.definition,
            otherProperties: copyMap(this.activeItem.otherProperties),
            whereUsed: copyMap(this.activeItem.whereUsed))
    }

    @Override
    void setupPreparatoryItem() {
        preparatoryItem = new NhsDDClass(
            catalogueItemId: UUID.fromString("f5276a0c-5458-4fa5-9bd3-ff786aef932f"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: this.activeItem.definition,
            otherProperties: copyMap(this.activeItem.otherProperties),
            whereUsed: copyMap(this.activeItem.whereUsed))
    }

    @Override
    void setupPreviousItems() {

    }

    void "should have the correct active item structure"() {
        when: "the publish structure is built"
        DictionaryItem structure = activeItem.getPublishStructure()

        then: "the expected structure is returned"
        verifyAll {
            structure.state == DictionaryItemState.ACTIVE
            structure.name == activeItem.name
            structure.sections.size() == 6
            structure.sections[0] instanceof DescriptionSection
            structure.sections[1] instanceof ClassAttributeSection
            structure.sections[2] instanceof ClassRelationshipSection
            structure.sections[3] instanceof WhereUsedSection
            structure.sections[4] instanceof AliasesSection
            structure.sections[5] instanceof ChangeLogSection
        }
    }

    void "should have the correct retired item structure"() {
        when: "the publish structure is built"
        DictionaryItem structure = retiredItem.getPublishStructure()

        then: "the expected structure is returned"
        verifyAll {
            structure.state == DictionaryItemState.RETIRED
            structure.name == retiredItem.name
            structure.sections.size() == 2
            structure.sections[0] instanceof DescriptionSection
            structure.sections[1] instanceof ChangeLogSection
        }
    }

    void "should have the correct preparatory item structure"() {
        when: "the publish structure is built"
        DictionaryItem structure = preparatoryItem.getPublishStructure()

        then: "the expected structure is returned"
        verifyAll {
            structure.state == DictionaryItemState.PREPARATORY
            structure.name == preparatoryItem.name
            structure.sections.size() == 2
            structure.sections[0] instanceof DescriptionSection
            structure.sections[1] instanceof ChangeLogSection
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
            ditaXml == """foo"""
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
            ditaXml == """foo"""
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
            ditaXml == """foo"""
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
        String descriptionHtml = structure.sections.find { it instanceof DescriptionSection }.generateHtml(websiteHtmlPublishContext)

        then: "the description is published"
        verifyAll {
            descriptionHtml
            descriptionHtml == """foo"""
        }

        when: "the attributes are converted to html"
        String attributesHtml = structure.sections.find { it instanceof ClassAttributeSection }.generateHtml(websiteHtmlPublishContext)

        then: "the attributes are published"
        verifyAll {
            attributesHtml
            attributesHtml == """foo"""
        }

        when: "the relationships are converted to html"
        String relationshipsHtml = structure.sections.find { it instanceof ClassRelationshipSection }.generateHtml(websiteHtmlPublishContext)

        then: "the relationships are published"
        verifyAll {
            relationshipsHtml
            relationshipsHtml == """foo"""
        }

        when: "the aliases are converted to html"
        String aliasesHtml = structure.sections.find { it instanceof AliasesSection }.generateHtml(websiteHtmlPublishContext)

        then: "the aliases are published"
        verifyAll {
            aliasesHtml
            aliasesHtml == """foo"""
        }

        when: "the where used are converted to html"
        String whereUsedHtml = structure.sections.find { it instanceof WhereUsedSection }.generateHtml(websiteHtmlPublishContext)

        then: "the where used are published"
        verifyAll {
            whereUsedHtml
            whereUsedHtml == """foo"""
        }

        when: "the change log is converted to html"
        String changeLogHtml = structure.sections.find { it instanceof ChangeLogSection }.generateHtml(websiteHtmlPublishContext)

        then: "the change log is published"
        verifyAll {
            changeLogHtml
            changeLogHtml == """foo"""
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
            ditaXml == """foo"""
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
            ditaXml == """foo"""
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
            ditaXml == """foo"""
        }
    }

    void "should produce a diff for all changes to change paper dita"() {
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
            ditaXml == """foo"""
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
            ditaXml == """foo"""
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
            html == """foo"""
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
            html == """foo"""
        }
    }

    void "should produce a diff for all changes to change paper html"() {
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
            html == """foo"""
        }
    }
}
