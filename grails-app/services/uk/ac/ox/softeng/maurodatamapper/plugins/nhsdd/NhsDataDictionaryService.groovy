package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.VersionTreeModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ModelDataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.profiles.DDBusinessDefinitionProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version
import uk.ac.ox.softeng.maurodatamapper.version.VersionChangeType

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
import uk.nhs.digital.maurodatamapper.datadictionary.GenerateDita
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Li
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Topic
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Ul
import uk.nhs.digital.maurodatamapper.datadictionary.importer.PluginImporter

import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Transactional
class NhsDataDictionaryService {

    TerminologyService terminologyService
    DataModelService dataModelService
    FolderService folderService
    VersionedFolderService versionedFolderService
    MetadataService metadataService

    DataSetService dataSetService
    ClassService classService
    ElementService elementService
    AttributeService attributeService

    BusinessDefinitionService businessDefinitionService
    SupportingInformationService supportingInformationService
    XmlSchemaConstraintService xmlSchemaConstraintService

    AuthorityService authorityService
    CodeSetService codeSetService




    def branches(UserSecurityPolicyManager userSecurityPolicyManager) {
        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(DataDictionary.DATA_DICTIONARY_CORE_MODEL_NAME)
        DataModel oldestAncestor = dataModelService.findOldestAncestor(coreModel)

        List<VersionTreeModel> versionTreeModelList = dataModelService.buildModelVersionTree(oldestAncestor, null, userSecurityPolicyManager)
        return versionTreeModelList
    }

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

        Set<String> duplicateAttributes = findDuplicates(dataDictionary.attributes.values().collect{it.name})
        Set<String> duplicateElements = findDuplicates(dataDictionary.elements.values().collect{it.name})
        Set<String> duplicateClasses = findDuplicates(dataDictionary.classes.values().collect{it.name})

        Set<DataDictionaryComponent> foundDuplicates = []
        if(duplicateAttributes.size() > 0) {
            duplicateAttributes.each {attName ->
                foundDuplicates.addAll(dataDictionary.attributes.values().findAll{it.name == attName})
            }
        }
        if(duplicateElements.size() > 0) {
            duplicateElements.each {elemName ->
                foundDuplicates.addAll(dataDictionary.elements.values().findAll{it.name == elemName})
            }
        }
        if(duplicateClasses.size() > 0) {
            duplicateClasses.each {className ->
                foundDuplicates.addAll(dataDictionary.classes.values().findAll{it.name == className})
            }
        }

        List<Map> check7components = foundDuplicates.
                sort{it.name}.collect{it -> outputComponent(it)}

