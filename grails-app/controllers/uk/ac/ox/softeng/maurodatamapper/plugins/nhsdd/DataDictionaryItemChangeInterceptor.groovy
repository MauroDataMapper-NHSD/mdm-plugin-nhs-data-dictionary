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
package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ContainerTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.TreeItem
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.core.tree.TreeItemService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import grails.artefact.Interceptor
import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus
/**
 * An interceptor to detect when properties of an item change that will
 * require links to them being re-generated.  Works by intercepting both
 * before and after calls that update folders, models, classes and elements
 * then records the value of the "label" property before the update then
 * compares that value after the update.
 *
 * Currently does not consider access restrictions as it is assumed this has
 * already being taken care of.
 */
@Slf4j
class DataDictionaryItemChangeInterceptor implements MdmInterceptor, Interceptor {
    TreeItemService treeItemService
    PathChangeService pathChangeService

    DataDictionaryItemChangeInterceptor() {
        match controller: ~/(versionedFolder|folder|dataModel|dataClass|dataElement|term)/, action: 'update'
    }

    boolean before() {
        ItemChangeState itemChangeState = getMauroItemChangeState(true)
        if (!itemChangeState) {
            log.warn("before: Ignoring item change state for controller: $controllerName, action: $actionName")
            return true
        }

        if (!itemChangeState.versionedFolderId) {
            log.warn("before: Ignoring item change, not in a versioned folder - $itemChangeState.domain '$itemChangeState.label' [$itemChangeState.id]")
            return true
        }

        // Track the previous item change state to compare in the after() interceptor method
        log.info("before: tracking item change - $itemChangeState.domain '$itemChangeState.label' [$itemChangeState.id] - $itemChangeState.path")
        request.itemChangeStatePrevious = itemChangeState

        true
    }

    boolean after() {
        if (response.status != HttpStatus.OK.value()) {
            log.warn("after: Ignoring item change for '$controllerName' [$params.id] - HttpStatus: $request")
            return true
        }

        // Check if the before() interceptor method was tracking a Mauro item for an item change. If not then this interceptor
        // doesn't need to do anything
        ItemChangeState itemChangeStatePrevious = request.itemChangeStatePrevious
        if (!itemChangeStatePrevious) {
            log.warn("after: Ignoring item change for '$controllerName' [$params.id] - not tracking")
            return true
        }

        ItemChangeState itemChangeStateNext = getMauroItemChangeState(false)
        if (!itemChangeStateNext) {
            log.warn("after: Ignoring item change for '$controllerName' [$params.id] - cannot get current item change state")
            return true
        }

        if (itemChangeStatePrevious.path == itemChangeStateNext.path) {
            log.warn("after: Ignoring item change for '$controllerName' [$params.id] - paths have not changed")
            return true
        }

        log.info("after: Starting async job to fix paths in data dictionary. Original path: $itemChangeStatePrevious.path, New path: $itemChangeStateNext.path")
        AsyncJob asyncJob = pathChangeService.asyncModifyRelatedItemsAfterPathChange(
            itemChangeStatePrevious.versionedFolderId,
            itemChangeStatePrevious.path,
            itemChangeStateNext.path,
            currentUser)

        if (!asyncJob) {
            log.error("after: Failed to create async job")
        }
        else {
            log.info("after: Created path change async job [$asyncJob.id] - status: $asyncJob.status")
        }

        true
    }

    ItemChangeState getMauroItemChangeState(boolean checkIfInVersionedFolder) {
        String id = params.id

        if (controllerName == "versionedFolder") {
            // Assume this versioned folder is the NHS Data Dictionary branch
            VersionedFolder container = VersionedFolder.get(id)
            return new ItemChangeState(
                id,
                container.class.simpleName,
                container.label,
                container.path,
                checkIfInVersionedFolder ? container.id : null)
        }

        if (controllerName == "folder") {
            Folder container = Folder.get(id)

            // Must check if the container is within a versioned folder (NHS Data Dictionary branch)
            UUID versionedFolderId = null
            if (checkIfInVersionedFolder) {
                ContainerTreeItem ancestors = treeItemService.buildContainerTreeWithAncestors(container, currentUserSecurityPolicyManager)
                TreeItem versionedFolderTreeItem = getAncestorVersionedFolder(ancestors)
                versionedFolderId = versionedFolderTreeItem?.id
            }

            return new ItemChangeState(
                id,
                container.class.simpleName,
                container.label,
                container.path,
                versionedFolderId)
        }

        // Must be a catalogue item
        CatalogueItem catalogueItem = null

        if (controllerName == "dataModel") {
            catalogueItem = DataModel.get(id)
        }
        else if (controllerName == "dataClass") {
            catalogueItem = DataClass.get(id)
        }
        else if (controllerName == "dataElement") {
            catalogueItem = DataElement.get(id)
        }
        else if (controllerName == "term") {
            catalogueItem = Term.get(id)
        }

        if (!catalogueItem) {
            return null
        }

        UUID versionedFolderId = null
        if (checkIfInVersionedFolder) {
            // Must check if the catalogue item is within a versioned folder (NHS Data Dictionary branch)
            ContainerTreeItem ancestors = treeItemService.buildCatalogueItemTreeWithAncestors(Folder.class, catalogueItem, currentUserSecurityPolicyManager)
            TreeItem versionedFolderTreeItem = getAncestorVersionedFolder(ancestors)
            versionedFolderId = versionedFolderTreeItem?.id
        }

        return new ItemChangeState(
            id,
            catalogueItem.class.simpleName,
            catalogueItem.label,
            catalogueItem.path,
            versionedFolderId)
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

    class ItemChangeState {
        final String id
        final String domain
        final String label
        final Path path
        final UUID versionedFolderId

        ItemChangeState(String id, String domain, String label, Path path, UUID versionedFolderId) {
            this.id = id
            this.domain = domain
            this.label = label
            this.path = path
            this.versionedFolderId = versionedFolderId
        }
    }
}
