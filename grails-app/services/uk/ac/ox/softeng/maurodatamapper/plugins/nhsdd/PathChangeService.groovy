package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent

@Slf4j
@Transactional
class PathChangeService {
    AsyncJobService asyncJobService
    NhsDataDictionaryService nhsDataDictionaryService

    DataModelService dataModelService
    DataClassService dataClassService
    DataElementService dataElementService
    TermService termService

    AsyncJob asyncModifyRelatedItemsAfterPathChange(UUID versionedFolderId, Path originalPath, Path updatedPath, User startedByUser) {
        asyncJobService.createAndSaveAsyncJob(
            "Path change update from $originalPath to $updatedPath",
            startedByUser,
            {
                modifyRelatedItemsAfterPathChange(versionedFolderId, originalPath, updatedPath)
            })
    }

    void modifyRelatedItemsAfterPathChange(UUID versionedFolderId, Path originalPath, Path updatedPath) {
        // The NHS data dictionary ingest will have added paths without model identifiers (e.g. branches like "$main"),
        // remove these so that we'll find the correct substrings to update
        Path originalPathWithoutBranches = originalPath.clone()
        originalPathWithoutBranches.pathNodes.forEach { it.modelIdentifier = "" }
        String originalPathWithoutBranchesString = originalPathWithoutBranches.toString()

        Path updatedPathWithoutBranches = updatedPath.clone()
        updatedPathWithoutBranches.pathNodes.forEach { it.modelIdentifier = "" }
        String updatedPathWithoutBranchesString = updatedPathWithoutBranches.toString()

        log.info("Original path: $originalPathWithoutBranchesString")
        log.info("Updated path: $updatedPathWithoutBranchesString")

        log.debug("Building Data Dictionary...")
        long startTime = System.currentTimeMillis()
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(versionedFolderId)
        log.info("Loaded Data Dictionary in ${Utils.timeTaken(startTime)}")

        log.info("Scanning '$dataDictionary.containingVersionedFolder.label' [$dataDictionary.containingVersionedFolder.id]")
        List<DataModel> dataModels = []
        List<DataClass> dataClasses = []
        List<DataElement> dataElements = []
        List<Term> terms = []

        // This is a brute-force approach, loading the entire data dictionary into memory is not ideal and will be slow,
        // but is the only effective way at the moment. There are future tasks aimed at optimising this to be more efficient:
        // - gh-17 - Trace backlinks of Mauro items
        // - gh-18 - Improve performance of hyperlink modification using backlinks
        scanNhsDataDictionary(
            dataDictionary,
            originalPathWithoutBranchesString,
            dataModels,
            dataClasses,
            dataElements,
            terms)

        updateMauroDataModels(dataModels, originalPathWithoutBranchesString, updatedPathWithoutBranchesString)
        updateMauroDataClasses(dataClasses, originalPathWithoutBranchesString, updatedPathWithoutBranchesString)
        updateMauroDataElements(dataElements, originalPathWithoutBranchesString, updatedPathWithoutBranchesString)
        updateMauroTerms(terms, originalPathWithoutBranchesString, updatedPathWithoutBranchesString)

        log.info("Path update in data dictionary completed in ${Utils.timeTaken(startTime)}")
    }

    static void scanNhsDataDictionary(
        NhsDataDictionary dataDictionary,
        String originalPath,
        List<DataModel> dataModels,
        List<DataClass> dataClasses,
        List<DataElement> dataElements,
        List<Term> terms) {

        List<NhsDataDictionaryComponent> components = dataDictionary.allComponents

        components.each { component ->
            def catalogueItem = component.catalogueItem

            if (catalogueItem instanceof DataModel) {
                DataModel dataModel = (DataModel)catalogueItem
                if (dataModel.description && dataModel.description.contains(originalPath)) {
                    log.info("Requires update: DataModel '$dataModel.label' [$dataModel.id]")
                    dataModels.add(dataModel)
                }
            }

            if (catalogueItem instanceof DataClass) {
                DataClass dataClass = (DataClass)catalogueItem
                if (dataClass.description && dataClass.description.contains(originalPath)) {
                    log.info("Requires update: DataClass '$dataClass.label' [$dataClass.id]")
                    dataClasses.add(dataClass)
                }
            }

            if (catalogueItem instanceof DataElement) {
                DataElement dataElement = (DataElement)catalogueItem
                if (dataElement.description && dataElement.description.contains(originalPath)) {
                    log.info("Requires update: DataElement '$dataElement.label' [$dataElement.id]")
                    dataElements.add(dataElement)
                }
            }

            if (catalogueItem instanceof Term) {
                Term term = (Term)catalogueItem
                if (term.description && term.description.contains(originalPath)) {
                    log.info("Requires update: Term '$term.label' [$term.id]")
                    terms.add(term)
                }
            }
        }
    }

    void updateMauroDataModels(List<DataModel> dataModels, String originalPath, String updatedPath) {
        if (dataModels.empty) {
            return
        }

        dataModels.each { dataModel ->
            dataModel.description = dataModel.description.replace(originalPath, updatedPath)

            try {
                dataModelService.save([flush: true, validate: true], dataModel)
                log.info("Updated DataModel '$dataModel.label' [$dataModel.id]")
            }
            catch (Exception exception) {
                log.error("Exception occurred updating DataModel '$dataModel.label' [$dataModel.id]: $exception.message")
            }
        }
    }

    void updateMauroDataClasses(List<DataClass> dataClasses, String originalPath, String updatedPath) {
        if (dataClasses.empty) {
            return
        }

        dataClasses.each { dataClass ->
            dataClass.description = dataClass.description.replace(originalPath, updatedPath)

            try {
                dataClassService.save([flush: true, validate: true], dataClass)
                log.info("Updated DataClass '$dataClass.label' [$dataClass.id]")
            }
            catch (Exception exception) {
                log.error("Exception occurred updating DataClass '$dataClass.label' [$dataClass.id]: $exception.message")
            }
        }
    }

    void updateMauroDataElements(List<DataElement> dataElements, String originalPath, String updatedPath) {
        if (dataElements.empty) {
            return
        }

        dataElements.each { dataElement ->
            dataElement.description = dataElement.description.replace(originalPath, updatedPath)

            try {
                dataElementService.save([flush: true, validate: true], dataElement)
                log.info("Updated DataElement '$dataElement.label' [$dataElement.id]")
            }
            catch (Exception exception) {
                log.error("Exception occurred updating DataElement '$dataElement.label' [$dataElement.id]: $exception.message")
            }
        }
    }

    void updateMauroTerms(List<Term> terms, String originalPath, String updatedPath) {
        if (terms.empty) {
            return
        }

        terms.each { term ->
            term.description = term.description.replace(originalPath, updatedPath)

            try {
                termService.save([flush: true, validate: true], term)
                log.info("Updated Term '$term.label' [$term.id]")
            }
            catch (Exception exception) {
                log.error("Exception occurred updating Term '$term.label' [$term.id]: $exception.message")
            }
        }
    }
}