        List<DataDictionaryComponent> prepItems = dataDictionary.elements.values().findAll{it.isPrepatory}
        List<Map> check8components = dataDictionary.dataSets.values().findAll{component ->
            component.catalogueItem.allDataElements.find {dataElement ->
                prepItems.find {it.name == dataElement.label}
            }
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
                ],
                [
                        checkName: "Re-used item names",
                        description: "Check that item names of retired classes, attributes and elements have not been re-used",
                        errors: check7components
                ],
                [
                        checkName: "Datasets that include preparatory items",
                        description: "Check that a dataset doesn't include any preparatory data elements in its definition",
                        errors: check8components
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


    File publishDita(String branchName) {
        DataDictionary dataDictionary = buildDataDictionary(branchName)

        File tempDir = File.createTempDir()
        System.err.println(tempDir.path)
        GenerateDita generateDita = new GenerateDita(dataDictionary, tempDir.path)
        dataDictionary.elements.values().each {ddElement ->
            ddElement.calculateFormatLinkMatchedItem(dataDictionary)
        }
        //generateDita.cleanSourceTarget(options.ditaOutputDir)
        generateDita.generateInput()

        File outputFile = new File(tempDir.path + "/" + "map.ditamap")

        File tempDir2 = File.createTempDir()
        System.err.println(tempDir2.path)

        FileOutputStream fos = new FileOutputStream(tempDir2.path + "/ditaCompressed.zip");
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(tempDir.path);

        zipFile(fileToZip, fileToZip.getName(), zipOut);

        zipOut.close();
        fos.close();

        return new File(tempDir2.path + "/ditaCompressed.zip")

    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    private static <T> Set<T> findDuplicates(Collection<T> collection) {

        Set<T> duplicates = new HashSet<>(1000);
        Set<T> uniques = new HashSet<>();

        for(T t : collection) {
            if(!uniques.add(t)) {
                duplicates.add(t);
            }
        }

        return duplicates;
    }



    @Transactional
    def ingest(User currentUser, def xml, String releaseDate, String finalise, String folderVersionNo, String prevVersion) {
        long startTime = System.currentTimeMillis()
        String dictionaryFolderName = "NHS Data Dictionary (${releaseDate})"
        if(!finalise && !folderVersionNo && !prevVersion) {
            deleteOriginalFolder(dictionaryFolderName)
        }

        long endTime = System.currentTimeMillis()
        log.warn("Delete old folder complete in ${Utils.getTimeString(endTime - startTime)}")
        startTime = endTime
        VersionedFolder dictionaryFolder = new VersionedFolder(authority: authorityService.defaultAuthority, label: dictionaryFolderName,
                                                               description: "", createdBy: currentUser
            .emailAddress)

        VersionedFolder prevDictionaryVersion = null
        if(prevVersion) {
            prevDictionaryVersion = versionedFolderService.get(prevVersion)
            dictionaryFolder.addToVersionLinks(
                linkType: VersionLinkType.NEW_FORK_OF,
                createdBy: currentUser.emailAddress,
                targetModel: prevDictionaryVersion
            )
        }

        dictionaryFolder.save()

        DataModel coreDataModel =
            new DataModel(label: "Data Dictionary Core",
                          description: "Elements, attributes and classes",
                          folder: dictionaryFolder,
                          createdBy: currentUser.emailAddress,
                          authority: authorityService.defaultAuthority,
                          type: DataModelType.DATA_STANDARD)
        if(prevDictionaryVersion) {
            DataModel prevCore = folderService.findAllModelsInFolder(prevDictionaryVersion).find {
                it.label == "Data Dictionary Core"
            }
            coreDataModel.addToVersionLinks(
                linkType: VersionLinkType.NEW_MODEL_VERSION_OF,
                createdBy: currentUser.emailAddress,
                targetModel: prevCore
            )
        }
        NhsDataDictionary nhsDataDictionary = new NhsDataDictionary()
        endTime = System.currentTimeMillis()
        log.warn("Create core model complete in ${Utils.getTimeString(endTime - startTime)}")
        startTime = endTime
        //attributeService.ingestFromXml(xml, dictionaryFolder, coreDataModel, currentUser.emailAddress, nhsDataDictionary)
        endTime = System.currentTimeMillis()
        log.warn("Ingest Attributes complete in ${Utils.getTimeString(endTime - startTime)}")
        startTime = endTime
        //elementService.ingestFromXml(xml, dictionaryFolder, coreDataModel, currentUser.emailAddress, nhsDataDictionary)
        endTime = System.currentTimeMillis()
        log.warn("Ingest Elements complete in ${Utils.getTimeString(endTime - startTime)}")
        startTime = endTime

        //dictionaryFolder.save()
        endTime = System.currentTimeMillis()
        log.warn("Save DD Folder complete in ${Utils.getTimeString(endTime - startTime)}")
        startTime = endTime

        if (coreDataModel.validate()) {
            endTime = System.currentTimeMillis()
            log.warn("Validate core model complete in ${Utils.getTimeString(endTime - startTime)}")
            startTime = endTime
            dataModelService.saveModelWithContent(coreDataModel)
            endTime = System.currentTimeMillis()
            log.warn("Save core model complete in ${Utils.getTimeString(endTime - startTime)}")

        } else throw new ApiInvalidModelException('DMSXX', 'Model is invalid',
                                                  coreDataModel.errors)

        startTime = System.currentTimeMillis()
        // dataSetService.ingestFromXml(xml, dictionaryFolder, coreDataModel, currentUser.emailAddress, nhsDataDictionary)
        endTime = System.currentTimeMillis()
        log.warn("Ingest data models complete in ${Utils.getTimeString(endTime - startTime)}")
        if(finalise == "true") {
            versionedFolderService.finaliseFolder(dictionaryFolder, currentUser, Version.from(folderVersionNo), VersionChangeType.MAJOR, releaseDate)
        }
    }


    void deleteOriginalFolder(String coreFolderName) {
        Folder folder = folderService.findByPath(coreFolderName)
        if(folder) {
            folderService.delete(folder, true, true)
        }
    }

    def codeSystemValidationBundle(User currentUser, String releaseDate) {
        String dictionaryFolderName = "NHS Data Dictionary (${releaseDate})"
        Folder folder = folderService.findByPath(dictionaryFolderName + "/Attribute Terminologies")
        Folder coreFolder = folder.parentFolder
        List entries = []
        DataModel coreDataModel = dataModelService.findByLabel("Data Dictionary Core")
        folder.childFolders.sort {it.label}.eachWithIndex {subFolder, idx1 ->
            folderService.findAllModelsInFolder(subFolder).eachWithIndex {model, idx2 ->
                // Assume a Terminology
                Terminology terminology = (Terminology) model

                DataType linkedAttributeType = coreDataModel.dataTypes.find {
                    it instanceof ModelDataType && ((ModelDataType) it).modelResourceId == terminology.id }
                DataElement linkedAttribute = linkedAttributeType.dataElements.first()

                Map<CodeSet, ModelDataType> codeSetDataTypeMap = [:]
                coreDataModel.dataTypes.findAll {dataType ->
                    dataType instanceof ModelDataType && ((ModelDataType) dataType).modelResourceDomainType == "CodeSet"
                }.each {dataType ->
                    CodeSet codeSet = codeSetService.get(((ModelDataType) dataType).modelResourceId)
                    codeSetDataTypeMap[codeSet] = (ModelDataType) dataType
                }

                List concepts = []
                terminology.terms.each {term ->
                    Metadata retiredMetadata = term.metadata.find{it.namespace == "uk.nhs.datadictionary.term" && it.key == "isRetired" }
                    boolean isRetired = (!retiredMetadata || retiredMetadata.value == "true")
                    Metadata defaultMetadata = term.metadata.find{it.namespace == "uk.nhs.datadictionary.term" && it.key == "isDefault" }
                    boolean isDefault = (!defaultMetadata || defaultMetadata.value == "true")
                    String retiredDate = term.metadata.find{it.namespace == "uk.nhs.datadictionary.term" && it.key == "retiredDate" }?.value
                    Map concept = [
                        code: term.code,
                        display: term.definition,
                        definition: term.definition,
                        property: [
                            [
                                code: "Publish Date",
                                valueDateTime: Instant.now().toString()
                            ],
                            [
                                code: "status",
                                valueCode: isRetired?"retired":"active"
                            ],
                        ]
                    ]
                    if(linkedAttribute && !isDefault) {
                        concept.property << [
                            code: "Data Attribute",
                            valueString: linkedAttribute.label
                        ]
                    }
                    if(retiredDate) {
                        concept.property << [
                            code: "Retired Date",
                            valueDateTime: retiredDate
                        ]
                    }
                    codeSetDataTypeMap.entrySet().each { entry ->
                        CodeSet codeSet = entry.key
                        ModelDataType dataType = entry.value
                        if(codeSet.terms.contains(term)) {
                            dataType.dataElements.each {dataElement ->
                                concept.property << [
                                    code: "Data Element",
                                    valueString: dataElement.label
                                ]
                            }
                        }
                    }

                    concepts << concept

                }
                String id = terminology.label.toLowerCase().replace(" ","_")
                String version = terminology.metadata.find { it.key == "version"}?.value
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                String versionId = LocalDate.now().format(formatter)
                Map resource = [
                    resourceType: "CodeSystem",
                    id: "NHS-Data_Dictionary-CS-" + terminology.label.toLowerCase().replace(" ","-") + "-" + version,
                    meta: [
                        security: [
                               [
                                   system: "http://ontoserver.csiro.au/CodeSystem/ontoserver-permissions",
                                   code: "DDT.read"
                               ],
                               [
                                   system: "http://ontoserver.csiro.au/CodeSystem/ontoserver-permissions",
                                   code: "DDT.write"
                              ]
                        ],
                        versionId: versionId
                    ],
                    language: "en-GB",
                    url: "https://private.datadictionary.nhs.uk/code_system/union/${id}",
                    identifier: [
                        [
                            system: "https://datadictionary.nhs.uk/V3",
                            value: "https://datadictionary.nhs.uk/data_dictionary/attributes/${id}"
                        ]
                    ],
                    version: version,
                    name: terminology.label.toUpperCase().replaceAll("[^A-Za-z0-9]","_"),
                    title: terminology.label,
                    status: "active",
                    experimental: "false",
                    date: Instant.now().toString(),
                    publisher: "NHS Digital",
                    contact: [
                        [
                            name: "NHS Digital Data Model and Dictionary Service",
                            telecom: [
                                [
                                    system: "email",
                                    value: "information.standards@nhs.net"
                                ], [
                                    system: "url",
                                    value: "https://datadictionary.nhs.uk/"
                                ]
                            ]
                        ]
                    ],
                    description: linkedAttribute.description,
                    copyright: "Copyright © NHS Digital",
                    caseSensitive: "false",
                    hierarchyMeaning: "is-a",
                    compositional: "false",
                    versionNeeded: "false",
                    content: "complete",
                    filter: [
                        [
                            code: "Data Element",
                            description: "Filter to identify which Data Elements a concept relates to",
                            operator: ["in", "=", "exists", "not-in"],
                            value: "Data Element"
                        ],
                        [
                            code: "Data Attribute",
                            description: "Filter to identify which Data Attribute a concept relates to",
                            operator: ["in", "=", "exists", "not-in"],
                            value: "Data Attribute"
                        ],
                        [
                            code: "Publish Date",
                            description: "Filter to enable filtering by Publish date/s",
                            operator: ["in", "=", "exists", "not-in"],
                            value: "Publish Date"
                        ],
                        [
                            code: "Retired Date",
                            description: "Filter to enable filtering by date of retirement",
                            operator: ["in", "=", "exists", "not-in"],
                            value: "Retired Date"
                        ],
                        [
                            code: "status",
                            description: "Filter to identify status of a concept",
                            operator: ["in", "=", "not-in"],
                            value: "status"
                        ]
                    ],
                    property: [
                        [
                            code: "Data Element",
                            description: "The Data Element that the concept relates to",
                            type: "string"
                        ],
                        [
                            code: "Data Attribute",
                            description: "The Data Attribute that the concept relates to",
                            type: "string"
                        ],
                        [
                            code: "Publish Date",
                            description: "The date on which the concept was first published and/or updated",
                            type: "dateTime"
                        ],
                        [
                            code: "Retired Date",
                            description: "The date from which the concept was published as retired",
                            type: "dateTime"
                        ],
                        [
                            code: "status",
                            description: "The status of the concept can be one of: draft | active | retired | unknown",
                            uri: "http://hl7.org/fhir/concept-properties#status",
                            type: "code"
                        ],
                    ],
                    count: concepts.size(),
                    concept: concepts
               ]
                   Map entry = [ //fullUrl: "CodeSystem/\$validate",
                              request: [
                                  method: "POST",
                                  url: "CodeSystem/\$validate"
                                  ],
                              resource: [
                                  resourceType: "Parameters",
                                  parameter: [
                                      [name: "Resource",
                                       resource: resource]
                                  ]
                              ]
                ]
                entries << entry
            }
        }
        return [resourceType: "Bundle", type: "transaction", entry: entries]
    }

    def valueSetValidationBundle(User currentUser, String releaseDate) {
        String dictionaryFolderName = "NHS Data Dictionary (${releaseDate})"
        Folder folder = folderService.findByPath(dictionaryFolderName + "/Data Element CodeSets")
        Folder coreFolder = folder.parentFolder
        List entries = []
        DataModel coreDataModel = dataModelService.findByLabel("Data Dictionary Core")
        folder.childFolders.sort {it.label}.eachWithIndex {subFolder, idx1 ->
            folderService.findAllModelsInFolder(subFolder).eachWithIndex {model, idx2 ->
                // Assume a Terminology
                CodeSet codeSet = (CodeSet) model

                DataType linkedElementType = coreDataModel.dataTypes.find {
                    it instanceof ModelDataType && ((ModelDataType) it).modelResourceId == codeSet.id }
                DataElement linkedElement = linkedElementType.dataElements.first()

                Map<Terminology, List<Term>> terminologyListMap = [:]
                codeSet.terms.each {term ->
                    Terminology terminology = term.terminology
                    if(terminologyListMap[terminology]) {
                        terminologyListMap[terminology] << term
                    } else {
                        terminologyListMap[terminology] = [term]
                    }
                }
                List includes = []
                terminologyListMap.entrySet().each { entry ->
                    Terminology terminology = entry.key
                    String terminologyId = terminology.label.toLowerCase().replace(" ","_")
                    String version = terminology.metadata.find { it.key == "version"}?.value
                    List<Term> terms = entry.value
                    def concept = []
                    terms.each {term ->
                        concept << [
                            code: term.code,
                            display: term.definition
                        ]
                    }
                    includes << [
                        system: "https://private.datadictionary.nhs.uk/code_system/union/${terminologyId}",
                        version: version,
                        concept: concept
                    ]
                }
                String id = codeSet.label.toLowerCase().replace(" ","_")
                String version = codeSet.metadata.find { it.key == "version"}?.value
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                String versionId = LocalDate.now().format(formatter)
                Map resource = [
                    resourceType: "ValueSet",
                    id: "NHS-Data_Dictionary-VS-" + codeSet.label.toLowerCase().replace(" ","-") + "-" + version,
                    meta: [
                        security: [
                            [
                                system: "http://ontoserver.csiro.au/CodeSystem/ontoserver-permissions",
                                code: "DDT.read"
                            ],
                            [
                                system: "http://ontoserver.csiro.au/CodeSystem/ontoserver-permissions",
                                code: "DDT.write"
                            ]
                        ],
                        versionId: versionId
                    ],
                    language: "en-GB",
                    url: "https://private.datadictionary.nhs.uk/valueSet/${id}",
                    identifier: [
                        [
                            system: "https://datadictionary.nhs.uk/V3",
                            value: "https://datadictionary.nhs.uk/data_dictionary/data_field_notes/${id}"
                        ]
                    ],
                    version: version,
                    name: codeSet.label.toUpperCase().replaceAll("[^A-Za-z0-9]","_"),
                    title: codeSet.label,
                    status: "active",
                    experimental: "false",
                    date: Instant.now().toString(),
                    publisher: "NHS Digital",
                    contact: [
                        [
                            name: "NHS Digital Data Model and Dictionary Service",
                            telecom: [
                                [
                                    system: "email",
                                    value: "information.standards@nhs.net"
                                ], [
                                    system: "url",
                                    value: "https://datadictionary.nhs.uk/"
                                ]
                            ]
                        ]
                    ],
                    description: linkedElement.description,
                    copyright: "Copyright © NHS Digital",
                    immutable: "true",
                    compose: [
                        include: includes
                    ]
                ]
                Map entry = [ //fullUrl: "CodeSystem/\$validate",
                              request: [
                                  method: "POST",
                                  url: "ValueSet/\$validate"
                              ],
                              resource: [
                                  resourceType: "Parameters",
                                  parameter: [
                                      [name: "Resource",
                                       resource: resource]
                                  ]
                              ]
                ]
                entries << entry
            }
        }
        return [resourceType: "Bundle", type: "transaction", entry: entries]
    }


    def changePaper(User currentUser, String sourceId, String targetId) {
        VersionedFolder sourceVF = versionedFolderService.get(sourceId)
        VersionedFolder targetVF = versionedFolderService.get(targetId)

/*        sourceVF.addToMetadata(new Metadata(key: "type", value: "Data Dictionary Change Notice"))
        sourceVF.addToMetadata(new Metadata(key: "reference", value: "1828"))
        sourceVF.addToMetadata(new Metadata(key: "versionNo", value: "1.0"))
        sourceVF.addToMetadata(new Metadata(key: "subject", value: "NHS England and NHS Improvement"))
        sourceVF.addToMetadata(new Metadata(key: "effectiveDate", value: "Immediate"))
        sourceVF.addToMetadata(new Metadata(key: "reasonForChange", value: "Change to Definitions"))
        sourceVF.addToMetadata(new Metadata(key: "publicationDate", value: "3rd June 2021"))
        sourceVF.addToMetadata(new Metadata(key: "backgroundText", value: backgroundText))
        sourceVF.addToMetadata(new Metadata(key: "sponsor", value: "Nicholas Oughtibridge, Head of Clinical Data Architecture, NHS Digital"))

        sourceVF.metadata.each {md ->
            md.namespace = "uk.nhs.datadictionary.changeNotice"
            md.createdBy = currentUser.emailAddress
        }

        sourceVF.save()
*/

        ObjectDiff od = versionedFolderService.getDiffForVersionedFolders(sourceVF, targetVF)
        System.err.println(od)

        String outputPath = "/Users/james/git/mauro/plugins/mdm-plugin-nhs-data-dictionary/src/main/resources/changePaperOutput"

        File outputDir = new File(outputPath)

        File newFile = new File(outputDir, "test.dita")
        newFile.createNewFile()
    }

    static String backgroundText = "            <p>NHS England and NHS Improvement have worked together since 1 April 2019 and are now known as a " +
                                   "single organisation.</p>\n" +
                                   "            <p>The NHS England and NHS Improvement websites have now merged; therefore changes are required to " +
                                   "the NHS Data Model and Dictionary to support the change.</p>\n" +
                                   "            <p>This Data Dictionary Change Notice (DDCN):</p>\n" +
                                   "            <ul>\n" +
                                   "                <li>Updates the NHS England NHS Business Definition to create a single definition for NHS " +
                                   "England and NHS Improvement</li>\n" +
                                   "                <li>Retires the NHS Improvement NHS Business Definition</li>\n" +
                                   "                <li>Updates all items that reference NHS England and NHS Improvement to reflect the change" +
                                   ".</li>\n" +
                                   "            </ul>"
}