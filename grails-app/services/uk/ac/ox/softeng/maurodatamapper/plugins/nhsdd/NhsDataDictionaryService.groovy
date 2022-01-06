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
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.VersionTreeModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ModelDataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version
import uk.ac.ox.softeng.maurodatamapper.version.VersionChangeType

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import uk.nhs.digital.maurodatamapper.datadictionary.GenerateDita
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDClassRelationship
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.AllClassesHaveRelationships
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.AllItemsHaveAlias
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.AllItemsHaveShortDescription
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.AttributesLinkedToAClass
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.DataSetsHaveAnOverview
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.ElementsLinkedToAnAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.IntegrityCheck
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.ReusedItemNames
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.utils.StereotypedCatalogueItem

import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Slf4j
@Transactional
class NhsDataDictionaryService {

    TerminologyService terminologyService
    DataModelService dataModelService
    FolderService folderService
    VersionedFolderService versionedFolderService
    MetadataService metadataService
    TermService termService

    DataSetService dataSetService
    ClassService classService
    ElementService elementService
    AttributeService attributeService

    BusinessDefinitionService businessDefinitionService
    SupportingInformationService supportingInformationService
    XmlSchemaConstraintService xmlSchemaConstraintService

    AuthorityService authorityService
    CodeSetService codeSetService
    SessionFactory sessionFactory


    def branches(UserSecurityPolicyManager userSecurityPolicyManager) {
        List<VersionedFolder> versionedFolders = VersionedFolder.findAll().findAll {
            it.label.startsWith("NHS Data Dictionary")
        }

        VersionedFolder oldestAncestor = versionedFolderService.findOldestAncestor(versionedFolders[0])

        List<VersionTreeModel> versionTreeModelList = versionedFolderService.buildModelVersionTree(oldestAncestor, null, null,
                                                                                                   true, userSecurityPolicyManager)
        return versionTreeModelList
    }

    List<IntegrityCheck> integrityChecks(UUID versionedFolderId) {

        NhsDataDictionary dataDictionary = buildDataDictionary(versionedFolderId)

        List<Class<IntegrityCheck>> integrityCheckClasses = [
            AllClassesHaveRelationships,
            AttributesLinkedToAClass,
            ElementsLinkedToAnAttribute,
            DataSetsHaveAnOverview,
            AllItemsHaveShortDescription,
            AllItemsHaveAlias,
            ReusedItemNames
        ]

        List<IntegrityCheck> integrityChecks = integrityCheckClasses.collect {checkClass ->
            IntegrityCheck integrityCheck = checkClass.getDeclaredConstructor().newInstance()
            integrityCheck.runCheck(dataDictionary)
            integrityCheck.sortErrors()
            integrityCheck
        }

        return integrityChecks
    }


    NhsDataDictionary buildDataDictionary(UUID versionedFolderId) {
        long totalStart = System.currentTimeMillis()
        NhsDataDictionary dataDictionary = new NhsDataDictionary()

        VersionedFolder thisVersionedFolder = versionedFolderService.get(versionedFolderId)

        List<Terminology> terminologies = terminologyService.findAllByFolderId(versionedFolderId)

        Terminology busDefTerminology = terminologies.find {it.label == NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME}
        Terminology supDefTerminology = terminologies.find {it.label == NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}
        Terminology xmlSchemaConstraintsTerminology = terminologies.find {it.label == NhsDataDictionary.XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME}

        Folder dataSetsFolder = thisVersionedFolder.childFolders.find {it.label == NhsDataDictionary.DATA_SETS_FOLDER_NAME}

        DataModel coreModel = getCoreModel(versionedFolderId)

        addAttributesToDictionary(coreModel, dataDictionary)
        addElementsToDictionary(coreModel, dataDictionary)
        addClassesToDictionary(coreModel, dataDictionary)
        addDataSetsToDictionary(dataSetsFolder, dataDictionary)
        addBusDefsToDictionary(busDefTerminology, dataDictionary)
        addSupDefsToDictionary(supDefTerminology, dataDictionary)
        addXmlSchemaConstraintsToDictionary(xmlSchemaConstraintsTerminology, dataDictionary)
        log.debug('Data Dictionary built in {}', Utils.timeTaken(totalStart))
        return dataDictionary
    }

