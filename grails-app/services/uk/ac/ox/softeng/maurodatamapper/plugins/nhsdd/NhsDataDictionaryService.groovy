package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles.DDBusinessDefinitionProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService

import grails.gorm.transactions.Transactional
import uk.nhs.digital.maurodatamapper.datadictionary.DDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.DDBusinessDefinition
import uk.nhs.digital.maurodatamapper.datadictionary.DDClass
import uk.nhs.digital.maurodatamapper.datadictionary.DDDataSet
import uk.nhs.digital.maurodatamapper.datadictionary.DDElement
import uk.nhs.digital.maurodatamapper.datadictionary.DDSupportingDefinition
import uk.nhs.digital.maurodatamapper.datadictionary.DDXmlSchemaConstraint
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionaryComponent
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionaryOptions
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Li
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Topic
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Ul
import uk.nhs.digital.maurodatamapper.datadictionary.importer.PluginImporter

@Transactional
class NhsDataDictionaryService {

    TerminologyService terminologyService
    DataModelService dataModelService
    FolderService folderService
    MetadataService metadataService

    DataSetService dataSetService
    ClassService classService
    ElementService elementService
    AttributeService attributeService

    BusinessDefinitionService businessDefinitionService
    SupportingInformationService supportingInformationService
    XmlSchemaConstraintService xmlSchemaConstraintService


    def statistics(String branchName) {
        DataDictionary dataDictionary = buildDataDictionary(branchName)

        return [
                "Attributes"      : [
                        "Total"      : dataDictionary.attributes.size(),
                        "Preparatory": dataDictionary.attributes.values().findAll { it.isPrepatory }.size(),
                        "Retired"    : dataDictionary.attributes.values().findAll { it.isRetired }.size()
                ],
                "Data Field Notes": [
                        "Total"      : dataDictionary.elements.size(),
                        "Preparatory": dataDictionary.elements.values().findAll { it.isPrepatory }.size(),
                        "Retired"    : dataDictionary.elements.values().findAll { it.isRetired }.size()
                ],
                "Classes"         : [
                        "Total"      : dataDictionary.classes.size(),
                        "Preparatory": dataDictionary.classes.values().findAll { it.isPrepatory }.size(),
                        "Retired"    : dataDictionary.classes.values().findAll { it.isRetired }.size()
                ],
                "Data Sets" : [
                        "Total"      : dataDictionary.dataSets.size(),
                        "Preparatory": dataDictionary.dataSets.values().findAll { it.isPrepatory }.size(),
                        "Retired"    : dataDictionary.dataSets.values().findAll { it.isRetired }.size()
                ],
                "Business Definitions" : [
                        "Total"      : dataDictionary.businessDefinitions.size(),
                        "Preparatory": dataDictionary.businessDefinitions.values().findAll { it.isPrepatory }.size(),
                        "Retired"    : dataDictionary.businessDefinitions.values().findAll { it.isRetired }.size()
                ],
                "Supporting Information" : [
                        "Total"      : dataDictionary.supportingInformation.size(),
                        "Preparatory": dataDictionary.supportingInformation.values().findAll { it.isPrepatory }.size(),
                        "Retired"    : dataDictionary.supportingInformation.values().findAll { it.isRetired }.size()
                ],
                "XML Schema Constraints" : [
                        "Total"      : dataDictionary.xmlSchemaConstraints.size(),
                        "Preparatory": dataDictionary.xmlSchemaConstraints.values().findAll { it.isPrepatory }.size(),
                        "Retired"    : dataDictionary.xmlSchemaConstraints.values().findAll { it.isRetired }.size()
                ]



        ]


    }

    Map outputComponent(DataDictionaryComponent ddComponent) {
        return [
                type: ddComponent.typeText,
                label: ddComponent.name,
                id: ddComponent.catalogueId.toString(),
                domainType: ddComponent.catalogueItem.domainType,
                parentId: ddComponent.parentCatalogueId.toString(),
                modelId: ddComponent.containerCatalogueId.toString()

        ]
    }


