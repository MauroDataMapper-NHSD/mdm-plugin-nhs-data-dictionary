package nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.NhsDataDictionaryService

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDBusinessDefinition
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDChangeLog
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDClass
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSet
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetClass
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetElement
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDElement
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.AliasesSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.ChangeLogSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DescriptionSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItem
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DictionaryItemState
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.WhereUsedSection
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.datasets.DataSetSection

@Integration
@Rollback
class NhsOtherDataSetStructureSpec extends DataDictionaryComponentStructureSpec<NhsDDDataSet> {
    @Autowired
    NhsDataDictionaryService dataDictionaryService

    NhsDDClass relatedPatientClass

    NhsDDElement elementNhsNumber
    NhsDDElement elementPersonGivenName
    NhsDDElement elementPersonBirthDate
    NhsDDElement elementPatientUsualAddress
    NhsDDElement elementDiseaseType

    @Override
    protected NhsDataDictionary createDataDictionary() {
        dataDictionaryService.newDataDictionary()
    }

    @Override
    String getDefinition() {
        """<p>The Diagnostic Data Set contains <a href="dm:Classes and Attributes|dc:PATIENT">PATIENTS</a> holding data.</p>"""
    }

    @Override
    void setupRelatedItems() {
        relatedPatientClass = new NhsDDClass(
            catalogueItemId: UUID.fromString("ae2f2b7b-c136-4cc7-9b71-872ee4efb3a6"),
            branchId: branchId,
            name: "PATIENT")

        // These are the elements for the data set tables
        elementNhsNumber = new NhsDDElement(
            catalogueItemId: UUID.fromString("dcdbc88d-d20e-49a8-bb2b-450bbf56900a"),
            branchId: branchId,
            name: "NHS NUMBER")

        elementPersonGivenName = new NhsDDElement(
            catalogueItemId: UUID.fromString("7e9642c3-14ae-4b59-bd3d-0160ea7f7616"),
            branchId: branchId,
            name: "PERSON GIVEN NAME")

        elementPersonBirthDate = new NhsDDElement(
            catalogueItemId: UUID.fromString("01c9734f-85e4-4fcf-a881-983621414673"),
            branchId: branchId,
            name: "PERSON BIRTH DATE")

        elementPatientUsualAddress = new NhsDDElement(
            catalogueItemId: UUID.fromString("d76f35ec-7fa6-47a3-ae4c-db246714ba8c"),
            branchId: branchId,
            name: "PATIENT USUAL ADDRESS")

        elementDiseaseType = new NhsDDElement(
            catalogueItemId: UUID.fromString("875b1b59-9c9d-452f-9cfb-354409f441a3"),
            branchId: branchId,
            name: "DISEASE TYPE")
    }

    @Override
    protected void setupComponentPathResolver() {
        super.setupComponentPathResolver()

        componentPathResolver.add(relatedPatientClass.getMauroPath(), relatedPatientClass)
        componentPathResolver.add(elementNhsNumber.getMauroPath(), elementNhsNumber)
        componentPathResolver.add(elementPersonGivenName.getMauroPath(), elementPersonGivenName)
        componentPathResolver.add(elementPersonBirthDate.getMauroPath(), elementPersonBirthDate)
        componentPathResolver.add(elementPatientUsualAddress.getMauroPath(), elementPatientUsualAddress)
        componentPathResolver.add(elementDiseaseType.getMauroPath(), elementDiseaseType)
    }

    @Override
    protected void setupCatalogueItemPathResolver() {
        super.setupCatalogueItemPathResolver()

        catalogueItemPathResolver.add(relatedPatientClass.getMauroPath(), relatedPatientClass.catalogueItemId)
        catalogueItemPathResolver.add(elementNhsNumber.getMauroPath(), elementNhsNumber.catalogueItemId)
        catalogueItemPathResolver.add(elementPersonGivenName.getMauroPath(), elementPersonGivenName.catalogueItemId)
        catalogueItemPathResolver.add(elementPersonBirthDate.getMauroPath(), elementPersonBirthDate.catalogueItemId)
        catalogueItemPathResolver.add(elementPatientUsualAddress.getMauroPath(), elementPatientUsualAddress.catalogueItemId)
        catalogueItemPathResolver.add(elementDiseaseType.getMauroPath(), elementDiseaseType.catalogueItemId)
    }

