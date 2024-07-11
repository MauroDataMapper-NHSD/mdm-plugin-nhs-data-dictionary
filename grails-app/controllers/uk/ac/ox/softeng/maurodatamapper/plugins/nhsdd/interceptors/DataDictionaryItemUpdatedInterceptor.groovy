/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.interceptors

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.artefact.Interceptor
import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus

/**
 * Custom interceptor for tracking when individual data dictionary items have been updated. There are two
 * possible update actions:
 *
 * 1. The description was changed - so the graph node is built/updated to add/remove successors/predecessors
 * that now match the new description.
 * 2. An item was renamed or moved - so the surrounding graph nodes are updated to refer to the new path
 * of this item.
 */
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
        boolean pathHasChanged = updateItemStateNext.path != updateItemStatePrevious.path

        if (!descriptionHasChanged && !pathHasChanged) {
            log.info("after: Ignoring item change for $updateItemStateNext.domainType '$updateItemStateNext.label' [$updateItemStateNext.id] - $updateItemStateNext.path - no significant changes found")
            return true
        }

        log.info("after: Starting async job to update graph node of $updateItemStateNext.domainType '$updateItemStateNext.label' [$updateItemStateNext.id] - $updateItemStateNext.path. (Original path: $updateItemStatePrevious.path)")
        AsyncJob asyncJob = null
        if (pathHasChanged) {
            asyncJob = dataDictionaryItemTrackerService.asyncUpdatePredecessorFromSuccessorGraphNodesAfterItemMoved(
                updateItemStateNext.versionedFolderId,
                updateItemStateNext.domainType,
                updateItemStateNext.id,
                updateItemStatePrevious.path.toString(),
                updateItemStateNext.path.toString(),
                currentUser
            )
        }
        else {
            asyncJob = dataDictionaryItemTrackerService.asyncBuildGraphNode(
            updateItemStateNext.versionedFolderId,
            updateItemStateNext.domainType,
            updateItemStateNext.id,
            currentUser)
        }

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

    private <T extends MdmDomain & InformationAware> UpdateItemState getUpdateItemStateFromModel(UUID versionedFolderId) {
        T item = getMauroItemFromModel()
        if (!item) {
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