    def integrityChecks(String branchName) {

        DataDictionary dataDictionary = buildDataDictionary()

        List<Map> check1components = dataDictionary.classes.values().findAll{ddClass ->
            !ddClass.isRetired && ddClass.classLinks.size() == 0
        }.sort{it.name}.collect{it -> outputComponent(it)}

        List<Map> check2components = dataDictionary.attributes.values().findAll{ddAttribute ->
            // System.err.println(ddAttribute.classLinks.size())
            !ddAttribute.isRetired &&
                    !dataDictionary.classes.values().any{cls ->
                        cls.attributes.keySet().find{att -> att.name == ddAttribute.name }
                    }
        }.sort{it.name}.collect{it -> outputComponent(it)}

        List<Map> check3components = dataDictionary.elements.values().findAll{ddElement ->
            // System.err.println(ddAttribute.classLinks.size())
            !ddElement.isRetired &&
            ddElement.getElementAttributes(dataDictionary) == []
        }.sort{it.name}.collect{it -> outputComponent(it)}

        List<Map> check4components = dataDictionary.dataSets.values().findAll{ddDataSet ->
            // System.err.println(ddAttribute.classLinks.size())
            !ddDataSet.isRetired &&
                    (ddDataSet.stringValues["overview"] == null || ddDataSet.stringValues["overview"] == "")
        }.sort{it.name}.collect{it -> outputComponent(it)}

        List<Map> check5components = dataDictionary.getAllComponents().findAll{component ->
            String shortDesc = component.getShortDesc(dataDictionary)
            (shortDesc == null || shortDesc == "")
        }.sort{it.name}.collect{it -> outputComponent(it)}

        List<Map> check6components = dataDictionary.getAllComponents().findAll{component ->
            !component.isRetired &&
            component.hasNoAliases()
        }.sort{it.name}.collect{it -> outputComponent(it)}

        return [
                [
                    checkName: "Class Relationships Defined",
                    description: "Check that all live classes have relationships to other classes defined",
                    errors: check1components
                ],
                [
                    checkName: "Attributes linked to a class",
                    description: "Check that all live attributes are linked to a class",
                    errors: check2components
                ],
                [
                    checkName: "Elements linked to an attribute",
                    description: "Check that all live elements are linked to an attribute",
                    errors: check3components
                ],
                [
                    checkName: "Data Sets have an overview",
                    description: "Check that all live datasets have an overview field",
                    errors: check4components
                ],
                [
                        checkName: "All items have a short description",
                        description: "Check that all items have a short description",
                        errors: check5components
                ],
                [
                        checkName: "All items have an alias",
                        description: "Check that all items have one of the alias fields completed",
                        errors: check6components
                ]

        ]
    }




    DataDictionary buildDataDictionary(String branchName) {
        DataDictionary dataDictionary = new DataDictionary()

        Terminology busDefTerminology = terminologyService.findCurrentMainBranchByLabel(DataDictionary.BUSDEF_TERMINOLOGY_NAME)
        Terminology supDefTerminology = terminologyService.findCurrentMainBranchByLabel(DataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME)
        Terminology xmlSchemaConstraintsTerminology = terminologyService.findCurrentMainBranchByLabel(DataDictionary.XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME)

        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(DataDictionary.DATA_DICTIONARY_CORE_MODEL_NAME)

        Folder folder = folderService.findByPath(DataDictionary.DATA_DICTIONARY_FOLDER_NAME.toString())
        Folder dataSetsFolder = folder.childFolders.find { it.label == DataDictionary.DATA_DICTIONARY_DATA_SETS_FOLDER_NAME }

        addAttributesToDictionary(coreModel, dataDictionary)
        addElementsToDictionary(coreModel, dataDictionary)
        addClassesToDictionary(coreModel, dataDictionary)

        addDataSetsToDictionary(dataSetsFolder, dataDictionary)

        addBusDefsToDictionary(busDefTerminology, dataDictionary)
        addSupDefsToDictionary(supDefTerminology, dataDictionary)
        addXmlSchemaConstraintsToDictionary(xmlSchemaConstraintsTerminology, dataDictionary)

        return dataDictionary
    }

    void addAttributesToDictionary(DataModel coreModel, DataDictionary dataDictionary) {
        DataClass attributesClass = coreModel.dataClasses.find { it.label == "Attributes" }
        DataClass retiredAttributesClass = attributesClass.dataClasses.find { it.label == "Retired" }

        Set<DataElement> attributeElements = []
        attributeElements.addAll(attributesClass.dataElements)
        attributeElements.addAll(retiredAttributesClass.dataElements)

        attributeElements.each { dataElement ->
            DDAttribute ddAttribute = new DDAttribute()
            ddAttribute.fromCatalogueItem(dataDictionary, dataElement, attributesClass.id, coreModel.id, metadataService)
            dataDictionary.attributes[ddAttribute.uin] = ddAttribute
        }

    }

    void addElementsToDictionary(DataModel coreModel, DataDictionary dataDictionary) {
        DataClass elementsClass = coreModel.dataClasses.find { it.label == "Data Field Notes" }
        DataClass retiredElementsClass = elementsClass.dataClasses.find { it.label == "Retired" }

        Set<DataElement> elementElements = []
        elementElements.addAll(elementsClass.dataElements)
        elementElements.addAll(retiredElementsClass.dataElements)

        elementElements.each { dataElement ->
            DDElement ddElement = new DDElement()
            ddElement.fromCatalogueItem(dataDictionary, dataElement, elementsClass.id, coreModel.id, metadataService)
            dataDictionary.elements[ddElement.uin] = ddElement
        }

    }