    Terminology getBusinessDefinitionTerminology(UUID versionedFolderId) {
        List<Terminology> terminologies = terminologyService.findAllByFolderId(versionedFolderId)
        terminologies.find {it.label == NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME}
    }

    Terminology getSupportingDefinitionTerminology(UUID versionedFolderId) {
        List<Terminology> terminologies = terminologyService.findAllByFolderId(versionedFolderId)
        terminologies.find {it.label == NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}
    }

    Terminology getXmlSchemaConstraintTerminology(UUID versionedFolderId) {
        List<Terminology> terminologies = terminologyService.findAllByFolderId(versionedFolderId)
        terminologies.find {it.label == NhsDataDictionary.XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME}
    }

    DataModel getCoreModel(UUID versionedFolderId) {
        List<DataModel> dataModels = dataModelService.findAllByFolderId(versionedFolderId)
        dataModels.find {it.label == NhsDataDictionary.CORE_MODEL_NAME}
    }

    Folder getDataSetsFolder(UUID versionedFolderId) {
        List<Folder> childFolders = folderService.findAllByParentId(versionedFolderId)
        childFolders.find {it.label == NhsDataDictionary.DATA_SETS_FOLDER_NAME}
    }

    void addAttributesToDictionary(DataModel coreModel, NhsDataDictionary dataDictionary) {
        DataClass attributesClass = coreModel.dataClasses.find {it.label == "Attributes"}
        DataClass retiredAttributesClass = attributesClass.dataClasses.find {it.label == "Retired"}
        List<DataElement> attributeElements = DataElement.by().inList('dataClass.id', [attributesClass.id, retiredAttributesClass.id]).list()

        dataDictionary.attributes = attributeService.collectNhsDataDictionaryComponents(attributeElements, dataDictionary)
    }

    void addElementsToDictionary(DataModel coreModel, NhsDataDictionary dataDictionary) {
        DataClass elementsClass = coreModel.dataClasses.find {it.label == "Data Field Notes"}
        DataClass retiredElementsClass = elementsClass.dataClasses.find {it.label == "Retired"}
        List<DataElement> elementElements = DataElement.by().inList('dataClass.id', [elementsClass.id, retiredElementsClass.id]).list()

        dataDictionary.elements = elementService.collectNhsDataDictionaryComponents(elementElements, dataDictionary)
    }

    void addClassesToDictionary(DataModel coreModel, NhsDataDictionary dataDictionary) {
        DataClass classesClass = coreModel.dataClasses.find {it.label == "Classes"}
        DataClass retiredClassesClass = classesClass.dataClasses.find {it.label == "Retired"}
        List<DataClass> classClasses = new DetachedCriteria<DataClass>(DataClass).inList('parentDataClass.id', [classesClass.id, retiredClassesClass.id]).list()
        dataDictionary.classes = classService.collectNhsDataDictionaryComponents(classClasses, dataDictionary)

/*       classClasses.each {dataClass ->
            //DDClass ddClass = new DDClass()
            //ddClass.fromCatalogueItem(dataDictionary, dataClass, classesClass.id, coreModel.id, metadataService)
            NhsDDClass clazz = classService.classFromDataClass(dataClass, dataDictionary)
            dataDictionary.classes[clazz.name] = clazz
        }

 */
        // Now link associations
        dataDictionary.classes.values().each { dataClass ->
            ((DataClass)dataClass.catalogueItem).dataElements.each { dataElement ->
                if(dataElement.dataType instanceof ReferenceType) {
                    DataClass referencedClass = ((ReferenceType)dataElement.dataType).referenceClass
                    dataClass.classRelationships.add(new NhsDDClassRelationship(
                        targetClass: dataDictionary.classes[referencedClass.label],
                        relationshipDescription: dataElement.label
                    ))
                }
            }
        }

    }

    void addDataSetsToDictionary(Folder dataSetsFolder, NhsDataDictionary dataDictionary) {
        Set<DataModel> dataSetModels = dataSetService.getAllDataSets(dataSetsFolder)
        dataDictionary.dataSets = dataSetService.collectNhsDataDictionaryComponents(dataSetModels, dataDictionary)
    }

