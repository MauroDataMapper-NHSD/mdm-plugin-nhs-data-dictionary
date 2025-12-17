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

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ContainerTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.TreeItem
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.core.tree.TreeItemService
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.DataDictionaryItemTrackerService
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.artefact.Interceptor
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

/**
 * Custom interceptor base class for Data Dictionary items.
 *
 * These item tracker interceptors are designed to capture when certain actions happen in Mauro e.g.
 * an item is created/updated/deleted. In those cases, the data dictionary graph must be maintained
 * at the same time to ensure the graph is accurate all the time.
 */
@Slf4j
abstract class DataDictionaryItemTrackerInterceptor implements MdmInterceptor, Interceptor {
    static Pattern CONTROLLER_PATTERN = ~/(folder|dataModel|dataClass|dataElement|term)/

    DataDictionaryItemTrackerService dataDictionaryItemTrackerService
    TreeItemService treeItemService

    protected UUID getId() {
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

    protected <T extends MdmDomain & InformationAware> T getMauroItemFromModel() {
        if (!model) {
            log.warn("View has not returned a model!")
            return null
        }

        if (!model.containsKey(controllerName)) {
            log.warn("Cannot find model matching controller name '$controllerName'")
            return null
        }

        T item = model[controllerName] as T
        item
    }

    protected <T extends MdmDomain & InformationAware> UUID getRootVersionedFolderId(T item) {
        ContainerTreeItem ancestors = getAncestors(item)
        TreeItem versionedFolderTreeItem = getAncestorVersionedFolder(ancestors)
        if (!versionedFolderTreeItem) {
            log.warn("$item.domainType '$item.label' [$item.id] - $item.path is not within a versioned folder, not item tracking")
            return null
        }

        versionedFolderTreeItem.id
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
}
