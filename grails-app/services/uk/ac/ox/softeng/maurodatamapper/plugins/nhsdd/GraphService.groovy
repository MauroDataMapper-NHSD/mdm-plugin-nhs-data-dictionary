package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.apache.commons.validator.routines.UrlValidator
import org.grails.datastore.gorm.GormEntity

import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
class GraphService {
    public static final String METADATA_NAMESPACE = "uk.nhs.datadictionary.graph"
    public static final String METADATA_KEY = "graphNode"

    JsonSlurper jsonSlurper = new JsonSlurper()

    PathService pathService

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

    <T extends MdmDomain & InformationAware & MetadataAware & GormEntity> void buildGraphNode(
        VersionedFolder rootBranch,
        T item) {
        GraphNode graphNode = getGraphNode(item)

        Set<String> descriptionSuccessorPaths = getSuccessorPathsFromHtml(item.description)

        // Locate all successors that have now been removed from the description and
        // update all of them to remove this predecessor
        graphNode.successors
            .findAll { successorPath -> !descriptionSuccessorPaths.contains(successorPath) }
            .forEach { successorPath ->
                updatePredecessorInSuccessorGraphNode(rootBranch, successorPath, item, true)

                log.info("Removing successor '$successorPath' from '$item.path' under root '$rootBranch.label$rootBranch.branchName' [$rootBranch.id]")
                graphNode.removeSuccessor(successorPath)
            }

        // Locate all new successors added to the description and update all of them to track
        // their new predecessor
        descriptionSuccessorPaths
            .findAll { successorPath -> !graphNode.hasSuccessor(successorPath) }
            .forEach { successorPath ->
            log.info("Adding successor '$successorPath' to '$item.path' under root '$rootBranch.label$rootBranch.branchName' [$rootBranch.id]")
            graphNode.addSuccessor(successorPath)

            updatePredecessorInSuccessorGraphNode(rootBranch, successorPath, item, false)
        }

        saveGraphNode(item, graphNode)
    }

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
        T successorItem = pathService.findResourceByPathFromRootResource(rootBranch, Path.from(successorPath)) as T
        if (!successorItem) {
            log.warn("Cannot find '$successorPath' under root '$rootBranch.label$rootBranch.branchName' [$rootBranch.id]")
            return
        }

        GraphNode successorGraphNode = getGraphNode(successorItem)
        if (modificationType == ModificationType.REMOVE) {
            log.info("Removing predecessor '$predecessorPath' from '$successorPath' under root '$rootBranch.label$rootBranch.branchName' [$rootBranch.id]")
            successorGraphNode.removePredecessor(predecessorPath)
        }
        else if (modificationType == ModificationType.UPDATE) {
            log.info("Updating predecessor '$predecessorPath' to '$replacementPredecessorPath' in '$successorPath' under root '$rootBranch.label$rootBranch.branchName' [$rootBranch.id]")
            successorGraphNode.replacePredecessor(predecessorPath, replacementPredecessorPath)
        }
        else {
            log.info("Adding predecessor '$predecessorPath' to '$successorPath' under root '$rootBranch.label$rootBranch.branchName' [$rootBranch.id]")
            successorGraphNode.addPredecessor(predecessorPath)
        }

        saveGraphNode(successorItem, successorGraphNode)
    }

    private static Set<String> getSuccessorPathsFromHtml(String source) {
        Pattern pattern = ~/<a\s+(?:[^>]*?\s+)?href=(["'])(.*?)\1/
        Matcher matcher = pattern.matcher(source)

        def urlValidator = new UrlValidator()
        Set<String> mauroPaths = []

        for (index in 0..<matcher.count) {
            // The 3rd group in the regex match will be the href value
            String hrefValue = matcher[index][2]

            // Assume that non-valid URLs are actually Mauro paths
            if (!urlValidator.isValid(hrefValue)) {
                mauroPaths.add(hrefValue)
            }
        }

        mauroPaths
    }

    enum ModificationType {
        ADD,
        REMOVE,
        UPDATE
    }
}
