package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.interceptors

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ContainerTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.TreeItem
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.core.tree.TreeItemService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.DataDictionaryItemTrackerService
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.GraphNode
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.GraphService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.artefact.Interceptor
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.springframework.http.HttpStatus

import java.util.regex.Pattern

@Slf4j
class DataDictionaryItemDeletedInterceptor implements MdmInterceptor, Interceptor {
    static Pattern CONTROLLER_PATTERN = ~/(folder|dataModel|dataClass|dataElement|term)/

    GraphService graphService
    TreeItemService treeItemService
    VersionedFolderService versionedFolderService
    DataDictionaryItemTrackerService dataDictionaryItemTrackerService

    DataDictionaryItemDeletedInterceptor() {
        match controller: CONTROLLER_PATTERN, action: 'delete'
    }

    @Override
    boolean before() {
        MdmDomain itemBeingDeleted = getItem()
        if (!itemBeingDeleted) {
            // Ignore
            return true
        }

        trackItemGraphNode(itemBeingDeleted)

        true
    }

    @Override
    boolean after() {
        if (response.status != HttpStatus.NO_CONTENT.value()) {
            log.warn("after: Ignoring item change for '$controllerName' [$params.id] - not deleted - HttpStatus: $request")
            return true
        }

        // Check if the before() interceptor method was tracking a Mauro item for an item change. If not then this
        // interceptor doesn't need to do anything
        DeleteItemState deleteItemState = request.deleteItemState
        if (!deleteItemState) {
            log.warn("after: Ignoring item change for '$controllerName' [$params.id] - not tracking")
            return true
        }

        log.info("after: Starting async job to update graph node successors to remove '$deleteItemState.path' under root branch [$deleteItemState.versionedFolderId]")
        AsyncJob asyncJob = dataDictionaryItemTrackerService.asyncUpdateSuccessorGraphNodesAfterItemDeleted(
            deleteItemState.versionedFolderId,
            deleteItemState.path,
            deleteItemState.graphNode,
            currentUser)

        if (!asyncJob) {
            log.error("after: Failed to create async job")
        }
        else {
            log.info("after: Created item deleted and graph node update async job [$asyncJob.id] - status: $asyncJob.status")
        }

        true
    }

    private <T extends MdmDomain & InformationAware & MetadataAware & GormEntity> void trackItemGraphNode(T item) {
        ContainerTreeItem ancestors = getAncestors(item)
        TreeItem versionedFolderTreeItem = getAncestorVersionedFolder(ancestors)
        if (!versionedFolderTreeItem) {
            log.debug("'$item.label' [$item.id] is not within a versioned folder, ignoring change tracking")
            return
        }

        try {
            GraphNode graphNode = graphService.getGraphNode(item)

            // Track the graph node to use in the after() interceptor method
            log.info("before: tracking item change - $item.domainType '$item.label' [$item.id] - $item.path")
            DeleteItemState deleteItemState = new DeleteItemState(
                versionedFolderTreeItem.id,
                item.path.toString(),
                graphNode)
            request.deleteItemState = deleteItemState
        }
        catch (Exception exception) {
            log.error("Cannot get graph node for '$item.label' [$item.id]: $exception.message", exception)
        }
    }

    private MdmDomain getItem() {
        String id = params.id

        if (controllerName == "folder") {
            return Folder.get(id)
        }

        if (controllerName == "dataModel") {
            return DataModel.get(id)
        }

        if (controllerName == "dataClass") {
            return DataClass.get(id)
        }
        if (controllerName == "dataElement") {
            return DataElement.get(id)
        }

        if (controllerName == "term") {
            return Term.get(id)
        }

        null
    }

    private ContainerTreeItem getAncestors(MdmDomain item) {
        if (item.class == Folder.class) {
            return treeItemService.buildContainerTreeWithAncestors(
                item as Container,
                currentUserSecurityPolicyManager)
        }

        treeItemService.buildCatalogueItemTreeWithAncestors(
            Folder.class,
            item as CatalogueItem,
            currentUserSecurityPolicyManager)
    }

    private TreeItem getAncestorVersionedFolder(TreeItem treeItem) {
        if (treeItem.domainType == VersionedFolder.class.simpleName) {
            return treeItem
        }

        if (treeItem.children.size() == 1) {
            return getAncestorVersionedFolder(treeItem.children.first())
        }

        null
    }

    class DeleteItemState {
        final UUID versionedFolderId
        final String path
        final GraphNode graphNode

        DeleteItemState(UUID versionedFolderId, String path, GraphNode graphNode) {
            this.versionedFolderId = versionedFolderId
            this.path = path
            this.graphNode = graphNode
        }
    }
}
