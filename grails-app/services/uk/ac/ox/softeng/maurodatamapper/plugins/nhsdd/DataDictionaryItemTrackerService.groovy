package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity

@Slf4j
@Transactional
class DataDictionaryItemTrackerService {
    GraphService graphService
    VersionedFolderService versionedFolderService
    AsyncJobService asyncJobService

    MdmDomain getMauroItem(String domainName, UUID id) {
        if (domainName in ["folder", "Folder"]) {
            return Folder.get(id)
        }

        if (domainName in ["dataModel", "DataModel"]) {
            return DataModel.get(id)
        }

        if (domainName in ["dataClass", "DataClass"]) {
            return DataClass.get(id)
        }
        if (domainName in ["dataElement", "DataElement"]) {
            return DataElement.get(id)
        }

        if (domainName in ["term", "Term"]) {
            return Term.get(id)
        }

        null
    }

    AsyncJob asyncBuildGraphNode(UUID versionedFolderId, String domainName, UUID id, User startedByUser) {
        asyncJobService.createAndSaveAsyncJob(
            "Item tracker: update graph node for item '$domainName' [$id] under VF [$versionedFolderId]",
            startedByUser) {
            buildGraphNode(versionedFolderId, domainName, id)
        }
    }

    GraphNode buildGraphNode(UUID versionedFolderId, String domainName, UUID id) {
        try {
            VersionedFolder rootBranch = versionedFolderService.get(versionedFolderId)
            if (!rootBranch) {
                log.warn("Cannot find versioned folder [$versionedFolderId]")
                return null
            }

            MdmDomain item = getMauroItem(domainName, id)
            if (!item) {
                log.warn("Cannot find item '$domainName' [$id]")
                return null
            }

            return graphService.buildGraphNode(rootBranch, item)
        }
        catch (Exception exception) {
            log.error(
                "Cannot build graph node for '$predecessorPath': $exception.message",
                exception)

            return null
        }
    }

    AsyncJob asyncUpdateSuccessorGraphNodesAfterItemDeleted(
        UUID versionedFolderId,
        String predecessorPath,
        GraphNode predecessorGraphNode,
        User startedByUser) {
        asyncJobService.createAndSaveAsyncJob(
            "Item tracker: remove deleted item '$predecessorPath' from graph nodes under VF [$versionedFolderId]",
            startedByUser) {
            updateSuccessorGraphNodesAfterItemDeleted(versionedFolderId, predecessorPath, predecessorGraphNode)
        }
    }

    void updateSuccessorGraphNodesAfterItemDeleted(
        UUID versionedFolderId,
        String predecessorPath,
        GraphNode predecessorGraphNode) {
        try {
            VersionedFolder rootBranch = versionedFolderService.get(versionedFolderId)
            if (!rootBranch) {
                log.warn("Cannot find versioned folder [$versionedFolderId]")
                return
            }

            graphService.removePredecessorFromSuccessorGraphNodes(
                rootBranch,
                predecessorPath,
                predecessorGraphNode)
        }
        catch (Exception exception) {
            log.error(
                "Cannot update successor graph nodes to remove predecessor '$predecessorPath': $exception.message",
                exception)
        }
    }

    AsyncJob asyncUpdatePredecessorFromSuccessorGraphNodesAfterItemMoved(
        UUID versionedFolderId,
        String domainName,
        UUID id,
        String originalPredecessorPath,
        String replacementPredecessorPath,
        User startedByUser) {
        asyncJobService.createAndSaveAsyncJob(
            "Item tracker: update successor graph nodes under VF [$versionedFolderId] to replace predecessor",
            startedByUser) {
            log.info("Item moved: updating successor graph nodes")
            updatePredecessorFromSuccessorGraphNodesAfterItemMoved(
                versionedFolderId,
                domainName,
                id,
                originalPredecessorPath,
                replacementPredecessorPath)

            log.info("Item moved: updating predecessor descriptions")
            modifyPredecessorsAndDescriptionsAfterItemMoved(
                versionedFolderId,
                domainName,
                id,
                originalPredecessorPath,
                replacementPredecessorPath)
        }
    }

