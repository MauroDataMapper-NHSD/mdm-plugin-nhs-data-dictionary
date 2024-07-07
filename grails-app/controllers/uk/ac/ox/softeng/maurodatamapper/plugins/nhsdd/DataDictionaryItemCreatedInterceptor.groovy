package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.ContainerTreeItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree.TreeItem
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.core.tree.TreeItemService
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.artefact.Interceptor
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity

import java.util.regex.Pattern

@Slf4j
class DataDictionaryItemCreatedInterceptor implements MdmInterceptor, Interceptor {
    static Pattern CONTROLLER_PATTERN = ~/(folder|dataModel|dataClass|dataElement|term)/

    GraphService graphService
    TreeItemService treeItemService
    VersionedFolderService versionedFolderService

    DataDictionaryItemCreatedInterceptor() {
        match controller: CONTROLLER_PATTERN, action: 'save'
    }

    @Override
    boolean after() {
        if (model.containsKey(controllerName)) {
            def mauroItem = model[controllerName]
            handleItem(mauroItem)
        }

        true
    }

    private <T extends MdmDomain & InformationAware & MetadataAware & GormEntity> void handleItem(T item) {
        // Must check if the catalogue item is within a versioned folder (NHS Data Dictionary branch)
        ContainerTreeItem ancestors = treeItemService.buildCatalogueItemTreeWithAncestors(
            Folder.class,
            item,
            currentUserSecurityPolicyManager)
        TreeItem versionedFolderTreeItem = getAncestorVersionedFolder(ancestors)
        if (!versionedFolderTreeItem) {
            log.debug("'$item.label' [$item.id] is not within a versioned folder, ignoring change tracking")
            return
        }

        try {
            VersionedFolder rootBranch = versionedFolderService.get(versionedFolderTreeItem.id)
            graphService.buildGraphNode(rootBranch, item)
        }
        catch (Exception exception) {
            log.error("Cannot build graph node for '$item.label' [$item.id]: $exception.message", exception)
        }
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
