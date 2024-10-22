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

    // The data sets below are for testing diffs
    NhsDDDataSet previousCellsChange
    NhsDDDataSet currentCellsChange

    NhsDDDataSet previousRowsChange
    NhsDDDataSet currentRowsChange

    NhsDDDataSet previousGroupsChange
    NhsDDDataSet currentGroupsChange

    NhsDDDataSet previousTablesChange
    NhsDDDataSet currentTablesChange

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

        activeItem.dataSetClasses.add(setupSingleGroupTable())
        activeItem.dataSetClasses.add(setupMultiGroupTable(false))
        activeItem.dataSetClasses.add(setupMultiGroupTable(true))
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

        group1.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 0,
                mandation: "R",
                name: elementPersonBirthDate.name,
                reuseElement: elementPersonBirthDate))

        def group2 = new NhsDDDataSetClass(
            name: "GROUP 2",
            description: "The second group"
        )

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

        // A table/group needs at least one Data Element listed for the choice classes to be found
        // correctly
        table.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 0,
                mandation: "M",
                name: elementPersonGivenName.name,
                reuseElement: elementPersonGivenName))

        // OR
        table.dataSetClasses.add(
            new NhsDDDataSetClass(
                webOrder: 1,
                name: "Choice",
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
                webOrder: 2,
                name: "Choice",
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
                webOrder: 3,
                name: "Choice",
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
                webOrder: 4,
                name: "Address Choice",
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
        previousItemDescriptionChange = new NhsDDDataSet(
            catalogueItemId: UUID.fromString("22710e00-7c41-4335-97da-2cafe9728804"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: "The previous description",
            otherProperties: copyMap(this.activeItem.otherProperties))

        previousItemAliasesChange = new NhsDDDataSet(
            catalogueItemId: UUID.fromString("8049682f-761f-4eab-b533-c00781615207"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: this.activeItem.definition,
            otherProperties: [
                'aliasPlural': "Baby's First Feeds",
                'aliasAlsoKnownAs': 'Baby Food',
            ])

        previousItemAllChange = new NhsDDDataSet(
            catalogueItemId: UUID.fromString("eeb8929f-819a-4cfe-9209-1e9867fa2b68"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: this.activeItem.name,
            definition: previousItemDescriptionChange.definition,
            otherProperties: copyMap(previousItemAliasesChange.otherProperties))

        // Now make simple data sets to test a variety of specification changes
        setupChangedCellsDataSets()
        setupChangedRowsDataSets()
        setupChangedGroupsDataSets()
        setupChangedTablesDataSets()
    }

    void setupChangedCellsDataSets() {
        String name = "Cells Change Data Set"
        String definition = "The definition"

        previousCellsChange = new NhsDDDataSet(
            catalogueItemId: UUID.fromString("e55b9656-3747-4ea9-92ed-cd98c2e0413b"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: name,
            definition: definition
        )

        def previousTable = new NhsDDDataSetClass(
            name: "Table",
            description: "The table"
        )

        previousTable.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 0,
                mandation: "M",
                name: elementNhsNumber.name,
                reuseElement: elementNhsNumber))

        previousTable.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 1,
                mandation: "O",
                name: elementDiseaseType.name,
                reuseElement: elementDiseaseType,
                maxMultiplicity: "-1")) // Multiple occurrences

        previousCellsChange.dataSetClasses.add(previousTable)

        currentCellsChange = new NhsDDDataSet(
            catalogueItemId: UUID.fromString("fb2dfd75-952b-428b-8165-f7cc0b7eb9bb"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: name,
            definition: definition
        )

        def currentTable = new NhsDDDataSetClass(
            name: previousTable.name,
            description: previousTable.description  // No change to header
        )

        currentTable.dataSetClasses.add(
            new NhsDDDataSetClass(  // Change: Replace NHS NUMBER with an "Or" element choice
                webOrder: 0,
                name: "Choice",
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

        currentTable.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 1,
                mandation: "O",
                name: elementDiseaseType.name,
                reuseElement: elementDiseaseType))  // Change: No multiplicity anymore

        currentCellsChange.dataSetClasses.add(currentTable)
    }

    void setupChangedRowsDataSets() {
        String name = "Rows Change Data Set"
        String definition = "The definition"

        previousRowsChange = new NhsDDDataSet(
            catalogueItemId: UUID.fromString("85b5b447-f946-49b6-8928-05cb9d32f8c3"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: name,
            definition: definition
        )

        def previousTable = new NhsDDDataSetClass(
            name: "Table",
            description: "The table"
        )

        previousTable.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 0,
                mandation: "M",
                name: elementNhsNumber.name,
                reuseElement: elementNhsNumber))

        previousTable.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 1,
                mandation: "R",
                name: elementPersonGivenName.name,
                reuseElement: elementPersonGivenName))

        previousTable.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 2,
                mandation: "O",
                name: elementDiseaseType.name,
                reuseElement: elementDiseaseType,
                maxMultiplicity: "-1")) // Multiple occurrences

        previousRowsChange.dataSetClasses.add(previousTable)

        currentRowsChange = new NhsDDDataSet(
            catalogueItemId: UUID.fromString("d1de1670-965b-49ff-aab2-027b059570d4"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: name,
            definition: definition
        )

        def currentTable = new NhsDDDataSetClass(
            name: previousTable.name,
            description: previousTable.description  // No change to header
        )

        currentTable.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 0,
                mandation: "M",
                name: elementPersonBirthDate.name,
                reuseElement: elementPersonBirthDate))  // Change: New row

        currentTable.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 1,
                mandation: "M",
                name: elementNhsNumber.name,
                reuseElement: elementNhsNumber))    // Unchanged

        // Change: Removed "PERSON GIVEN NAME"

        currentTable.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 1,
                mandation: "M", // Change: Modify mandation
                name: elementDiseaseType.name,
                reuseElement: elementDiseaseType,
                maxMultiplicity: "-1")) // Multiple occurrences

        currentRowsChange.dataSetClasses.add(currentTable)
    }

    void setupChangedGroupsDataSets() {
        String name = "Groups Change Data Set"
        String definition = "The definition"

        previousGroupsChange = new NhsDDDataSet(
            catalogueItemId: UUID.fromString("f9546c65-77f3-4f44-9711-f6106f94ee0c"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: name,
            definition: definition
        )

        def previousTable = new NhsDDDataSetClass(
            name: "Table",
            description: "The table",
            isChoice: true
        )

        def previousGroup1 = new NhsDDDataSetClass(
            name: "GROUP 1",
            description: "The first group"
        )

        previousGroup1.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 0,
                mandation: "M",
                name: elementNhsNumber.name,
                reuseElement: elementNhsNumber))

        def previousGroup2 = new NhsDDDataSetClass(
            name: "GROUP 2",
            description: "The second group"
        )

        previousGroup2.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 0,
                mandation: "M",
                name: elementDiseaseType.name,
                reuseElement: elementDiseaseType))

        previousTable.dataSetClasses.add(previousGroup1)
        previousTable.dataSetClasses.add(previousGroup2)

        previousGroupsChange.dataSetClasses.add(previousTable)

        currentGroupsChange = new NhsDDDataSet(
            catalogueItemId: UUID.fromString("08a60808-9d38-4b2d-9b9b-8f1a870f339f"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: name,
            definition: definition
        )

        def currentTable = new NhsDDDataSetClass(
            name: "Table",
            description: "The table",
            isChoice: true
        )

        // Change: Removed GROUP 1

        def currentGroup2 = new NhsDDDataSetClass(
            name: "GROUP 2",
            description: "The second group has changed" // Change: Header modified
        )

        currentGroup2.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 0,
                mandation: "M",
                name: elementDiseaseType.name,
                reuseElement: elementDiseaseType))  // No change

        def currentGroup3 = new NhsDDDataSetClass(
            name: "GROUP 3",
            description: "The second group has changed" // Change: New group
        )

        currentGroup3.dataSetElements.add(
            new NhsDDDataSetElement(
                webOrder: 0,
                mandation: "M",
                name: elementPersonGivenName.name,
                reuseElement: elementPersonGivenName))

        currentTable.dataSetClasses.add(currentGroup2)
        currentTable.dataSetClasses.add(currentGroup3)

        currentGroupsChange.dataSetClasses.add(currentTable)
    }

    void setupChangedTablesDataSets() {
        String name = "Tables Change Data Set"
        String definition = "The definition"

        previousTablesChange = new NhsDDDataSet(
            catalogueItemId: UUID.fromString("112e2106-a8cd-4b08-9a82-82121d3dfa24"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: name,
            definition: definition
        )

        def previousTable1 = new NhsDDDataSetClass(
            name: "Table 1",
            description: "The first table",
            dataSetElements: [
                new NhsDDDataSetElement(
                    webOrder: 0,
                    mandation: "M",
                    name: elementNhsNumber.name,
                    reuseElement: elementNhsNumber)
            ]
        )

        def previousTable2 = new NhsDDDataSetClass(
            name: "Table 2",
            description: "The second table",
            dataSetElements: [
                new NhsDDDataSetElement(
                    webOrder: 0,
                    mandation: "M",
                    name: elementPersonGivenName.name,
                    reuseElement: elementPersonGivenName)
            ]
        )

        previousTablesChange.dataSetClasses.add(previousTable1)
        previousTablesChange.dataSetClasses.add(previousTable2)

        currentTablesChange = new NhsDDDataSet(
            catalogueItemId: UUID.fromString("011ddcdb-da6e-48fb-9e5f-1dace51ef542"),
            branchId: branchId,
            dataDictionary: dataDictionary,
            name: name,
            definition: definition
        )

        // Change: Remove TABLE 1

        def currentTable2 = new NhsDDDataSetClass(
            name: "Table 2",
            description: "The second table has changed",    // Change: Header modified
            dataSetElements: [
                new NhsDDDataSetElement(
                    webOrder: 0,
                    mandation: "M",
                    name: elementPersonGivenName.name,
                    reuseElement: elementPersonGivenName)
            ]
        )

        def currentTable3 = new NhsDDDataSetClass(
            name: "Table 3",
            description: "The third table",    // Change: New table
            dataSetElements: [
                new NhsDDDataSetElement(
                    webOrder: 0,
                    mandation: "M",
                    name: elementPersonBirthDate.name,
                    reuseElement: elementPersonBirthDate)
            ]
        )

        currentTablesChange.dataSetClasses.add(currentTable2)
        currentTablesChange.dataSetClasses.add(currentTable3)
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
            ditaXml == """<topic id='data_set_diagnostic_data_set'>
  <title outputclass='dataSet'>
    <text>Diagnostic Data Set</text>
  </title>
  <shortdesc>The Diagnostic Data Set contains 
PATIENTS holding data</shortdesc>
  <topic id='data_set_diagnostic_data_set_description'>
    <title>Description</title>
    <body>
      <div>
        <p>The Diagnostic Data Set contains 

          <xref outputclass='class' keyref='class_patient' scope='local'>PATIENTS</xref> holding data.
        </p>
      </div>
    </body>
  </topic>
  <topic id='data_set_diagnostic_data_set_specification'>
    <title>Specification</title>
    <body>
      <div>
        <table outputclass='table table-sm table-striped table-bordered'>
          <tgroup cols='2'>
            <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
            <colspec align='left' colnum='2' colname='col2' colwidth='8*' />
            <thead>
              <row outputclass='thead-light'>
                <entry namest='col1' nameend='col2'>
                  <b>Single Group</b>
                  <p>This is a single group table</p>
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
                    <xref outputclass='element' keyref='data_element_nhs_number' format='html'>NHS NUMBER</xref>
                  </p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>R</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_person_given_name' format='html'>PERSON GIVEN NAME</xref>
                  </p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>O</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_disease_type' format='html'>DISEASE TYPE</xref>
                  </p>
                  <p>Multiple occurrences of this item are permitted</p>
                </entry>
              </row>
            </tbody>
          </tgroup>
        </table>
      </div>
      <div>
        <table outputclass='table table-sm table-striped table-bordered'>
          <tgroup cols='2'>
            <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
            <colspec align='left' colnum='2' colname='col2' colwidth='8*' />
            <thead>
              <row outputclass='thead-light'>
                <entry namest='col1' nameend='col2'>
                  <b>Multi Group: Continuous</b>
                  <p>This is a multi group table</p>
                </entry>
              </row>
            </thead>
            <tbody>
              <row outputclass='table-primary'>
                <entry namest='col1' nameend='col1'>
                  <p>
                    <b>Mandation</b>
                  </p>
                </entry>
                <entry namest='col2' nameend='col2'>
                  <b>GROUP 1</b>
                  <p>The first group</p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>M</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_nhs_number' format='html'>NHS NUMBER</xref>
                  </p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>R</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_person_birth_date' format='html'>PERSON BIRTH DATE</xref>
                  </p>
                </entry>
              </row>
              <row>
                <entry namest='col1' nameend='col2'>
                  <p />
                </entry>
              </row>
              <row outputclass='table-primary'>
                <entry namest='col1' nameend='col1'>
                  <p>
                    <b>Mandation</b>
                  </p>
                </entry>
                <entry namest='col2' nameend='col2'>
                  <b>GROUP 2</b>
                  <p>The second group</p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>M</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_disease_type' format='html'>DISEASE TYPE</xref>
                  </p>
                  <p>Multiple occurrences of this item are permitted</p>
                </entry>
              </row>
            </tbody>
          </tgroup>
        </table>
      </div>
      <div>
        <table outputclass='table table-sm table-striped table-bordered'>
          <tgroup cols='2'>
            <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
            <colspec align='left' colnum='2' colname='col2' colwidth='8*' />
            <thead>
              <row outputclass='thead-light'>
                <entry namest='col1' nameend='col2'>
                  <b>Multi Group: Choice</b>
                  <p>This is a multi group table</p>
                </entry>
              </row>
            </thead>
            <tbody>
              <row outputclass='table-primary'>
                <entry namest='col1' nameend='col1'>
                  <p>
                    <b>Mandation</b>
                  </p>
                </entry>
                <entry namest='col2' nameend='col2'>
                  <b>GROUP 1</b>
                  <p>The first group</p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>M</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_nhs_number' format='html'>NHS NUMBER</xref>
                  </p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>R</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_person_birth_date' format='html'>PERSON BIRTH DATE</xref>
                  </p>
                </entry>
              </row>
              <row>
                <entry namest='col1' nameend='col2'>
                  <b>Or</b>
                </entry>
              </row>
              <row outputclass='table-primary'>
                <entry namest='col1' nameend='col1'>
                  <p>
                    <b>Mandation</b>
                  </p>
                </entry>
                <entry namest='col2' nameend='col2'>
                  <b>GROUP 2</b>
                  <p>The second group</p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>M</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_disease_type' format='html'>DISEASE TYPE</xref>
                  </p>
                  <p>Multiple occurrences of this item are permitted</p>
                </entry>
              </row>
            </tbody>
          </tgroup>
        </table>
      </div>
      <div>
        <table outputclass='table table-sm table-striped table-bordered'>
          <tgroup cols='2'>
            <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
            <colspec align='left' colnum='2' colname='col2' colwidth='8*' />
            <thead>
              <row outputclass='thead-light'>
                <entry namest='col1' nameend='col2'>
                  <b>Element Choices</b>
                  <p>This is a group of element choices</p>
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
                    <xref outputclass='element' keyref='data_element_person_given_name' format='html'>PERSON GIVEN NAME</xref>
                  </p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>M</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_nhs_number' format='html'>NHS NUMBER</xref>
                  </p>
                  <p>Or</p>
                  <p>
                    <xref outputclass='element' keyref='data_element_person_given_name' format='html'>PERSON GIVEN NAME</xref>
                  </p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>M</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_person_given_name' format='html'>PERSON GIVEN NAME</xref>
                  </p>
                  <p>And</p>
                  <p>
                    <xref outputclass='element' keyref='data_element_disease_type' format='html'>DISEASE TYPE</xref>
                  </p>
                  <p>Multiple occurrences of this item are permitted</p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>M</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_nhs_number' format='html'>NHS NUMBER</xref>
                  </p>
                  <p>And/Or</p>
                  <p>
                    <xref outputclass='element' keyref='data_element_person_given_name' format='html'>PERSON GIVEN NAME</xref>
                  </p>
                  <p>And/Or</p>
                  <p>
                    <xref outputclass='element' keyref='data_element_disease_type' format='html'>DISEASE TYPE</xref>
                  </p>
                  <p>Multiple occurrences of this item are permitted</p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>R</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_patient_usual_address' format='html'>PATIENT USUAL ADDRESS</xref>
                    <text> - </text>
                    <xref outputclass='class' href='https://datadictionary.nhs.uk/classes/address_structured.html' format='html' scope='external'>ADDRESS STRUCTURED</xref>
                  </p>
                  <p>Or</p>
                  <p>
                    <xref outputclass='element' keyref='data_element_patient_usual_address' format='html'>PATIENT USUAL ADDRESS</xref>
                    <text> - </text>
                    <xref outputclass='class' href='https://datadictionary.nhs.uk/classes/address_unstructured.html' format='html' scope='external'>ADDRESS UNSTRUCTURED</xref>
                  </p>
                </entry>
              </row>
            </tbody>
          </tgroup>
        </table>
      </div>
    </body>
  </topic>
  <topic id='data_set_diagnostic_data_set_aliases'>
    <title>Also Known As</title>
    <body>
      <p>This Data Set is also known by these names:</p>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='1* 2*'>
        <sthead outputclass='thead-light'>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry>Plural</stentry>
          <stentry>Diagnostics Data Set</stentry>
        </strow>
      </simpletable>
    </body>
  </topic>
  <topic id='data_set_diagnostic_data_set_whereUsed'>
    <title>Where Used</title>
    <body>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='1* 3* 2*'>
        <sthead outputclass='thead-light'>
          <stentry>Type</stentry>
          <stentry>Link</stentry>
          <stentry>How used</stentry>
        </sthead>
        <strow>
          <stentry>Data Set</stentry>
          <stentry>
            <xref outputclass='dataSet' keyref='data_set_diagnostic_data_set' format='html'>Diagnostic Data Set</xref>
          </stentry>
          <stentry>references in description Diagnostic Data Set</stentry>
        </strow>
      </simpletable>
    </body>
  </topic>
  <topic id='data_set_diagnostic_data_set_changeLog'>
    <title>Change Log</title>
    <body>
      <div>
        <p>Click on the links below to view the change requests this item is part of:</p>
      </div>
      <simpletable outputclass='table table-sm table-striped' relcolwidth='2* 5* 3*'>
        <sthead outputclass='thead-light'>
          <stentry>Change Request</stentry>
          <stentry>Change Request Description</stentry>
          <stentry>Implementation Date</stentry>
        </sthead>
        <strow>
          <stentry>
            <xref href='https://test.nhs.uk/change/cr1000' format='html' scope='external'>CR1000</xref>
          </stentry>
          <stentry>Change 1000</stentry>
          <stentry>01 April 2024</stentry>
        </strow>
        <strow>
          <stentry>
            <xref href='https://test.nhs.uk/change/cr2000' format='html' scope='external'>CR2000</xref>
          </stentry>
          <stentry>Change 2000</stentry>
          <stentry>01 September 2024</stentry>
        </strow>
      </simpletable>
      <div>
        <p>Click 

          <xref outputclass='- topic/xref xref' href='https://www.datadictionary.nhs.uk/archive' format='html' scope='external'>here</xref> to see the Change Log Information for changes before January 2025.
        </p>
      </div>
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
            dataSetHtml == """<div class="- topic/body body">
  <div class="table-container">
    <table class="- topic/table table table-striped table-bordered table-sm">
      <colgroup>
        <col style="width: 20%" />
        <col style="width: 80%" />
      </colgroup>
      <thead class="- topic/thead thead">
        <tr class="- topic/row thead-light">
          <th colspan="2" class="- topic/entry entry align-center">
            <b>Single Group</b>
            <p class="- topic/p p">This is a single group table</p>
          </th>
        </tr>
        <tr class="- topic/row">
          <th class="- topic/entry entry align-center">
            <p class="- topic/p p">Mandation</p>
          </th>
          <th class="- topic/entry entry">
            <p class="- topic/p p">Data Elements</p>
          </th>
        </tr>
      </thead>
      <tbody class="- topic/tbody tbody">
        <tr class="- topic/row">
          <td class="- topic/entry entry">
            <p class="- topic/p p">M</p>
          </td>
          <td class="- topic/entry entry">
            <p class="- topic/p p">
              <a class="element" title="NHS NUMBER" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/dcdbc88d-d20e-49a8-bb2b-450bbf56900a">NHS NUMBER</a>
            </p>
          </td>
        </tr>
        <tr class="- topic/row">
          <td class="- topic/entry entry">
            <p class="- topic/p p">R</p>
          </td>
          <td class="- topic/entry entry">
            <p class="- topic/p p">
              <a class="element" title="PERSON GIVEN NAME" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/7e9642c3-14ae-4b59-bd3d-0160ea7f7616">PERSON GIVEN NAME</a>
            </p>
          </td>
        </tr>
        <tr class="- topic/row">
          <td class="- topic/entry entry">
            <p class="- topic/p p">O</p>
          </td>
          <td class="- topic/entry entry">
            <p class="- topic/p p">
              <a class="element" title="DISEASE TYPE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/875b1b59-9c9d-452f-9cfb-354409f441a3">DISEASE TYPE</a>
            </p>
            <p class="- topic/p p">Multiple occurrences of this item are permitted</p>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
  <div class="table-container">
    <table class="- topic/table table table-striped table-bordered table-sm">
      <colgroup>
        <col style="width: 20%" />
        <col style="width: 80%" />
      </colgroup>
      <thead class="- topic/thead thead">
        <tr class="- topic/row thead-light">
          <th colspan="2" class="- topic/entry entry align-center">
            <b>Multi Group: Continuous</b>
            <p class="- topic/p p">This is a multi group table</p>
          </th>
        </tr>
      </thead>
      <tbody class="- topic/tbody tbody">
        <tr class="- topic/row table-primary">
          <td class="- topic/entry entry align-center">
            <b>Mandation</b>
          </td>
          <td class="- topic/entry entry">
            <b>GROUP 1</b>
            <p class="- topic/p p">The first group</p>
          </td>
        </tr>
        <tr class="- topic/row">
          <td class="- topic/entry entry">
            <p class="- topic/p p">M</p>
          </td>
          <td class="- topic/entry entry">
            <p class="- topic/p p">
              <a class="element" title="NHS NUMBER" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/dcdbc88d-d20e-49a8-bb2b-450bbf56900a">NHS NUMBER</a>
            </p>
          </td>
        </tr>
        <tr class="- topic/row">
          <td class="- topic/entry entry">
            <p class="- topic/p p">R</p>
          </td>
          <td class="- topic/entry entry">
            <p class="- topic/p p">
              <a class="element" title="PERSON BIRTH DATE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/01c9734f-85e4-4fcf-a881-983621414673">PERSON BIRTH DATE</a>
            </p>
          </td>
        </tr>
        <tr class="- topic/row">
          <td class="- topic/entry entry align-center" colspan="2">
            <b></b>
          </td>
        </tr>
        <tr class="- topic/row table-primary">
          <td class="- topic/entry entry align-center">
            <b>Mandation</b>
          </td>
          <td class="- topic/entry entry">
            <b>GROUP 2</b>
            <p class="- topic/p p">The second group</p>
          </td>
        </tr>
        <tr class="- topic/row">
          <td class="- topic/entry entry">
            <p class="- topic/p p">M</p>
          </td>
          <td class="- topic/entry entry">
            <p class="- topic/p p">
              <a class="element" title="DISEASE TYPE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/875b1b59-9c9d-452f-9cfb-354409f441a3">DISEASE TYPE</a>
            </p>
            <p class="- topic/p p">Multiple occurrences of this item are permitted</p>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
  <div class="table-container">
    <table class="- topic/table table table-striped table-bordered table-sm">
      <colgroup>
        <col style="width: 20%" />
        <col style="width: 80%" />
      </colgroup>
      <thead class="- topic/thead thead">
        <tr class="- topic/row thead-light">
          <th colspan="2" class="- topic/entry entry align-center">
            <b>Multi Group: Choice</b>
            <p class="- topic/p p">This is a multi group table</p>
          </th>
        </tr>
      </thead>
      <tbody class="- topic/tbody tbody">
        <tr class="- topic/row table-primary">
          <td class="- topic/entry entry align-center">
            <b>Mandation</b>
          </td>
          <td class="- topic/entry entry">
            <b>GROUP 1</b>
            <p class="- topic/p p">The first group</p>
          </td>
        </tr>
        <tr class="- topic/row">
          <td class="- topic/entry entry">
            <p class="- topic/p p">M</p>
          </td>
          <td class="- topic/entry entry">
            <p class="- topic/p p">
              <a class="element" title="NHS NUMBER" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/dcdbc88d-d20e-49a8-bb2b-450bbf56900a">NHS NUMBER</a>
            </p>
          </td>
        </tr>
        <tr class="- topic/row">
          <td class="- topic/entry entry">
            <p class="- topic/p p">R</p>
          </td>
          <td class="- topic/entry entry">
            <p class="- topic/p p">
              <a class="element" title="PERSON BIRTH DATE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/01c9734f-85e4-4fcf-a881-983621414673">PERSON BIRTH DATE</a>
            </p>
          </td>
        </tr>
        <tr class="- topic/row">
          <td class="- topic/entry entry align-center" colspan="2">
            <b>Or</b>
          </td>
        </tr>
        <tr class="- topic/row table-primary">
          <td class="- topic/entry entry align-center">
            <b>Mandation</b>
          </td>
          <td class="- topic/entry entry">
            <b>GROUP 2</b>
            <p class="- topic/p p">The second group</p>
          </td>
        </tr>
        <tr class="- topic/row">
          <td class="- topic/entry entry">
            <p class="- topic/p p">M</p>
          </td>
          <td class="- topic/entry entry">
            <p class="- topic/p p">
              <a class="element" title="DISEASE TYPE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/875b1b59-9c9d-452f-9cfb-354409f441a3">DISEASE TYPE</a>
            </p>
            <p class="- topic/p p">Multiple occurrences of this item are permitted</p>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
  <div class="table-container">
    <table class="- topic/table table table-striped table-bordered table-sm">
      <colgroup>
        <col style="width: 20%" />
        <col style="width: 80%" />
      </colgroup>
      <thead class="- topic/thead thead">
        <tr class="- topic/row thead-light">
          <th colspan="2" class="- topic/entry entry align-center">
            <b>Element Choices</b>
            <p class="- topic/p p">This is a group of element choices</p>
          </th>
        </tr>
        <tr class="- topic/row">
          <th class="- topic/entry entry align-center">
            <p class="- topic/p p">Mandation</p>
          </th>
          <th class="- topic/entry entry">
            <p class="- topic/p p">Data Elements</p>
          </th>
        </tr>
      </thead>
      <tbody class="- topic/tbody tbody">
        <tr class="- topic/row">
          <td class="- topic/entry entry">
            <p class="- topic/p p">M</p>
          </td>
          <td class="- topic/entry entry">
            <p class="- topic/p p">
              <a class="element" title="PERSON GIVEN NAME" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/7e9642c3-14ae-4b59-bd3d-0160ea7f7616">PERSON GIVEN NAME</a>
            </p>
          </td>
        </tr>
        <tr class="- topic/row">
          <td class="- topic/entry entry">
            <p class="- topic/p p">M</p>
          </td>
          <td class="- topic/entry entry">
            <p class="- topic/p p">
              <a class="element" title="NHS NUMBER" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/dcdbc88d-d20e-49a8-bb2b-450bbf56900a">NHS NUMBER</a>
            </p>
            <p class="- topic/p p">Or</p>
            <p class="- topic/p p">
              <a class="element" title="PERSON GIVEN NAME" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/7e9642c3-14ae-4b59-bd3d-0160ea7f7616">PERSON GIVEN NAME</a>
            </p>
          </td>
        </tr>
        <tr class="- topic/row">
          <td class="- topic/entry entry">
            <p class="- topic/p p">M</p>
          </td>
          <td class="- topic/entry entry">
            <p class="- topic/p p">
              <a class="element" title="PERSON GIVEN NAME" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/7e9642c3-14ae-4b59-bd3d-0160ea7f7616">PERSON GIVEN NAME</a>
            </p>
            <p class="- topic/p p">And</p>
            <p class="- topic/p p">
              <a class="element" title="DISEASE TYPE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/875b1b59-9c9d-452f-9cfb-354409f441a3">DISEASE TYPE</a>
            </p>
            <p class="- topic/p p">Multiple occurrences of this item are permitted</p>
          </td>
        </tr>
        <tr class="- topic/row">
          <td class="- topic/entry entry">
            <p class="- topic/p p">M</p>
          </td>
          <td class="- topic/entry entry">
            <p class="- topic/p p">
              <a class="element" title="NHS NUMBER" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/dcdbc88d-d20e-49a8-bb2b-450bbf56900a">NHS NUMBER</a>
            </p>
            <p class="- topic/p p">And/Or</p>
            <p class="- topic/p p">
              <a class="element" title="PERSON GIVEN NAME" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/7e9642c3-14ae-4b59-bd3d-0160ea7f7616">PERSON GIVEN NAME</a>
            </p>
            <p class="- topic/p p">And/Or</p>
            <p class="- topic/p p">
              <a class="element" title="DISEASE TYPE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/875b1b59-9c9d-452f-9cfb-354409f441a3">DISEASE TYPE</a>
            </p>
            <p class="- topic/p p">Multiple occurrences of this item are permitted</p>
          </td>
        </tr>
        <tr class="- topic/row">
          <td class="- topic/entry entry">
            <p class="- topic/p p">R</p>
          </td>
          <td class="- topic/entry entry">
            <p class="- topic/p p">
              <a class="element" title="PATIENT USUAL ADDRESS" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/d76f35ec-7fa6-47a3-ae4c-db246714ba8c">PATIENT USUAL ADDRESS</a> - 
              <a class="class" title="ADDRESS STRUCTURED" href="https://datadictionary.nhs.uk/classes/address_structured.html">ADDRESS STRUCTURED</a>
            </p>
            <p class="- topic/p p">Or</p>
            <p class="- topic/p p">
              <a class="element" title="PATIENT USUAL ADDRESS" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/d76f35ec-7fa6-47a3-ae4c-db246714ba8c">PATIENT USUAL ADDRESS</a> - 
              <a class="class" title="ADDRESS UNSTRUCTURED" href="https://datadictionary.nhs.uk/classes/address_unstructured.html">ADDRESS UNSTRUCTURED</a>
            </p>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</div>"""
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
            ditaXml == """<topic id='data_set_diagnostic_data_set'>
  <title>
    <text>Diagnostic Data Set</text>
  </title>
  <shortdesc>Change to Data Set: New</shortdesc>
  <body>
    <div outputclass='new'>
      <div>
        <p>The Diagnostic Data Set contains 

          <xref outputclass='class' keyref='class_patient' scope='local'>PATIENTS</xref> holding data.
        </p>
      </div>
    </div>
    <div>
      <div>
        <table outputclass='new'>
          <tgroup cols='2'>
            <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
            <colspec align='left' colnum='2' colname='col2' colwidth='8*' />
            <thead>
              <row>
                <entry namest='col1' nameend='col2'>
                  <b>Single Group</b>
                  <p>This is a single group table</p>
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
                    <xref outputclass='element' keyref='data_element_nhs_number' format='html'>NHS NUMBER</xref>
                  </p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>R</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_person_given_name' format='html'>PERSON GIVEN NAME</xref>
                  </p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>O</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_disease_type' format='html'>DISEASE TYPE</xref>
                  </p>
                  <p>Multiple occurrences of this item are permitted</p>
                </entry>
              </row>
            </tbody>
          </tgroup>
        </table>
      </div>
      <div>
        <table outputclass='new'>
          <tgroup cols='2'>
            <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
            <colspec align='left' colnum='2' colname='col2' colwidth='8*' />
            <thead>
              <row>
                <entry namest='col1' nameend='col2'>
                  <b>Multi Group: Continuous</b>
                  <p>This is a multi group table</p>
                </entry>
              </row>
            </thead>
            <tbody>
              <row outputclass='table-primary'>
                <entry namest='col1' nameend='col1'>
                  <p>
                    <b>Mandation</b>
                  </p>
                </entry>
                <entry namest='col2' nameend='col2'>
                  <b>GROUP 1</b>
                  <p>The first group</p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>M</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_nhs_number' format='html'>NHS NUMBER</xref>
                  </p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>R</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_person_birth_date' format='html'>PERSON BIRTH DATE</xref>
                  </p>
                </entry>
              </row>
              <row>
                <entry namest='col1' nameend='col2'>
                  <p />
                </entry>
              </row>
              <row outputclass='table-primary'>
                <entry namest='col1' nameend='col1'>
                  <p>
                    <b>Mandation</b>
                  </p>
                </entry>
                <entry namest='col2' nameend='col2'>
                  <b>GROUP 2</b>
                  <p>The second group</p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>M</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_disease_type' format='html'>DISEASE TYPE</xref>
                  </p>
                  <p>Multiple occurrences of this item are permitted</p>
                </entry>
              </row>
            </tbody>
          </tgroup>
        </table>
      </div>
      <div>
        <table outputclass='new'>
          <tgroup cols='2'>
            <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
            <colspec align='left' colnum='2' colname='col2' colwidth='8*' />
            <thead>
              <row>
                <entry namest='col1' nameend='col2'>
                  <b>Multi Group: Choice</b>
                  <p>This is a multi group table</p>
                </entry>
              </row>
            </thead>
            <tbody>
              <row outputclass='table-primary'>
                <entry namest='col1' nameend='col1'>
                  <p>
                    <b>Mandation</b>
                  </p>
                </entry>
                <entry namest='col2' nameend='col2'>
                  <b>GROUP 1</b>
                  <p>The first group</p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>M</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_nhs_number' format='html'>NHS NUMBER</xref>
                  </p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>R</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_person_birth_date' format='html'>PERSON BIRTH DATE</xref>
                  </p>
                </entry>
              </row>
              <row>
                <entry namest='col1' nameend='col2'>
                  <b>Or</b>
                </entry>
              </row>
              <row outputclass='table-primary'>
                <entry namest='col1' nameend='col1'>
                  <p>
                    <b>Mandation</b>
                  </p>
                </entry>
                <entry namest='col2' nameend='col2'>
                  <b>GROUP 2</b>
                  <p>The second group</p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>M</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_disease_type' format='html'>DISEASE TYPE</xref>
                  </p>
                  <p>Multiple occurrences of this item are permitted</p>
                </entry>
              </row>
            </tbody>
          </tgroup>
        </table>
      </div>
      <div>
        <table outputclass='new'>
          <tgroup cols='2'>
            <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
            <colspec align='left' colnum='2' colname='col2' colwidth='8*' />
            <thead>
              <row>
                <entry namest='col1' nameend='col2'>
                  <b>Element Choices</b>
                  <p>This is a group of element choices</p>
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
                    <xref outputclass='element' keyref='data_element_person_given_name' format='html'>PERSON GIVEN NAME</xref>
                  </p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>M</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_nhs_number' format='html'>NHS NUMBER</xref>
                  </p>
                  <p>Or</p>
                  <p>
                    <xref outputclass='element' keyref='data_element_person_given_name' format='html'>PERSON GIVEN NAME</xref>
                  </p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>M</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_person_given_name' format='html'>PERSON GIVEN NAME</xref>
                  </p>
                  <p>And</p>
                  <p>
                    <xref outputclass='element' keyref='data_element_disease_type' format='html'>DISEASE TYPE</xref>
                  </p>
                  <p>Multiple occurrences of this item are permitted</p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>M</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_nhs_number' format='html'>NHS NUMBER</xref>
                  </p>
                  <p>And/Or</p>
                  <p>
                    <xref outputclass='element' keyref='data_element_person_given_name' format='html'>PERSON GIVEN NAME</xref>
                  </p>
                  <p>And/Or</p>
                  <p>
                    <xref outputclass='element' keyref='data_element_disease_type' format='html'>DISEASE TYPE</xref>
                  </p>
                  <p>Multiple occurrences of this item are permitted</p>
                </entry>
              </row>
              <row>
                <entry>
                  <p>R</p>
                </entry>
                <entry>
                  <p>
                    <xref outputclass='element' keyref='data_element_patient_usual_address' format='html'>PATIENT USUAL ADDRESS</xref>
                    <text> - </text>
                    <xref outputclass='class' href='https://datadictionary.nhs.uk/classes/address_structured.html' format='html' scope='external'>ADDRESS STRUCTURED</xref>
                  </p>
                  <p>Or</p>
                  <p>
                    <xref outputclass='element' keyref='data_element_patient_usual_address' format='html'>PATIENT USUAL ADDRESS</xref>
                    <text> - </text>
                    <xref outputclass='class' href='https://datadictionary.nhs.uk/classes/address_unstructured.html' format='html' scope='external'>ADDRESS UNSTRUCTURED</xref>
                  </p>
                </entry>
              </row>
            </tbody>
          </tgroup>
        </table>
      </div>
    </div>
    <div>
      <p>This Data Set is also known by these names:</p>
      <simpletable relcolwidth='1* 2*'>
        <sthead>
          <stentry>Context</stentry>
          <stentry>Alias</stentry>
        </sthead>
        <strow>
          <stentry outputclass='new'>Plural</stentry>
          <stentry outputclass='new'>Diagnostics Data Set</stentry>
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
            ditaXml == """<topic id='data_set_diagnostic_data_set'>
  <title>
    <text>Diagnostic Data Set</text>
  </title>
  <shortdesc>Change to Data Set: Updated description</shortdesc>
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
            ditaXml == """<topic id='data_set_diagnostic_data_set_retired'>
  <title>
    <text>Diagnostic Data Set</text>
  </title>
  <shortdesc>Change to Data Set: Retired</shortdesc>
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
  <h3>Diagnostic Data Set</h3>
  <h4>Change to Data Set: Updated description</h4>
  <div>
    <div>
      <p>The <span class="diff-html-removed" id="removed-diff-0" previous="first-diff" changeId="removed-diff-0" next="added-diff-0">previous </span><span class="diff-html-added" id="added-diff-0" previous="removed-diff-0" changeId="added-diff-0" next="last-diff">current </span>description</p>
    </div>
  </div>
</div>"""
        }
    }

    void "should produce a diff for an updated specification (cells change) to change paper dita"() {
        given: "the publish structures are built"
        DictionaryItem previousStructure = previousCellsChange.getPublishStructure()
        DictionaryItem currentStructure = currentCellsChange.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted"
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

    void "should produce a diff for an updated specification (cells change) to change paper html"() {
        given: "the publish structures are built"
        DictionaryItem previousStructure = previousCellsChange.getPublishStructure()
        DictionaryItem currentStructure = currentCellsChange.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted"
        String html = diff.generateHtml(changePaperHtmlPublishContext)

        then: "the expected output is published"
        verifyAll {
            html
            html == """foo"""
        }
    }

    void "should produce a diff for an updated specification (rows change) to change paper dita"() {
        given: "the publish structures are built"
        DictionaryItem previousStructure = previousRowsChange.getPublishStructure()
        DictionaryItem currentStructure = currentRowsChange.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted"
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

    void "should produce a diff for an updated specification (rows change) to change paper html"() {
        given: "the publish structures are built"
        DictionaryItem previousStructure = previousRowsChange.getPublishStructure()
        DictionaryItem currentStructure = currentRowsChange.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted"
        String html = diff.generateHtml(changePaperHtmlPublishContext)

        then: "the expected output is published"
        verifyAll {
            html
            html == """foo"""
        }
    }

    void "should produce a diff for an updated specification (groups change) to change paper dita"() {
        given: "the publish structures are built"
        DictionaryItem previousStructure = previousGroupsChange.getPublishStructure()
        DictionaryItem currentStructure = currentGroupsChange.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted"
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

    void "should produce a diff for an updated specification (groups change) to change paper html"() {
        given: "the publish structures are built"
        DictionaryItem previousStructure = previousGroupsChange.getPublishStructure()
        DictionaryItem currentStructure = currentGroupsChange.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted"
        String html = diff.generateHtml(changePaperHtmlPublishContext)

        then: "the expected output is published"
        verifyAll {
            html
            html == """foo"""
        }
    }

    void "should produce a diff for an updated specification (tables change) to change paper dita"() {
        given: "the publish structures are built"
        DictionaryItem previousStructure = previousTablesChange.getPublishStructure()
        DictionaryItem currentStructure = currentTablesChange.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted"
        Topic dita = diff.generateDita(changePaperDitaPublishContext)
        verifyAll {
            dita
        }

        then: "the expected output is published"
        String ditaXml = dita.toXmlString()
        verifyAll {
            ditaXml == """<topic id='data_set_tables_change_data_set'>
  <title>
    <text>Tables Change Data Set</text>
  </title>
  <shortdesc>Change to Data Set: Specification</shortdesc>
  <body>
    <div>
      <div>
        <table>
          <tgroup cols='2'>
            <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
            <colspec align='left' colnum='2' colname='col2' colwidth='8*' />
            <thead>
              <row>
                <entry namest='col1' nameend='col2'>
                  <b>Table 2</b>
                  <p>The second table has changed</p>
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
                    <xref outputclass='element' keyref='data_element_person_given_name' format='html'>PERSON GIVEN NAME</xref>
                  </p>
                </entry>
              </row>
            </tbody>
          </tgroup>
        </table>
      </div>
      <div>
        <table outputclass='new'>
          <tgroup cols='2'>
            <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
            <colspec align='left' colnum='2' colname='col2' colwidth='8*' />
            <thead>
              <row>
                <entry namest='col1' nameend='col2'>
                  <b>Table 3</b>
                  <p>The third table</p>
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
                    <xref outputclass='element' keyref='data_element_person_birth_date' format='html'>PERSON BIRTH DATE</xref>
                  </p>
                </entry>
              </row>
            </tbody>
          </tgroup>
        </table>
      </div>
      <div>
        <table outputclass='deleted'>
          <tgroup cols='2'>
            <colspec align='center' colnum='1' colname='col1' colwidth='2*' />
            <colspec align='left' colnum='2' colname='col2' colwidth='8*' />
            <thead>
              <row>
                <entry namest='col1' nameend='col2'>
                  <b>Table 1</b>
                  <p>The first table</p>
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
                    <xref outputclass='element' keyref='data_element_nhs_number' format='html'>NHS NUMBER</xref>
                  </p>
                </entry>
              </row>
            </tbody>
          </tgroup>
        </table>
      </div>
    </div>
  </body>
</topic>"""
        }
    }

    void "should produce a diff for an updated specification (tables change) to change paper html"() {
        given: "the publish structures are built"
        DictionaryItem previousStructure = previousTablesChange.getPublishStructure()
        DictionaryItem currentStructure = currentTablesChange.getPublishStructure()

        when: "a diff is produced against the previous item"
        DictionaryItem diff = currentStructure.produceDiff(previousStructure)

        then: "a diff exists"
        verifyAll {
            diff
        }

        when: "the diff structure is converted"
        String html = diff.generateHtml(changePaperHtmlPublishContext)

        then: "the expected output is published"
        verifyAll {
            html
            html == """<div>
  <h3>Tables Change Data Set</h3>
  <h4>Change to Data Set: Specification</h4>
  <div>
    <div>
      <table>
        <colgroup>
          <col style="width: 20%" />
          <col style="width: 80%" />
        </colgroup>
        <thead>
          <tr>
            <th colspan="2" class=" align-center">
              <b>Table 2</b>
              <p>The second table has changed</p>
            </th>
          </tr>
          <tr>
            <th class=" align-center">
              <p>Mandation</p>
            </th>
            <th>
              <p>Data Elements</p>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>
              <p>M</p>
            </td>
            <td>
              <p>
                <a class="element" title="PERSON GIVEN NAME" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/7e9642c3-14ae-4b59-bd3d-0160ea7f7616">PERSON GIVEN NAME</a>
              </p>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div>
      <table class="new">
        <colgroup>
          <col style="width: 20%" />
          <col style="width: 80%" />
        </colgroup>
        <thead>
          <tr>
            <th colspan="2" class=" align-center">
              <b>Table 3</b>
              <p>The third table</p>
            </th>
          </tr>
          <tr>
            <th class=" align-center">
              <p>Mandation</p>
            </th>
            <th>
              <p>Data Elements</p>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>
              <p>M</p>
            </td>
            <td>
              <p>
                <a class="element" title="PERSON BIRTH DATE" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/01c9734f-85e4-4fcf-a881-983621414673">PERSON BIRTH DATE</a>
              </p>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div>
      <table class="deleted">
        <colgroup>
          <col style="width: 20%" />
          <col style="width: 80%" />
        </colgroup>
        <thead>
          <tr>
            <th colspan="2" class=" align-center">
              <b>Table 1</b>
              <p>The first table</p>
            </th>
          </tr>
          <tr>
            <th class=" align-center">
              <p>Mandation</p>
            </th>
            <th>
              <p>Data Elements</p>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>
              <p>M</p>
            </td>
            <td>
              <p>
                <a class="element" title="NHS NUMBER" href="#/preview/782602d4-e153-45d8-a271-eb42396804da/element/dcdbc88d-d20e-49a8-bb2b-450bbf56900a">NHS NUMBER</a>
              </p>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</div>"""
        }
    }
}