    GraphNode updatePredecessorFromSuccessorGraphNodesAfterItemMoved(
        UUID versionedFolderId,
        String domainName,
        UUID id,
        String originalPredecessorPath,
        String replacementPredecessorPath) {
        try {
            VersionedFolder rootBranch = versionedFolderService.get(versionedFolderId)
            if (!rootBranch) {
                log.warn("Cannot find versioned folder [$versionedFolderId]")
                return null
            }

            MdmDomain item = getMauroItem(domainName, id)
            if (!item) {
                log.warn("Cannot find item '$domainName' [$id]")
                return null
            }

            GraphNode predecessorGraphNode = graphService.getGraphNode(item)

            return graphService.updatePredecessorFromSuccessorGraphNodes(
                rootBranch,
                originalPredecessorPath,
                replacementPredecessorPath,
                predecessorGraphNode)
        }
        catch (Exception exception) {
            log.error(
                "Cannot update successor graph nodes to change predecessor path '$originalPredecessorPath' with '$replacementPredecessorPath': $exception.message",
                exception)

            return null
        }
    }

    void modifyPredecessorsAndDescriptionsAfterItemMoved(
        UUID versionedFolderId,
        String domainName,
        UUID id,
        String originalPath,
        String replacementPath) {
        try {
            VersionedFolder rootBranch = versionedFolderService.get(versionedFolderId)
            if (!rootBranch) {
                log.warn("Cannot find versioned folder [$versionedFolderId]")
                return
            }

            MdmDomain item = getMauroItem(domainName, id)
            if (!item) {
                log.warn("Cannot find item '$domainName' [$id]")
                return
            }

            GraphNode graphNode = graphService.getGraphNode(item)

            // The NHS data dictionary ingest will have added paths without model identifiers (e.g. branches like "$main"),
            // remove these so that we'll find the correct substrings to update
            Path originalPathWithoutBranch = GraphService.createPathWithoutBranchName(Path.from(originalPath))
            Path replacementPathWithoutBranch = GraphService.createPathWithoutBranchName(Path.from(replacementPath))

            String originalHyperlink = "<a href=\"$originalPathWithoutBranch\">"
            String replacementHyperlink = "<a href=\"$replacementPathWithoutBranch\">"
            log.debug("Original hyperlink: $originalHyperlink, Updated hyperlink: $replacementHyperlink")

            graphNode.predecessors.forEach { predecessorPath ->
                // The predecessor path will refer to the original path, especially if it is a model item.
                // For example, if the item moved was "dm:Model|dc:Class" to "dm:Model|dc:Class Modified",
                // then a child path of "dm:Model|dc:Class|de:Element" also needs to change to find the
                // item correctly
                String correctedPredecessorPath = predecessorPath.replace(originalPath, replacementPath)

                modifyDescriptionInPredecessorItem(
                    correctedPredecessorPath,
                    originalHyperlink,
                    replacementHyperlink,
                    originalPath,
                    replacementPath)
            }
        }
        catch (Exception exception) {
            log.error(
                "Cannot update predecessors and descriptions to change path '$originalPath' with '$replacementPath': $exception.message",
                exception)
        }
    }

    private <T extends MdmDomain & InformationAware & MetadataAware & GormEntity> void modifyDescriptionInPredecessorItem(
        String predecessorPath,
        String originalHyperlink,
        String replacementHyperlink,
        String originalPath,
        String replacementPath) {
        try {
            T predecessorItem = graphService.findGraphResourceByPath(predecessorPath) as T
            if (!predecessorItem) {
                return
            }

            if (!predecessorItem.description || predecessorItem.description.length() == 0) {
                log.debug("Ignore modifying $predecessorItem.domainType '$predecessorItem.label' [$predecessorItem.id] - no description")
                return
            }

            predecessorItem.description = predecessorItem.description.replace(originalHyperlink, replacementHyperlink)

            predecessorItem.save([flush: true, validate: true])
            log.info("Updated description of $predecessorItem.domainType '$predecessorItem.label' [$predecessorItem.id]")

            GraphNode graphNode = graphService.getGraphNode(predecessorItem)
            log.info("Updating successor '$originalPath' to '$replacementPath' in '$predecessorPath'")
            graphNode.replaceSuccessor(originalPath, replacementPath)
            graphService.saveGraphNode(predecessorItem, graphNode)
        }
        catch (Exception exception) {
            log.error("Exception occurred updating $predecessorPath: $exception.message", exception)
        }
    }
}
