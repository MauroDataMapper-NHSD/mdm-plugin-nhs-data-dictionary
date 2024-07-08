package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.interceptors

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.artefact.Interceptor
import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus

@Slf4j
class DataDictionaryItemUpdatedInterceptor extends DataDictionaryItemTrackerInterceptor implements Interceptor {
    DataDictionaryItemUpdatedInterceptor() {
        match controller: CONTROLLER_PATTERN, action: 'update'
    }

    @Override
    boolean before() {
        // Fetch the original item state from Mauro before changes occur
        UpdateItemState updateItemState = getUpdateItemStateFromMauro()

        if (!updateItemState) {
            log.warn("before: Ignoring item update for controller: $controllerName, action: $actionName")
            return true
        }

        // Track the previous item state to compare in the after() interceptor method
        log.info("before: tracking item change - $updateItemState.domainType '$updateItemState.label' [$updateItemState.id] - $updateItemState.path")
        request.updateItemStatePrevious = updateItemState

        true
    }

    @Override
    boolean after() {
        if (response.status != HttpStatus.OK.value()) {
            log.warn("after: Ignoring item change for '$controllerName' [$params.id] - HttpStatus: $request")
            return true
        }

        // Check if the before() interceptor method was tracking a Mauro item for an item change. If not then this interceptor
        // doesn't need to do anything
        UpdateItemState updateItemStatePrevious = request.updateItemStatePrevious
        if (!updateItemStatePrevious) {
            log.warn("after: Ignoring item change for '$controllerName' [$params.id] - not tracking")
            return true
        }

        // Find the current state of the item now. Fetch from the model to be used by the view to reduce further calls to the
        // database
        UpdateItemState updateItemStateNext = getUpdateItemStateFromModel(updateItemStatePrevious.versionedFolderId)
        if (!updateItemStateNext) {
            log.warn("after: Ignoring item change for '$controllerName' [$params.id] - cannot get current item update state")
            return true
        }

        boolean descriptionHasChanged = updateItemStateNext.description.compareTo(updateItemStatePrevious.description) != 0

        // TODO: compare paths - was an item moved/renamed?

        if (!descriptionHasChanged) {
            log.info("after: Ignoring item change for $updateItemStateNext.domainType '$updateItemStateNext.label' [$updateItemStateNext.id] - $updateItemStateNext.path - no significant changes found")
            return true
        }

        log.info("after: Starting async job to update graph node of $updateItemStateNext.domainType '$updateItemStateNext.label' [$updateItemStateNext.id] - $updateItemStateNext.path. (Original path: $updateItemStatePrevious.path)")
        AsyncJob asyncJob = dataDictionaryItemTrackerService.asyncBuildGraphNode(
            updateItemStateNext.versionedFolderId,
            updateItemStateNext.domainType,
            updateItemStateNext.id,
            currentUser)

        if (!asyncJob) {
            log.error("after: Failed to create async job")
        }
        else {
            log.info("after: Created item updated and graph node update async job [$asyncJob.id] - status: $asyncJob.status")
        }

        true
    }

    private <T extends MdmDomain & InformationAware> UpdateItemState getUpdateItemStateFromMauro() {
        UUID id = getId()
        T item = dataDictionaryItemTrackerService.getMauroItem(controllerName, id) as T
        if (!item) {
            log.warn("Cannot find item [$id] using controller '$controllerName'")
            return null
        }

        UUID versionedFolderId = getRootVersionedFolderId(item)
        if (!versionedFolderId) {
            // Won't track this item - assume not part of the NHS Data Dictionary
            return null
        }

        new UpdateItemState(
            versionedFolderId,
            item.id,
            item.domainType,
            item.label,
            item.path,
            item.description)
    }

    private UUID getId() {
        if (!params.containsKey('id')) {
            return null
        }

        def id = params.id

        // Sometimes the domain interceptors have stored "id" from the URL route as UUIDs, sometimes as strings...
        if (id instanceof UUID) {
            return id as UUID
        }

        return UUID.fromString(id)
    }

    private <T extends MdmDomain & InformationAware> UpdateItemState getUpdateItemStateFromModel(UUID versionedFolderId) {
        if (!model) {
            log.warn("View has not returned a model!")
            return null
        }

        if (!model.containsKey(controllerName)) {
            log.warn("Cannot find model matching controller name '$controllerName'")
            return null
        }

        T item = model[controllerName] as T

        new UpdateItemState(
            versionedFolderId,
            item.id,
            item.domainType,
            item.label,
            item.path,
            item.description)
    }

    class UpdateItemState {
        final UUID versionedFolderId
        final UUID id
        final String domainType
        final String label
        final Path path
        final String description

        UpdateItemState(
            UUID versionedFolderId,
            UUID id,
            String domainType,
            String label,
            Path path,
            String description) {
            this.versionedFolderId = versionedFolderId
            this.id = id
            this.domainType = domainType
            this.label = label
            this.path = path
            this.description = description ?: ""
        }
    }
}
