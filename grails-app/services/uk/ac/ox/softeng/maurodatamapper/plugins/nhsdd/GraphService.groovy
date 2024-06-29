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

    <T extends MetadataAware & GormEntity> void saveGraphNode(T item, GraphNode graphNode) {
        Metadata metadata = item.findMetadataByNamespaceAndKey(METADATA_NAMESPACE, METADATA_KEY)

        if (!metadata) {
            item.addToMetadata(METADATA_NAMESPACE, METADATA_KEY, graphNode.toJson())
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

        Set<String> successorPaths = getSuccessorPathsFromHtml(item.description)

        successorPaths.forEach { successorPath ->
            log.info("Adding successor '$successorPath' to '$item.path' under root '$rootBranch.label$rootBranch.branchName' [$rootBranch.id]")
            graphNode.addSuccessor(successorPath)

            updatePredecessorGraphNode(rootBranch, successorPath, item)
        }

        saveGraphNode(item, graphNode)
    }

    private <T extends MdmDomain & InformationAware & MetadataAware & GormEntity> void updatePredecessorGraphNode(
        VersionedFolder rootBranch,
        String successorPath,
        T predecessorItem) {
        T successorItem = pathService.findResourceByPathFromRootResource(rootBranch, Path.from(successorPath)) as T
        if (!successorItem) {
            log.warn("Cannot find '$successorPath' under root '$rootBranch.label$rootBranch.branchName' [$rootBranch.id]")
            return
        }

        log.info("Adding predecessor '$predecessorItem.path' to '$successorPath' under root '$rootBranch.label$rootBranch.branchName' [$rootBranch.id]")
        GraphNode successorGraphNode = getGraphNode(successorItem)
        successorGraphNode.addPredecessor(predecessorItem.path)
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
}