    void addBusDefsToDictionary(Terminology busDefsTerminology, NhsDataDictionary dataDictionary) {
        dataDictionary.businessDefinitions = businessDefinitionService.collectNhsDataDictionaryComponents(termService.findAllByTerminologyId(busDefsTerminology.id), dataDictionary)
    }

    void addSupDefsToDictionary(Terminology supDefsTerminology, NhsDataDictionary dataDictionary) {
        dataDictionary.supportingInformation = supportingInformationService.collectNhsDataDictionaryComponents(termService.findAllByTerminologyId(supDefsTerminology.id), dataDictionary)
    }

    void addXmlSchemaConstraintsToDictionary(Terminology xmlSchemaConstraintsTerminology, NhsDataDictionary dataDictionary) {
        dataDictionary.xmlSchemaConstraints =
            xmlSchemaConstraintService.collectNhsDataDictionaryComponents(termService.findAllByTerminologyId(xmlSchemaConstraintsTerminology.id), dataDictionary)
    }

    Set<DataClass> getDataClasses(includeRetired = false) {
        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(NhsDataDictionary.CORE_MODEL_NAME)

        DataClass classesClass = coreModel.dataClasses.find {it.label == NhsDataDictionary.DATA_CLASSES_CLASS_NAME}

        Set<DataClass> classClasses = []
        classClasses.addAll(classesClass.dataClasses.findAll {it.label != "Retired"})
        if (includeRetired) {
            DataClass retiredClassesClass = classesClass.dataClasses.find {it.label == "Retired"}
            classClasses.addAll(retiredClassesClass.dataClasses)
        }

        return classClasses

    }

    Set<DataElement> getDataFieldNotes(includeRetired = false) {
        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(NhsDataDictionary.CORE_MODEL_NAME)

        DataClass elementsClass = coreModel.dataClasses.find {it.label == NhsDataDictionary.DATA_FIELD_NOTES_CLASS_NAME}

        Set<DataElement> elementElements = []
        elementElements.addAll(elementsClass.dataElements)
        if (includeRetired) {
            DataClass retiredElementsClass = elementsClass.dataClasses.find {it.label == "Retired"}
            elementElements.addAll(retiredElementsClass.dataElements)
        }
        return elementElements
    }

    Set<DataElement> getAttributes(includeRetired = false) {
        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(NhsDataDictionary.CORE_MODEL_NAME)

        DataClass attributesClass = coreModel.dataClasses.find {it.label == NhsDataDictionary.ATTRIBUTES_CLASS_NAME}

        Set<DataElement> attributeElements = []
        attributeElements.addAll(attributesClass.dataElements)
        if (includeRetired) {
            DataClass retiredAttributesClass = attributesClass.dataClasses.find {it.label == "Retired"}
            attributeElements.addAll(retiredAttributesClass.dataElements)
        }
        return attributeElements
    }

    List<StereotypedCatalogueItem> allItemsIndex(UUID versionedFolderId, boolean includeRetired = true) {
        List<StereotypedCatalogueItem> allItems = []

        allItems.addAll(dataSetService.index(versionedFolderId, includeRetired).collect {
            new StereotypedCatalogueItem(it, "dataModel")
        })
        allItems.addAll(classService.index(versionedFolderId, includeRetired).collect {
            new StereotypedCatalogueItem(it, "class")
        })
        allItems.addAll(elementService.index(versionedFolderId, includeRetired).collect {
            new StereotypedCatalogueItem(it, "element")
        })
        allItems.addAll(attributeService.index(versionedFolderId, includeRetired).collect {
            new StereotypedCatalogueItem(it, "attribute")
        })
        allItems.addAll(businessDefinitionService.index(versionedFolderId, includeRetired).collect {
            new StereotypedCatalogueItem(it, "businessDefinition")
        })
        allItems.addAll(supportingInformationService.index(versionedFolderId, includeRetired).collect {
            new StereotypedCatalogueItem(it, "supportingInformation")
        })
        allItems.addAll(xmlSchemaConstraintService.index(versionedFolderId, includeRetired).collect {
            new StereotypedCatalogueItem(it, "xmlSchemaConstraint")
        })
        //        System.err.println(allItems.sort {it.key.label})
        return allItems.sort()
    }


