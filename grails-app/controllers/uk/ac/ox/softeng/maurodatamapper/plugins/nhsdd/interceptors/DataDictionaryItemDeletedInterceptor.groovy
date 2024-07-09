package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.interceptors

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.GraphNode
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.GraphService
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.artefact.Interceptor
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.springframework.http.HttpStatus

@Slf4j
class DataDictionaryItemDeletedInterceptor extends DataDictionaryItemTrackerInterceptor implements Interceptor {
    GraphService graphService

    DataDictionaryItemDeletedInterceptor() {
        match controller: CONTROLLER_PATTERN, action: 'delete'
    }

    @Override
    boolean before() {
        MdmDomain itemBeingDeleted = dataDictionaryItemTrackerService.getMauroItem(controllerName, getId())
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
        UUID versionedFolderId = getRootVersionedFolderId(item)
        if (!versionedFolderId) {
            // Won't track this item - assume not part of the NHS Data Dictionary
            return
        }

        try {
            GraphNode graphNode = graphService.getGraphNode(item)

            // Track the graph node to use in the after() interceptor method
            log.info("before: tracking item change - $item.domainType '$item.label' [$item.id] - $item.path")
            DeleteItemState deleteItemState = new DeleteItemState(
                versionedFolderId,
                item.path.toString(),
                graphNode)
            request.deleteItemState = deleteItemState
        }
        catch (Exception exception) {
            log.error("Cannot get graph node for '$item.label' [$item.id]: $exception.message", exception)
        }
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
