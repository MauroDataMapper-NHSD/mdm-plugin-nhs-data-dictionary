/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.VersionTreeModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version
import uk.ac.ox.softeng.maurodatamapper.version.VersionChangeType

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDClassRelationship
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.fhir.FhirBundle
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.fhir.FhirCodeSystem
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.fhir.FhirEntry
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.fhir.FhirValueSet
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.AllClassesHaveRelationships
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.AllItemsHaveAlias
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.AllItemsHaveShortDescription
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.AttributesLinkedToAClass
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.BrokenLinks
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.DataSetsHaveAnOverview
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.DataSetsIncludePreparatoryItem
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.ElementsLinkedToAnAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.IntegrityCheck
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks.ReusedItemNames
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish.ChangePaperUtility
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish.WebsiteUtility
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.utils.StereotypedCatalogueItem

import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Slf4j
@Transactional
class NhsDataDictionaryService {

    static final Map<String, String> KNOWN_KEYS = [
        'retired.template': '<p>This item has been retired from the NHS Data Model and Dictionary.</p>' +
                           '<p>Access to the last live version of this item can be obtained by emailing <a href=\"mailto:information.standards@nhs' +
                           '.net\">information.standards@nhs.net</a> with "NHS Data Model and Dictionary - Archive Request" in the email subject ' +
                            'line.</p>',
        'preparatory.template': '<p><b>This item is being used for development purposes and has not yet been approved.</b></p>']