    @Override
    void setupActiveItem() {
        activeItem = new NhsDDDataSet(
            catalogueItemId: UUID.fromString("901c2d3d-0111-41d1-acc9-5b501c1dc397"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: "Diagnostic Data Set",
            definition: definition,
            otherProperties: [
                'aliasPlural': 'Diagnostics Data Set'
            ])

        addWhereUsed(activeItem, activeItem)     // Self reference

        addChangeLog(
            activeItem,
            new NhsDDChangeLog(reference: "CR1000", referenceUrl: "https://test.nhs.uk/change/cr1000", description: "Change 1000", implementationDate: "01 April 2024"),
            new NhsDDChangeLog(reference: "CR2000", referenceUrl: "https://test.nhs.uk/change/cr2000", description: "Change 2000", implementationDate: "01 September 2024"))

        //activeItem.dataSetClasses.add(setupSingleGroupTable())
//        activeItem.dataSetClasses.add(setupMultiGroupTable(false))
//        activeItem.dataSetClasses.add(setupMultiGroupTable(true))
        activeItem.dataSetClasses.add(setupElementChoicesGroup())
    }

    NhsDDDataSetClass setupSingleGroupTable() {
        def table = new NhsDDDataSetClass(
            name: "Single Group",
            description: "This is a single group table"
        )

        table.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 0,
                mandation: "M",
                name: elementNhsNumber.name,
                reuseElement: elementNhsNumber))

