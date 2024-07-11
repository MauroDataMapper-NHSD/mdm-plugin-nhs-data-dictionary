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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.apache.commons.validator.routines.UrlValidator
import org.grails.datastore.gorm.GormEntity
import org.springframework.beans.factory.annotation.Autowired

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Service to handle all graph operations on a Data Dictionary branch.
 *
 * A Data Dictionary branch is equivalent to a Versioned Folder.
 *
 * Th2 graph is a representation of how dictionary items are related to each other. The description fields
 * of dictionary items contain hyperlinks to other dictionary items, so they build up a relationship to each other.
 * Each dictionary item with these links to Mauro paths will automatically have a {@link GraphNode} attached, with
 * the following properties:
 *
 * - successors - A list of Mauro paths referred to in an items description field - links going "out"
 * - predecessors - A list of Mauro paths which refer to an item in their description field - links coming "in"
 *
 * For example, given these two Data Classes, they would have these graph nodes:
 *
 * - Label: ACCOMMODATION
 * - Path: dm:Classes and Attributes$main|dc:ACCOMMODATION
 * - Description: "Refers to <a href='dm:Classes and Attributes$main|dc:ACTIVITY'>an item</a>"
 * - Graph node: successors: [dm:Classes and Attributes$main|dc:ACTIVITY], predecessors: []
 *
 * - Label: ACTIVITY
 * - Path: dm:Classes and Attributes$main|dc:ACTIVITY
 * - Graph node: successors: [], predecessors: [dm:Classes and Attributes$main|dc:ACCOMMODATION]
 *
 * All {@link GraphNode} objects for every item are stored in a metadata key under that item and fetched/updated
 * via this service.
 */
@Slf4j
class GraphService {
    public static final String METADATA_NAMESPACE = "uk.nhs.datadictionary.graph"
    public static final String METADATA_KEY = "graphNode"

    /**
     * These are the only Mauro domain types we choose to add graph nodes to.
     */
    private static List<Class> ACCEPTED_RESOURCE_CLASSES = [
        DataModel.class,
        DataClass.class,
        DataElement.class,
        Folder.class,
        Term.class
    ]

    JsonSlurper jsonSlurper = new JsonSlurper()

    PathService pathService
    MetadataService metadataService

    @Autowired(required = false)
    List<MdmDomainService> domainServices

    boolean isAcceptableGraphResourceClass(Class resourceClass) {
        resourceClass in ACCEPTED_RESOURCE_CLASSES
    }

    <T extends MdmDomain & MetadataAware & GormEntity> T findGraphResource(Class resourceClass, UUID resourceId) {
        if (!isAcceptableGraphResourceClass(resourceClass)) {
            throw new ApiBadRequestException("GS01", "Unacceptable graph resource '$resourceClass.simpleName'")
        }

        MdmDomainService domainService = domainServices.find { service -> service.handles(resourceClass) }
        if (!domainService) {
            throw new ApiBadRequestException("GS02", "No domain service available to handle graph resource '$resourceClass.simpleName'")
        }

        domainService.get(resourceId) as T
    }

    <T extends MdmDomain & MetadataAware & GormEntity> T findGraphResourceByPath(String path) {
        Path pathObject = Path.from(path)
        Class<Object> resourceClass = identifyResourceClassFromPath(pathObject)
        T item = pathService.findResourceByPathFromRootClass(resourceClass, pathObject) as T
        if (!item) {
            log.warn("Cannot find graph resource using path '$path'")
            return null
        }

        item
    }

    GraphNode getGraphNode(MetadataAware item) {
        Metadata metadata = item.findMetadataByNamespaceAndKey(METADATA_NAMESPACE, METADATA_KEY)
        if (!metadata || !metadata.value) {
            return new GraphNode()
        }

        jsonSlurper.parseText(metadata.value) as GraphNode
    }

    <T extends MdmDomain & MetadataAware & GormEntity> void saveGraphNode(T item, GraphNode graphNode) {
        Metadata metadata = item.findMetadataByNamespaceAndKey(METADATA_NAMESPACE, METADATA_KEY)

        if (!metadata) {
            item.addToMetadata(METADATA_NAMESPACE, METADATA_KEY, graphNode.toJson(), item.createdBy)
            item.save([flush: true])
            return
        }

        metadata.value = graphNode.toJson()
        metadata.save([flush: true])
    }

    <T extends MdmDomain & MetadataAware & GormEntity> void deleteGraphNode(T item) {
        Metadata metadata = item.findMetadataByNamespaceAndKey(METADATA_NAMESPACE, METADATA_KEY)
        if (metadata) {
            item.metadata.remove(metadata)
            metadataService.delete(metadata, true)
        }
    }