    static final String NHSDD_PROPERTY_CATEGORY = 'NHS Data Dictionary'



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
        if(versionedFolders.size() == 0) {
            return []
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
            DataSetsIncludePreparatoryItem,
            AllItemsHaveShortDescription,
            AllItemsHaveAlias,
            ReusedItemNames,
            BrokenLinks
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
        NhsDataDictionary dataDictionary = newDataDictionary()

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
        classClasses.removeAll {it.label == "Retired"}
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

    List<StereotypedCatalogueItem> allItemsIndex(UUID versionedFolderId) {
        NhsDataDictionary dataDictionary = buildDataDictionary(versionedFolderId)

        List<StereotypedCatalogueItem> allItems = []

        allItems.addAll(dataDictionary.dataSets.values().collect {
            new StereotypedCatalogueItem(it)
        })
        allItems.addAll(dataDictionary.classes.values().collect {
            new StereotypedCatalogueItem(it)
        })
        allItems.addAll(dataDictionary.elements.values().collect {
            new StereotypedCatalogueItem(it)
        })
        allItems.addAll(dataDictionary.attributes.values().collect {
            new StereotypedCatalogueItem(it)
        })
        allItems.addAll(dataDictionary.businessDefinitions.values().collect {
            new StereotypedCatalogueItem(it)
        })
        allItems.addAll(dataDictionary.supportingInformation.values().collect {
            new StereotypedCatalogueItem(it)
        })
        allItems.addAll(dataDictionary.xmlSchemaConstraints.values().collect {
            new StereotypedCatalogueItem(it)
        })
        return allItems.sort()
    }


    File publishDita(UUID versionedFolderId) {


        //NhsDataDictionary dataDictionary = buildDataDictionary(versionedFolderId)
        NhsDataDictionary dataDictionary = newDataDictionary()
        String outputPath = "/Users/james/Desktop/ditaTest/"

        WebsiteUtility.generateWebsite(dataDictionary, outputPath)
        return null


/*        NhsDataDictionary dataDictionary = buildDataDictionary(versionedFolderId)

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
*/
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


    VersionedFolder ingest(User currentUser, def xml, String releaseDate, Boolean finalise, String folderVersionNo, String prevVersion, boolean deletePrevious = false) {

        NhsDataDictionary nhsDataDictionary = NhsDataDictionary.buildFromXml(xml, releaseDate, folderVersionNo)

        log.info('Ingesting new NHSDD with release date {}, finalise {}, folderVersionNo {}, prevVersion {}', releaseDate, finalise, folderVersionNo, prevVersion)
        long startTime = System.currentTimeMillis()
        long originalStartTime = startTime
        long endTime
        String dictionaryFolderName = "NHS Data Dictionary (${releaseDate})"
        if (deletePrevious || (!finalise && !folderVersionNo && !prevVersion)) {
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

        endTime = System.currentTimeMillis()
        log.info('{} model built in {}', NhsDataDictionary.CORE_MODEL_NAME, Utils.getTimeString(endTime - startTime))

        TreeMap<String, Terminology> attributeTerminologiesByName = new TreeMap<>()
        TreeMap<String, DataElement> attributeElementsByName = new TreeMap<>()

        startTime = System.currentTimeMillis()
        attributeService.persistAttributes(nhsDataDictionary, dictionaryFolder, coreDataModel, currentUser.emailAddress, attributeTerminologiesByName, attributeElementsByName)
        log.info('AttributeService persisted in {}', Utils.timeTaken(startTime))

        startTime = System.currentTimeMillis()
        elementService.persistElements(nhsDataDictionary, dictionaryFolder, coreDataModel, currentUser.emailAddress, attributeTerminologiesByName)
        log.info('ElementService persisted in {}', Utils.timeTaken(startTime))

        startTime = System.currentTimeMillis()
        classService.persistClasses(nhsDataDictionary, dictionaryFolder, coreDataModel, currentUser.emailAddress, attributeElementsByName)
        log.info('ClassService persisted in {}', Utils.timeTaken(startTime))

        startTime = System.currentTimeMillis()
        businessDefinitionService.persistBusinessDefinitions(nhsDataDictionary, dictionaryFolder, currentUser.emailAddress)
        log.info('BusinessDefinitionService persisted in {}', Utils.timeTaken(startTime))

        startTime = System.currentTimeMillis()
        supportingInformationService.persistSupportingInformation(nhsDataDictionary, dictionaryFolder, currentUser.emailAddress)
        log.info('SupportingInformationService persisted in {}', Utils.timeTaken(startTime))

        startTime = System.currentTimeMillis()
        xmlSchemaConstraintService.persistXmlSchemaConstraints(nhsDataDictionary, dictionaryFolder, currentUser.emailAddress)
        log.info('XmlSchemaConstraintService persisted in {}', Utils.timeTaken(startTime))

        startTime = System.currentTimeMillis()
        log.debug('Validating [{}] model', NhsDataDictionary.CORE_MODEL_NAME)
        dataModelService.validate(coreDataModel)
        endTime = System.currentTimeMillis()
        log.info('Validate [{}] model complete in {}', NhsDataDictionary.CORE_MODEL_NAME, Utils.getTimeString(endTime - startTime))
        startTime = endTime
        if (coreDataModel.hasErrors()) {
            throw new ApiInvalidModelException('DMSXX', 'Model is invalid', coreDataModel.errors)
        }

        dataModelService.saveModelWithContent(coreDataModel, 1000)
        endTime = System.currentTimeMillis()
        log.info('Saved [{}] model complete in {}', NhsDataDictionary.CORE_MODEL_NAME, Utils.getTimeString(endTime - startTime))

        startTime = System.currentTimeMillis()
        dataSetService.persistDataSets(nhsDataDictionary, dictionaryFolder, coreDataModel, currentUser)
        log.info('DataSetService persist complete in {}', Utils.timeTaken(startTime))

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

    FhirBundle codeSystemValidationBundle(UUID versionedFolderId) {
        NhsDataDictionary dataDictionary = buildDataDictionary(versionedFolderId)
        String publishDate = Instant.now().toString()
        List<FhirEntry> entries = dataDictionary.attributes.values()
            .findAll { attribute ->
                !attribute.isRetired() &&
                    attribute.codes.size() > 0
            }
            .sort { it.name }
            .collect {attribute ->
                new FhirEntry( requestUrl: 'CodeSystem/$validate',
                               resource: FhirCodeSystem.fromDDAttribute(attribute, "1.0.0", publishDate))
            }

        return new FhirBundle(type: "transaction", entries: entries)
    }

    FhirBundle valueSetValidationBundle(UUID versionedFolderId) {
        NhsDataDictionary dataDictionary = buildDataDictionary(versionedFolderId)
        String publishDate = Instant.now().toString()
        List<FhirEntry> entries = dataDictionary.elements.values()
            .findAll { element ->
                !element.isRetired() &&
                    element.codes.size() > 0
            }
            .sort { it.name }
            .collect { element ->
                new FhirEntry( requestUrl: 'ValueSet/$validate',
                               resource: FhirValueSet.fromDDElement(element, "1.0.0", publishDate))
            }
        return new FhirBundle(type: "transaction", entries: entries)
    }


    def changePaper(UUID versionedFolderId) {

        VersionedFolder thisDictionary = versionedFolderService.get(versionedFolderId)
        VersionedFolder previousVersion = versionedFolderService.getFinalisedParent(thisDictionary)

        NhsDataDictionary thisDataDictionary = buildDataDictionary(thisDictionary.id)
        NhsDataDictionary previousDataDictionary = buildDataDictionary(previousVersion.id)


        String outputPath = "/Users/james/Desktop/ditaTest/"

        ChangePaperUtility.generateChangePaper(thisDataDictionary, previousDataDictionary, outputPath)

    }

    NhsDataDictionary newDataDictionary() {
        NhsDataDictionary nhsDataDictionary = new NhsDataDictionary()
        setTemplateText(nhsDataDictionary)
        return nhsDataDictionary
    }

    void setTemplateText(NhsDataDictionary dataDictionary) {
        ApiProperty.findAllByCategory(NHSDD_PROPERTY_CATEGORY).each {apiProperty ->
            if(apiProperty.key == "retired.template") {
                dataDictionary.retiredItemText = apiProperty.value
            }
            if(apiProperty.key == "preparatory.template") {
                dataDictionary.preparatoryItemText = apiProperty.value
            }
        }
        if(!dataDictionary.preparatoryItemText) {
            dataDictionary.preparatoryItemText = KNOWN_KEYS["preparatory.template"]
        }
        if(!dataDictionary.retiredItemText) {
            dataDictionary.preparatoryItemText = KNOWN_KEYS["retired.template"]
        }
    }
}