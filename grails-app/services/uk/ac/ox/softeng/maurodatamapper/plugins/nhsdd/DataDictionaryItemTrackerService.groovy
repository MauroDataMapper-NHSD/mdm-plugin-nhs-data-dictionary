package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class DataDictionaryItemTrackerService {
    GraphService graphService
    VersionedFolderService versionedFolderService
    AsyncJobService asyncJobService

    AsyncJob asyncUpdateSuccessorGraphNodesAfterItemDeleted(
        UUID versionedFolderId,
        String predecessorPath,
        GraphNode predecessorGraphNode,
        User startedByUser) {
        asyncJobService.createAndSaveAsyncJob(
            "Item tracker: remove deleted item '$predecessorPath' from successor graph nodes under root branch [$versionedFolderId]",
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
}