    /**
     * Build a graph node for a Mauro item. Use this when an item has been created or updated.
     * @param rootBranch The Versioned Folder this item is a descendant of.
     * @param item The item to update.
     * @return The new or updated {@link GraphNode}.
     *
     * Building a graph node requires scanning the item's description field for links to Mauro paths. All of
     * these found paths become "successors" to the item passed in, then all of these successors will be updated
     * to add the item passed in as a "predecessor".
     */
    <T extends MdmDomain & InformationAware & MetadataAware & GormEntity> GraphNode buildGraphNode(
        VersionedFolder rootBranch,
        T item) {
        GraphNode graphNode = getGraphNode(item)

        if (!item.description || item.description.empty) {
            log.debug("No successors found for '$item.path' under root '$rootBranch.label\$$rootBranch.branchName' [$rootBranch.id]")
            return graphNode
        }

        Set<String> descriptionSuccessorPaths = getSuccessorPathsFromHtml(
            item.description,
            rootBranch.branchName)

        // Locate all successors that have now been removed from the description and
        // update all of them to remove this predecessor
        graphNode.successors
            .findAll { successorPath -> !descriptionSuccessorPaths.contains(successorPath) }
            .forEach { successorPath ->
                updatePredecessorInSuccessorGraphNode(rootBranch, successorPath, item, true)

                log.info("Removing successor '$successorPath' from '$item.path' under root '$rootBranch.label\$$rootBranch.branchName' [$rootBranch.id]")
                graphNode.removeSuccessor(successorPath)
            }

        // Locate all new successors added to the description and update all of them to track
        // their new predecessor
        descriptionSuccessorPaths
            .findAll { successorPath -> !graphNode.hasSuccessor(successorPath) }
            .forEach { successorPath ->
            log.info("Adding successor '$successorPath' to '$item.path' under root '$rootBranch.label\$$rootBranch.branchName' [$rootBranch.id]")
            graphNode.addSuccessor(successorPath)

            updatePredecessorInSuccessorGraphNode(rootBranch, successorPath, item, false)
        }

        saveGraphNode(item, graphNode)

        graphNode
    }

    /**
     * Rebuild all graph nodes under a provided Versioned Folder.
     * @param rootBranch The Versioned Folder these items are a descendant of.
     * @param items The list of items to rebuild.
     */
    <T extends MdmDomain & InformationAware & MetadataAware & GormEntity> List<GraphNode> buildAllGraphNodes(
        VersionedFolder rootBranch,
        List<T> items) {
        List<GraphNode> graphNodes = []

        long startTime = System.currentTimeMillis()

        items.forEach { item -> {
            try {
                log.info("Removing graph node for $item.domainType '$item.label' [$item.id]")
                deleteGraphNode(item)
            }
            catch (Exception exception) {
                log.error("Error removing graph node: $exception.message", exception)
            }
        }}

        items.forEach { item ->
            try {
                log.info("Rebuilding graph node for $item.domainType '$item.label' [$item.id]")
                GraphNode graphNode = buildGraphNode(rootBranch, item)
                graphNodes.add(graphNode)
            }
            catch (Exception exception) {
                log.error("Error removing graph node: $exception.message", exception)
            }
        }

        log.info("Built all graph nodes under '$rootBranch\$$rootBranch.branchName' [$rootBranch.id] in ${Utils.timeTaken(startTime)}")

        graphNodes
    }

    /**
     * Remove references to an item (the "predecessor) in other graph nodes. Use this when removing an item.
     * @param rootBranch The ancestor Versioned Folder.
     * @param predecessorPath The path of the item that was removed.
     * @param predecessorGraphNode The {@link GraphNode} of the item that was removed, containing the successor
     * list to track and update
     */
    void removePredecessorFromSuccessorGraphNodes(
        VersionedFolder rootBranch,
        String predecessorPath,
        GraphNode predecessorGraphNode) {
        predecessorGraphNode.successors.forEach { successorPath ->
            updatePredecessorInSuccessorGraphNode(
                rootBranch,
                successorPath,
                predecessorPath,
                null,
                ModificationType.REMOVE)
        }

        predecessorGraphNode.clearSuccessors()
    }

