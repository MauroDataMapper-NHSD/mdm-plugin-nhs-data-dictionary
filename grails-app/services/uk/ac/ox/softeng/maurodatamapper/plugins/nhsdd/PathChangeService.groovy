package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
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

    FolderService folderService
    DataModelService dataModelService
    DataClassService dataClassService
    DataElementService dataElementService
    TermService termService

    AsyncJob asyncModifyRelatedItemsAfterPathChange(
        UUID versionedFolderId,
        String originalLabel,
        Path originalPath,
        String updatedLabel,
        Path updatedPath,
        User startedByUser) {
        asyncJobService.createAndSaveAsyncJob(
            "Path change update from $originalPath to $updatedPath",
            startedByUser,
            {
                modifyRelatedItemsAfterPathChange(versionedFolderId, originalLabel, originalPath, updatedLabel, updatedPath)
            })
    }

    void modifyRelatedItemsAfterPathChange(
        UUID versionedFolderId,
        String originalLabel,
        Path originalPath,
        String updatedLabel,
        Path updatedPath) {
        // The NHS data dictionary ingest will have added paths without model identifiers (e.g. branches like "$main"),
        // remove these so that we'll find the correct substrings to update
        Path originalPathWithoutBranches = originalPath.clone()
        originalPathWithoutBranches.pathNodes.forEach { it.modelIdentifier = "" }

        Path updatedPathWithoutBranches = updatedPath.clone()
        updatedPathWithoutBranches.pathNodes.forEach { it.modelIdentifier = "" }

        String originalHyperlink = "<a href=\"$originalPathWithoutBranches\">$originalLabel</a>"
        log.info("Original path: $originalPathWithoutBranches")
        log.info("Original hyperlink: $originalHyperlink")

        String updatedHyperlink = "<a href=\"$updatedPathWithoutBranches\">$updatedLabel</a>"
        log.info("Updated path: $updatedPathWithoutBranches")
        log.info("Updated hyperlink: $updatedHyperlink")

        log.debug("Building Data Dictionary...")
        long startTime = System.currentTimeMillis()
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(versionedFolderId)

        log.info("Scanning '$dataDictionary.containingVersionedFolder.label' [$dataDictionary.containingVersionedFolder.id]")
        List<Folder> folders = []
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
            originalHyperlink,
            folders,
            dataModels,
            dataClasses,
            dataElements,
            terms)

        // Update all the collected Mauro catalogue items to update their descriptions. Replace the old hyperlink
        // with the new one so nothing is broken
        updateMauroFolders(folders, originalHyperlink, updatedHyperlink)
        updateMauroDataModels(dataModels, originalHyperlink, updatedHyperlink)
        updateMauroDataClasses(dataClasses, originalHyperlink, updatedHyperlink)
        updateMauroDataElements(dataElements, originalHyperlink, updatedHyperlink)
        updateMauroTerms(terms, originalHyperlink, updatedHyperlink)

        log.info("Path update in data dictionary completed in ${Utils.timeTaken(startTime)}")
    }

    static void scanNhsDataDictionary(
        NhsDataDictionary dataDictionary,
        String originalHyperlink,
        List<Folder> folders,
        List<DataModel> dataModels,
        List<DataClass> dataClasses,
        List<DataElement> dataElements,
        List<Term> terms) {

        List<NhsDataDictionaryComponent> components = dataDictionary.allComponents

        components.each { component ->
            def catalogueItem = component.catalogueItem

            if (catalogueItem instanceof Folder) {
                Folder folder = (Folder)catalogueItem
                if (folder.description && folder.description.contains(originalHyperlink)) {
                    log.info("Requires update: Folder '$folder.label' [$folder.id]")
                    folders.add(folder)
                }
            }

            if (catalogueItem instanceof DataModel) {
                DataModel dataModel = (DataModel)catalogueItem
                if (dataModel.description && dataModel.description.contains(originalHyperlink)) {
                    log.info("Requires update: DataModel '$dataModel.label' [$dataModel.id]")
                    dataModels.add(dataModel)
                }
            }

            if (catalogueItem instanceof DataClass) {
                DataClass dataClass = (DataClass)catalogueItem
                if (dataClass.description && dataClass.description.contains(originalHyperlink)) {
                    log.info("Requires update: DataClass '$dataClass.label' [$dataClass.id]")
                    dataClasses.add(dataClass)
                }
            }

            if (catalogueItem instanceof DataElement) {
                DataElement dataElement = (DataElement)catalogueItem
                if (dataElement.description && dataElement.description.contains(originalHyperlink)) {
                    log.info("Requires update: DataElement '$dataElement.label' [$dataElement.id]")
                    dataElements.add(dataElement)
                }
            }

            if (catalogueItem instanceof Term) {
                Term term = (Term)catalogueItem
                if (term.description && term.description.contains(originalHyperlink)) {
                    log.info("Requires update: Term '$term.label' [$term.id]")
                    terms.add(term)
                }
            }
        }
    }

    void updateMauroFolders(List<Folder> folders, String original, String updated) {
        if (folders.empty) {
            return
        }

        folders.each { folder ->
            folder.description = folder.description.replace(original, updated)

            try {
                folderService.save([flush: true, validate: true], folder)
                log.info("Updated Folder '$folder.label' [$folder.id]")
            }
            catch (Exception exception) {
                log.error("Exception occurred updating Folder '$folder.label' [$folder.id]: $exception.message")
            }
        }
    }

    void updateMauroDataModels(List<DataModel> dataModels, String original, String updated) {
        if (dataModels.empty) {
            return
        }

        dataModels.each { dataModel ->
            dataModel.description = dataModel.description.replace(original, updated)

            try {
                dataModelService.save([flush: true, validate: true], dataModel)
                log.info("Updated DataModel '$dataModel.label' [$dataModel.id]")
            }
            catch (Exception exception) {
                log.error("Exception occurred updating DataModel '$dataModel.label' [$dataModel.id]: $exception.message")
            }
        }
    }

    void updateMauroDataClasses(List<DataClass> dataClasses, String original, String updated) {
        if (dataClasses.empty) {
            return
        }

        dataClasses.each { dataClass ->
            dataClass.description = dataClass.description.replace(original, updated)

            try {
                dataClassService.save([flush: true, validate: true], dataClass)
                log.info("Updated DataClass '$dataClass.label' [$dataClass.id]")
            }
            catch (Exception exception) {
                log.error("Exception occurred updating DataClass '$dataClass.label' [$dataClass.id]: $exception.message")
            }
        }
    }

    void updateMauroDataElements(List<DataElement> dataElements, String original, String updated) {
        if (dataElements.empty) {
            return
        }

        dataElements.each { dataElement ->
            dataElement.description = dataElement.description.replace(original, updated)

            try {
                dataElementService.save([flush: true, validate: true], dataElement)
                log.info("Updated DataElement '$dataElement.label' [$dataElement.id]")
            }
            catch (Exception exception) {
                log.error("Exception occurred updating DataElement '$dataElement.label' [$dataElement.id]: $exception.message")
            }
        }
    }

    void updateMauroTerms(List<Term> terms, String original, String updated) {
        if (terms.empty) {
            return
        }

        terms.each { term ->
            term.description = term.description.replace(original, updated)

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