    void addClassesToDictionary(DataModel coreModel, DataDictionary dataDictionary) {
        DataClass classesClass = coreModel.dataClasses.find { it.label == "Classes" }
        DataClass retiredClassesClass = classesClass.dataClasses.find { it.label == "Retired" }

        Set<DataClass> classClasses = []
        classClasses.addAll(classesClass.dataClasses)
        classClasses.addAll(retiredClassesClass.dataClasses)

        classClasses.each { dataClass ->
            DDClass ddClass = new DDClass()
            ddClass.fromCatalogueItem(dataDictionary, dataClass, classesClass.id, coreModel.id, metadataService)
            dataDictionary.classes[ddClass.uin] = ddClass
        }

    }

    void addDataSetsToDictionary(Folder dataSetsFolder, DataDictionary dataDictionary) {
        Set<DataModel> dataSetModels = dataSetService.getAllDataSets(dataSetsFolder)

        dataSetModels.each { dataModel ->
            DDDataSet ddDataSet = new DDDataSet()
            ddDataSet.fromCatalogueItem(dataDictionary, dataModel, null, null, metadataService)
            dataDictionary.dataSets[ddDataSet.uin] = ddDataSet
        }
    }

    void addBusDefsToDictionary(Terminology busDefsTerminology, DataDictionary dataDictionary) {
        busDefsTerminology.terms.each { term ->
            DDBusinessDefinition ddBusinessDefinition = new DDBusinessDefinition()
            ddBusinessDefinition.fromCatalogueItem(dataDictionary, term, busDefsTerminology.id, busDefsTerminology.id, metadataService)
            dataDictionary.businessDefinitions[ddBusinessDefinition.uin] = ddBusinessDefinition
        }
    }

    void addSupDefsToDictionary(Terminology supDefsTerminology, DataDictionary dataDictionary) {
        supDefsTerminology.terms.each { term ->
            DDSupportingDefinition ddSupportingDefinition = new DDSupportingDefinition()
            ddSupportingDefinition.fromCatalogueItem(dataDictionary, term, supDefsTerminology.id, supDefsTerminology.id, metadataService)
            dataDictionary.supportingInformation[ddSupportingDefinition.uin] = ddSupportingDefinition
        }
    }

    void addXmlSchemaConstraintsToDictionary(Terminology xmlSchemaConstraintsTerminology, DataDictionary dataDictionary) {
        xmlSchemaConstraintsTerminology.terms.each { term ->
            DDXmlSchemaConstraint ddXmlSchemaConstraint = new DDXmlSchemaConstraint()
            ddXmlSchemaConstraint.fromCatalogueItem(dataDictionary, term, xmlSchemaConstraintsTerminology.id, xmlSchemaConstraintsTerminology.id, metadataService)
            dataDictionary.xmlSchemaConstraints[ddXmlSchemaConstraint.uin] = ddXmlSchemaConstraint
        }
    }


    Set<DataClass> getDataClasses(includeRetired = false) {
        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(DataDictionary.DATA_DICTIONARY_CORE_MODEL_NAME)

        DataClass classesClass = coreModel.dataClasses.find { it.label == DataDictionary.DATA_DICTIONARY_DATA_CLASSES_CLASS_NAME }

        Set<DataClass> classClasses = []
        classClasses.addAll(classesClass.dataClasses.findAll {it.label != "Retired"})
        if(includeRetired) {
            DataClass retiredClassesClass = classesClass.dataClasses.find { it.label == "Retired" }
            classClasses.addAll(retiredClassesClass.dataClasses)
        }

        return classClasses

    }

    Set<DataElement> getDataFieldNotes(includeRetired = false) {
        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(DataDictionary.DATA_DICTIONARY_CORE_MODEL_NAME)

        DataClass elementsClass = coreModel.dataClasses.find { it.label == DataDictionary.DATA_DICTIONARY_DATA_FIELD_NOTES_CLASS_NAME }

        Set<DataElement> elementElements = []
        elementElements.addAll(elementsClass.dataElements)
        if(includeRetired) {
            DataClass retiredElementsClass = elementsClass.dataClasses.find { it.label == "Retired" }
            elementElements.addAll(retiredElementsClass.dataElements)
        }
        return elementElements
    }

    Set<DataElement> getAttributes(includeRetired = false) {
        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(DataDictionary.DATA_DICTIONARY_CORE_MODEL_NAME)

        DataClass attributesClass = coreModel.dataClasses.find { it.label == DataDictionary.DATA_DICTIONARY_ATTRIBUTES_CLASS_NAME }

        Set<DataElement> attributeElements = []
        attributeElements.addAll(attributesClass.dataElements)
        if(includeRetired) {
            DataClass retiredAttributesClass = attributesClass.dataClasses.find { it.label == "Retired" }
            attributeElements.addAll(retiredAttributesClass.dataElements)
        }
        return attributeElements
    }

    List<Map> allItemsIndex() {
        List<Map> allItems = []

        allItems.addAll(dataSetService.index())
        allItems.addAll(classService.index())
        allItems.addAll(elementService.index())
        allItems.addAll(attributeService.index())

        allItems.addAll(businessDefinitionService.index())
        allItems.addAll(supportingInformationService.index())
        allItems.addAll(xmlSchemaConstraintService.index())


        return allItems.sort{it.name }
    }




}