    File publishDita(UUID versionedFolderId) {
        NhsDataDictionary dataDictionary = buildDataDictionary(versionedFolderId)

        File tempDir = File.createTempDir()
        log.debug(tempDir.path)
        GenerateDita generateDita = new GenerateDita(dataDictionary, tempDir.path)
        dataDictionary.elements.values().each {ddElement ->
            ddElement.calculateFormatLinkMatchedItem(dataDictionary)
        }
        //generateDita.cleanSourceTarget(options.ditaOutputDir)
        generateDita.generateInput()

        File outputFile = new File(tempDir.path + "/" + "map.ditamap")

        File tempDir2 = File.createTempDir()
        log.debug(tempDir2.path)

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

        for (T t : collection) {
            if (!uniques.add(t)) {
                duplicates.add(t);
            }
        }

        return duplicates;
    }


    VersionedFolder ingest(User currentUser, def xml, String releaseDate, Boolean finalise, String folderVersionNo, String prevVersion) {

        NhsDataDictionary nhsDataDictionary = NhsDataDictionary.buildFromXml(xml, releaseDate)

        log.info('Ingesting new NHSDD with release date {}, finalise {}, folderVersionNo {}, prevVersion {}', releaseDate, finalise, folderVersionNo, prevVersion)
        long startTime = System.currentTimeMillis()
        long originalStartTime = startTime
        long endTime
        String dictionaryFolderName = "NHS Data Dictionary (${releaseDate})"
        if (!finalise && !folderVersionNo && !prevVersion) {
            deleteOriginalFolder(dictionaryFolderName)
            endTime = System.currentTimeMillis()
            log.info("Delete old folder complete in ${Utils.getTimeString(endTime - startTime)}")
            startTime = endTime
        }

        VersionedFolder dictionaryFolder = new VersionedFolder(authority: authorityService.defaultAuthority, label: dictionaryFolderName, createdBy: currentUser.emailAddress)

        if (!folderService.validate(dictionaryFolder)) {
            throw new ApiInvalidModelException('NHSDD', 'Invalid model', dictionaryFolder.errors)
        }

        versionedFolderService.save(dictionaryFolder)

        VersionedFolder prevDictionaryVersion = null
        if (prevVersion) {
            prevDictionaryVersion = versionedFolderService.get(prevVersion)
            versionedFolderService.setFolderIsNewBranchModelVersionOfFolder(dictionaryFolder, prevDictionaryVersion, currentUser)
        }

        DataModel coreDataModel =
            new DataModel(label: NhsDataDictionary.CORE_MODEL_NAME,
                          description: "Elements, attributes and classes",
                          folder: dictionaryFolder,
                          createdBy: currentUser.emailAddress,
                          authority: authorityService.defaultAuthority,
                          type: DataModelType.DATA_STANDARD)
        /*
        if (prevDictionaryVersion) {
            DataModel prevCore = folderService.findAllModelsInFolder(prevDictionaryVersion).find {
                it.label == NhsDataDictionary.CORE_MODEL_NAME
            }
            coreDataModel.addToVersionLinks(
                linkType: VersionLinkType.NEW_MODEL_VERSION_OF,
                createdBy: currentUser.emailAddress,
                targetModel: prevCore
            )
        }
        NhsDataDictionary nhsDataDictionary = new NhsDataDictionary()
        nhsDataDictionary.currentUser = currentUser
        nhsDataDictionary.coreDataModel = coreDataModel
        */

        endTime = System.currentTimeMillis()
        log.info('{} model built in {}', NhsDataDictionary.CORE_MODEL_NAME, Utils.getTimeString(endTime - startTime))

        Map<String, Terminology> attributeTerminologiesByName = [:]
        Map<String, DataElement> attributeElementsByName = [:]


        attributeService.persistAttributes(nhsDataDictionary, dictionaryFolder, coreDataModel, currentUser.emailAddress, attributeTerminologiesByName, attributeElementsByName)
        elementService.persistElements(nhsDataDictionary, dictionaryFolder, coreDataModel, currentUser.emailAddress, attributeTerminologiesByName)
        classService.persistClasses(nhsDataDictionary, dictionaryFolder, coreDataModel, currentUser.emailAddress, attributeElementsByName)
        businessDefinitionService.persistBusinessDefinitions(nhsDataDictionary, dictionaryFolder, currentUser.emailAddress)
        supportingInformationService.persistSupportingInformation(nhsDataDictionary, dictionaryFolder, currentUser.emailAddress)
        xmlSchemaConstraintService.persistXmlSchemaConstraints(nhsDataDictionary, dictionaryFolder, currentUser.emailAddress)

        startTime = System.currentTimeMillis()
        // Validation takes 2-6 mins which is annoying for testing
        log.trace('Validating {} model', NhsDataDictionary.CORE_MODEL_NAME)
        dataModelService.validate(coreDataModel)
        endTime = System.currentTimeMillis()
        log.info('Validate {} model complete in {}', NhsDataDictionary.CORE_MODEL_NAME, Utils.getTimeString(endTime - startTime))
        startTime = endTime
        if (coreDataModel.hasErrors()) {
            throw new ApiInvalidModelException('DMSXX', 'Model is invalid', coreDataModel.errors)
        }

        dataModelService.saveModelWithContent(coreDataModel)
        endTime = System.currentTimeMillis()
        log.info('Saved {} model complete in ', Utils.getTimeString(endTime - startTime))

        startTime = System.currentTimeMillis()
        dataSetService.persistDataSets(nhsDataDictionary, dictionaryFolder, coreDataModel, currentUser)
        log.info('{} persist complete in {}', dataSetService.getClass().getCanonicalName(), Utils.timeTaken(startTime))

        // If any changes remain on the dictionary folder then save them
        if (dictionaryFolder.isDirty()) {
            log.debug('Validate and save {}', dictionaryFolderName)
            startTime = System.currentTimeMillis()
            if (!folderService.validate(dictionaryFolder)) {
                throw new ApiInvalidModelException('NHSDD', 'Invalid model', dictionaryFolder.errors)
            }
            versionedFolderService.save(dictionaryFolder, validate: false, flush: true)
            log.info('Validate and save {} complete in {}', dictionaryFolderName, Utils.timeTaken(startTime))
        } else {
            // Otherwise flush and clear the session before we do anything else
            log.debug('Flush the session')
            sessionFactory.currentSession.flush()
        }

        dictionaryFolder = versionedFolderService.get(dictionaryFolder.id)

        if (finalise) {
            log.info('Finalising {}', dictionaryFolderName)
            startTime = System.currentTimeMillis()
            Version requestedFolderVersion = folderVersionNo ? Version.from(folderVersionNo) : null
            versionedFolderService.finaliseFolder(dictionaryFolder, currentUser, requestedFolderVersion, VersionChangeType.MAJOR, releaseDate)
            log.info('Finalise {} complete in {}', dictionaryFolderName, Utils.timeTaken(startTime))
        }

        log.info('Final save and flush of the ingested {}', dictionaryFolderName)
        if (!folderService.validate(dictionaryFolder)) {
            throw new ApiInvalidModelException('NHSDD', 'Invalid model', dictionaryFolder.errors)
        }
        versionedFolderService.saveFolderHierarchy(dictionaryFolder)
        log.info('Ingest {} complete in {}', dictionaryFolderName, Utils.timeTaken(originalStartTime))
        dictionaryFolder

    }