    /**
     * Replaces references to an item (the "predecessor) in other graph nodes. Use this when removing an item
     * label has been changed or the path of the item has changed (e.g. "moved").
     * @param rootBranch The ancestor Versioned Folder.
     * @param originalPredecessorPath The original path of the predecessor.
     * @param replacementPredecessorPath The new path of the predecessor.
     * @param predecessorGraphNode The {@link GraphNode} of the item that was removed, containing the successor
     * list to track and update
     */
    void updatePredecessorFromSuccessorGraphNodes(
        VersionedFolder rootBranch,
        String originalPredecessorPath,
        String replacementPredecessorPath,
        GraphNode predecessorGraphNode) {
        predecessorGraphNode.successors.forEach { successorPath ->
            updatePredecessorInSuccessorGraphNode(
                rootBranch,
                successorPath,
                originalPredecessorPath,
                replacementPredecessorPath,
                ModificationType.UPDATE)
        }
    }

    private <T extends MdmDomain & InformationAware & MetadataAware & GormEntity> void updatePredecessorInSuccessorGraphNode(
        VersionedFolder rootBranch,
        String successorPath,
        T predecessorItem,
        boolean removing) {
        updatePredecessorInSuccessorGraphNode(
            rootBranch,
            successorPath,
            predecessorItem.path.toString(),
            null,
            removing ? ModificationType.REMOVE : ModificationType.ADD)
    }

    private <T extends MdmDomain & InformationAware & MetadataAware & GormEntity> void updatePredecessorInSuccessorGraphNode(
        VersionedFolder rootBranch,
        String successorPath,
        String predecessorPath,
        String replacementPredecessorPath,
        ModificationType modificationType) {
        // Need to locate the object via the path first. That means we need to know what resource class to search under
        T successorItem = findGraphResourceByPath(successorPath)
        if (!successorItem) {
            log.warn("Cannot find '$successorPath' under root '$rootBranch.label\$$rootBranch.branchName' [$rootBranch.id]")
            return
        }

        GraphNode successorGraphNode = getGraphNode(successorItem)
        if (modificationType == ModificationType.REMOVE) {
            log.info("Removing predecessor '$predecessorPath' from '$successorPath' under root '$rootBranch.label\$$rootBranch.branchName' [$rootBranch.id]")
            successorGraphNode.removePredecessor(predecessorPath)
        }
        else if (modificationType == ModificationType.UPDATE) {
            log.info("Updating predecessor '$predecessorPath' to '$replacementPredecessorPath' in '$successorPath' under root '$rootBranch.label\$$rootBranch.branchName' [$rootBranch.id]")
            successorGraphNode.replacePredecessor(predecessorPath, replacementPredecessorPath)
        }
        else {
            log.info("Adding predecessor '$predecessorPath' to '$successorPath' under root '$rootBranch.label\$$rootBranch.branchName' [$rootBranch.id]")
            successorGraphNode.addPredecessor(predecessorPath)
        }

        saveGraphNode(successorItem, successorGraphNode)
    }

    private static Set<String> getSuccessorPathsFromHtml(String source, String branchName) {
        Pattern pattern = ~/<a\s+(?:[^>]*?\s+)?href=(["'])(.*?)\1/
        Matcher matcher = pattern.matcher(source)

        def urlValidator = new UrlValidator()
        Set<String> mauroPaths = []

        for (index in 0..<matcher.count) {
            // The 3rd group in the regex match will be the href value
            String hrefValue = matcher[index][2]

            // Assume that non-valid URLs are actually Mauro paths
            if (!urlValidator.isValid(hrefValue)) {
                // It is possible that the path listed in the description is not well defined i.e. does not specify
                // a direct model branch and is implicit. We want to guarantee that the path is referring to
                // a model/item that is a direct descendant of a versioned folder, so modify the path to match the
                // same branch name
                Path path = createPathAimedAtBranch(hrefValue, branchName)
                mauroPaths.add(path.toString())
            }
        }

        mauroPaths
    }

    static Path createPathWithoutBranchName(Path path) {
        Path copy = path.clone()
        copy.pathNodes.forEach { node -> node.modelIdentifier = "" }
        copy
    }

    private static Path createPathAimedAtBranch(String pathString, String branchName) {
        Path path = Path.from(pathString)

        // Manually adjust the branch name of the path given for model types
        path.pathNodes
            .findAll { pathNode -> pathNode.prefix == "dm" || pathNode.prefix == "te" }
            .forEach { pathNode -> pathNode.modelIdentifier = branchName }

        path
    }

    private static <T> Class<T> identifyResourceClassFromPath(Path path) {
        String prefix = path.first().prefix
        if (prefix == "dm") {
            return DataModel.class
        }

        if (prefix == "te") {
            return Terminology.class
        }

        throw new Exception("Cannot identify resource class from path prefix '$prefix' for the path '$path'")
    }

    enum ModificationType {
        ADD,
        REMOVE,
        UPDATE
    }
}