        table.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 1,
                mandation: "R",
                name: elementPersonGivenName.name,
                reuseElement: elementPersonGivenName))

        table.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 2,
                mandation: "O",
                name: elementDiseaseType.name,
                reuseElement: elementDiseaseType,
                maxMultiplicity: "-1")) // Multiple occurrences

        table
    }

    NhsDDDataSetClass setupMultiGroupTable(boolean isChoice) {
        String groupType = isChoice ? "Choice" : "Continuous"
        def table = new NhsDDDataSetClass(
            name: "Multi Group: ${groupType}",
            description: "This is a multi group table",
            isChoice: isChoice
        )

        def group1 = new NhsDDDataSetClass(
            name: "GROUP 1",
            description: "The first group"
        )

        group1.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 0,
                mandation: "M",
                name: elementNhsNumber.name,
                reuseElement: elementNhsNumber))

        def group2 = new NhsDDDataSetClass(
            name: "GROUP 2",
            description: "The second group"
        )

        group1.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 0,
                mandation: "R",
                name: elementPersonBirthDate.name,
                reuseElement: elementPersonBirthDate))

        group2.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 1,
                mandation: "M",
                name: elementDiseaseType.name,
                reuseElement: elementDiseaseType,
                maxMultiplicity: "-1")) // Multiple occurrences

        table.dataSetClasses.add(group1)
        table.dataSetClasses.add(group2)

        table
    }

    NhsDDDataSetClass setupElementChoicesGroup() {
        def table = new NhsDDDataSetClass(
            name: "Element Choices",
            description: "This is a group of element choices")

        // OR
        table.dataSetClasses.add(
            new NhsDDDataSetClass(
                webOrder: 0,
                name: "Choice OR",
                mandation: "M",
                isChoice: true,
                dataSetElements: [
                    new NhsDDDataSetElement(
                        webOrder: 0,
                        name: elementNhsNumber.name,
                        reuseElement: elementNhsNumber),
                    new NhsDDDataSetElement(
                        webOrder: 1,
                        name: elementPersonGivenName.name,
                        reuseElement: elementPersonGivenName)
                ]
            )
        )

        // AND
        table.dataSetClasses.add(
            new NhsDDDataSetClass(
                webOrder: 1,
                name: "Choice AND",
                mandation: "M",
                isAnd: true,
                dataSetElements: [
                    new NhsDDDataSetElement(
                        webOrder: 0,
                        name: elementPersonGivenName.name,
                        reuseElement: elementPersonGivenName),
                    new NhsDDDataSetElement(
                        webOrder: 1,
                        name: elementDiseaseType.name,
                        reuseElement: elementDiseaseType,
                        maxMultiplicity: "-1") // Multiple occurrences
                ]
            )
        )

        // AND/OR
        table.dataSetClasses.add(
            new NhsDDDataSetClass(
                webOrder: 2,
                name: "Choice AND/OR",
                mandation: "M",
                isInclusiveOr: true,
                dataSetElements: [
                    new NhsDDDataSetElement(
                        webOrder: 0,
                        name: elementNhsNumber.name,
                        reuseElement: elementNhsNumber),
                    new NhsDDDataSetElement(
                        webOrder: 1,
                        name: elementPersonGivenName.name,
                        reuseElement: elementPersonGivenName),
                    new NhsDDDataSetElement(
                        webOrder: 2,
                        name: elementDiseaseType.name,
                        reuseElement: elementDiseaseType,
                        maxMultiplicity: "-1") // Multiple occurrences
                ]
            )
        )

        // ADDRESS CHOICE
        table.dataSetClasses.add(
            new NhsDDDataSetClass(
                webOrder: 3,
                name: "Choice ADDRESS",
                mandation: "R",
                isAddress: true,
                address1: "https://datadictionary.nhs.uk/classes/address_structured.html",
                address2: "https://datadictionary.nhs.uk/classes/address_unstructured.html",
                dataSetElements: [
                    new NhsDDDataSetElement(
                        webOrder: 0,
                        name: elementPatientUsualAddress.name,
                        reuseElement: elementPatientUsualAddress)
                ]
            )
        )

        table
    }

    @Override
    void setupRetiredItem() {
        retiredItem = new NhsDDDataSet(
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
        preparatoryItem = new NhsDDDataSet(
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
        // TODO: setup changes to test
    }

    void "should have the correct active item structure"() {
        when: "the publish structure is built"
        DictionaryItem structure = activeItem.getPublishStructure()

        then: "the expected structure is returned"
        verifyAll {
            structure.state == DictionaryItemState.ACTIVE
            structure.name == activeItem.name
            structure.sections.size() == 5
            structure.sections[0] instanceof DescriptionSection
            structure.sections[1] instanceof DataSetSection
            structure.sections[2] instanceof AliasesSection
            structure.sections[3] instanceof WhereUsedSection
            structure.sections[4] instanceof ChangeLogSection
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
            ditaXml == """<topic id='data_set_diagnostic_data_set_retired'>
  <title outputclass='dataSet retired'>
    <text>Diagnostic Data Set (Retired)</text>
  </title>
  <shortdesc>The Diagnostic Data Set contains 
PATIENTS holding data</shortdesc>
  <topic id='data_set_diagnostic_data_set_retired_description'>
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
  <topic id='data_set_diagnostic_data_set_retired_changeLog'>
    <title>Change Log</title>
    <body />
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
            ditaXml == """<topic id='data_set_diagnostic_data_set'>
  <title outputclass='dataSet'>
    <text>Diagnostic Data Set</text>
  </title>
  <shortdesc>This item is being used for development purposes and has not yet been approved.</shortdesc>
  <topic id='data_set_diagnostic_data_set_description'>
    <title>Description</title>
    <body>
      <div>
        <p>This item is being used for development purposes and has not yet been approved.</p>
      </div>
    </body>
  </topic>
  <topic id='data_set_diagnostic_data_set_changeLog'>
    <title>Change Log</title>
    <body />
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
            titleHtml == """<h1 class="title topictitle1 dataSet">Diagnostic Data Set</h1>
<div class="- topic/body body">
  <p class="- topic/shortdesc shortdesc">The Diagnostic Data Set contains 
PATIENTS holding data</p>
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
    <p class="- topic/p p"><p>The Diagnostic Data Set contains <a class="class" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/class/ae2f2b7b-c136-4cc7-9b71-872ee4efb3a6">PATIENTS</a> holding data.</p></p>
  </div>
</div>"""
        }

        when: "the data set is converted to html"
        DataSetSection dataSetSection = structure.sections.find { it instanceof DataSetSection } as DataSetSection
        String dataSetHtml = dataSetSection.generateHtml(websiteHtmlPublishContext)

        then: "the aliases are published"
        verifyAll {
            dataSetHtml
            dataSetHtml == """foo"""
        }
    }

    void "should produce no diff when items are the same"() {
        given: "the publish structures are built"
        DictionaryItem previousStructure = activeItem.getPublishStructure()
        DictionaryItem currentStructure = activeItem.getPublishStructure()

        when: "a diff is produced"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "no diff is returned"
        verifyAll {
            !diff
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

    // TODO: add tests to verify diffs on specification
}