    void deleteOriginalFolder(String coreFolderName) {
        Folder folder = folderService.findByPath(coreFolderName)
        if (folder) {
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
                    it instanceof ModelDataType && ((ModelDataType) it).modelResourceId == terminology.id
                }
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
                    Metadata retiredMetadata = term.metadata.find {it.namespace == "uk.nhs.datadictionary.term" && it.key == "isRetired"}
                    boolean isRetired = (!retiredMetadata || retiredMetadata.value == "true")
                    Metadata defaultMetadata = term.metadata.find {it.namespace == "uk.nhs.datadictionary.term" && it.key == "isDefault"}
                    boolean isDefault = (!defaultMetadata || defaultMetadata.value == "true")
                    String retiredDate = term.metadata.find {it.namespace == "uk.nhs.datadictionary.term" && it.key == "retiredDate"}?.value
                    Map concept = [
                        code      : term.code,
                        display   : term.definition,
                        definition: term.definition,
                        property  : [
                            [
                                code         : "Publish Date",
                                valueDateTime: Instant.now().toString()
                            ],
                            [
                                code     : "status",
                                valueCode: isRetired ? "retired" : "active"
                            ],
                        ]
                    ]
                    if (linkedAttribute && !isDefault) {
                        concept.property << [
                            code       : "Data Attribute",
                            valueString: linkedAttribute.label
                        ]
                    }
                    if (retiredDate) {
                        concept.property << [
                            code         : "Retired Date",
                            valueDateTime: retiredDate
                        ]
                    }
                    codeSetDataTypeMap.entrySet().each {entry ->
                        CodeSet codeSet = entry.key
                        ModelDataType dataType = entry.value
                        if (codeSet.terms.contains(term)) {
                            dataType.dataElements.each {dataElement ->
                                concept.property << [
                                    code       : "Data Element",
                                    valueString: dataElement.label
                                ]
                            }
                        }
                    }

                    concepts << concept

                }
                String id = terminology.label.toLowerCase().replace(" ", "_")
                String version = terminology.metadata.find {it.key == "version"}?.value
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                String versionId = LocalDate.now().format(formatter)
                Map resource = [
                    resourceType    : "CodeSystem",
                    id              : "NHS-Data_Dictionary-CS-" + terminology.label.toLowerCase().replace(" ", "-") + "-" + version,
                    meta            : [
                        security : [
                            [
                                system: "http://ontoserver.csiro.au/CodeSystem/ontoserver-permissions",
                                code  : "DDT.read"
                            ],
                            [
                                system: "http://ontoserver.csiro.au/CodeSystem/ontoserver-permissions",
                                code  : "DDT.write"
                            ]
                        ],
                        versionId: versionId
                    ],
                    language        : "en-GB",
                    url             : "https://private.datadictionary.nhs.uk/code_system/union/${id}",
                    identifier      : [
                        [
                            system: "https://datadictionary.nhs.uk/V3",
                            value : "https://datadictionary.nhs.uk/data_dictionary/attributes/${id}"
                        ]
                    ],
                    version         : version,
                    name            : terminology.label.toUpperCase().replaceAll("[^A-Za-z0-9]", "_"),
                    title           : terminology.label,
                    status          : "active",
                    experimental    : "false",
                    date            : Instant.now().toString(),
                    publisher       : "NHS Digital",
                    contact         : [
                        [
                            name   : "NHS Digital Data Model and Dictionary Service",
                            telecom: [
                                [
                                    system: "email",
                                    value : "information.standards@nhs.net"
                                ], [
                                    system: "url",
                                    value : "https://datadictionary.nhs.uk/"
                                ]
                            ]
                        ]
                    ],
                    description     : linkedAttribute.description,
                    copyright       : "Copyright © NHS Digital",
                    caseSensitive   : "false",
                    hierarchyMeaning: "is-a",
                    compositional   : "false",
                    versionNeeded   : "false",
                    content         : "complete",
                    filter          : [
                        [
                            code       : "Data Element",
                            description: "Filter to identify which Data Elements a concept relates to",
                            operator   : ["in", "=", "exists", "not-in"],
                            value      : "Data Element"
                        ],
                        [
                            code       : "Data Attribute",
                            description: "Filter to identify which Data Attribute a concept relates to",
                            operator   : ["in", "=", "exists", "not-in"],
                            value      : "Data Attribute"
                        ],
                        [
                            code       : "Publish Date",
                            description: "Filter to enable filtering by Publish date/s",
                            operator   : ["in", "=", "exists", "not-in"],
                            value      : "Publish Date"
                        ],
                        [
                            code       : "Retired Date",
                            description: "Filter to enable filtering by date of retirement",
                            operator   : ["in", "=", "exists", "not-in"],
                            value      : "Retired Date"
                        ],
                        [
                            code       : "status",
                            description: "Filter to identify status of a concept",
                            operator   : ["in", "=", "not-in"],
                            value      : "status"
                        ]
                    ],
                    property        : [
                        [
                            code       : "Data Element",
                            description: "The Data Element that the concept relates to",
                            type       : "string"
                        ],
                        [
                            code       : "Data Attribute",
                            description: "The Data Attribute that the concept relates to",
                            type       : "string"
                        ],
                        [
                            code       : "Publish Date",
                            description: "The date on which the concept was first published and/or updated",
                            type       : "dateTime"
                        ],
                        [
                            code       : "Retired Date",
                            description: "The date from which the concept was published as retired",
                            type       : "dateTime"
                        ],
                        [
                            code       : "status",
                            description: "The status of the concept can be one of: draft | active | retired | unknown",
                            uri        : "http://hl7.org/fhir/concept-properties#status",
                            type       : "code"
                        ],
                    ],
                    count           : concepts.size(),
                    concept         : concepts
                ]
                Map entry = [//fullUrl: "CodeSystem/\$validate",
                             request : [
                                 method: "POST",
                                 url   : "CodeSystem/\$validate"
                             ],
                             resource: [
                                 resourceType: "Parameters",
                                 parameter   : [
                                     [name    : "Resource",
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
                    it instanceof ModelDataType && ((ModelDataType) it).modelResourceId == codeSet.id
                }
                DataElement linkedElement = linkedElementType.dataElements.first()

                Map<Terminology, List<Term>> terminologyListMap = [:]
                codeSet.terms.each {term ->
                    Terminology terminology = term.terminology
                    if (terminologyListMap[terminology]) {
                        terminologyListMap[terminology] << term
                    } else {
                        terminologyListMap[terminology] = [term]
                    }
                }
                List includes = []
                terminologyListMap.entrySet().each {entry ->
                    Terminology terminology = entry.key
                    String terminologyId = terminology.label.toLowerCase().replace(" ", "_")
                    String version = terminology.metadata.find {it.key == "version"}?.value
                    List<Term> terms = entry.value
                    def concept = []
                    terms.each {term ->
                        concept << [
                            code   : term.code,
                            display: term.definition
                        ]
                    }
                    includes << [
                        system : "https://private.datadictionary.nhs.uk/code_system/union/${terminologyId}",
                        version: version,
                        concept: concept
                    ]
                }
                String id = codeSet.label.toLowerCase().replace(" ", "_")
                String version = codeSet.metadata.find {it.key == "version"}?.value
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                String versionId = LocalDate.now().format(formatter)
                Map resource = [
                    resourceType: "ValueSet",
                    id          : "NHS-Data_Dictionary-VS-" + codeSet.label.toLowerCase().replace(" ", "-") + "-" + version,
                    meta        : [
                        security : [
                            [
                                system: "http://ontoserver.csiro.au/CodeSystem/ontoserver-permissions",
                                code  : "DDT.read"
                            ],
                            [
                                system: "http://ontoserver.csiro.au/CodeSystem/ontoserver-permissions",
                                code  : "DDT.write"
                            ]
                        ],
                        versionId: versionId
                    ],
                    language    : "en-GB",
                    url         : "https://private.datadictionary.nhs.uk/valueSet/${id}",
                    identifier  : [
                        [
                            system: "https://datadictionary.nhs.uk/V3",
                            value : "https://datadictionary.nhs.uk/data_dictionary/data_field_notes/${id}"
                        ]
                    ],
                    version     : version,
                    name        : codeSet.label.toUpperCase().replaceAll("[^A-Za-z0-9]", "_"),
                    title       : codeSet.label,
                    status      : "active",
                    experimental: "false",
                    date        : Instant.now().toString(),
                    publisher   : "NHS Digital",
                    contact     : [
                        [
                            name   : "NHS Digital Data Model and Dictionary Service",
                            telecom: [
                                [
                                    system: "email",
                                    value : "information.standards@nhs.net"
                                ], [
                                    system: "url",
                                    value : "https://datadictionary.nhs.uk/"
                                ]
                            ]
                        ]
                    ],
                    description : linkedElement.description,
                    copyright   : "Copyright © NHS Digital",
                    immutable   : "true",
                    compose     : [
                        include: includes
                    ]
                ]
                Map entry = [//fullUrl: "CodeSystem/\$validate",
                             request : [
                                 method: "POST",
                                 url   : "ValueSet/\$validate"
                             ],
                             resource: [
                                 resourceType: "Parameters",
                                 parameter   : [
                                     [name    : "Resource",
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
        log.debug(od)

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