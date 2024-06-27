package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.apache.commons.validator.routines.UrlValidator

import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
class ReferencedItemsService {
    public static final String METADATA_NAMESPACE = "uk.nhs.datadictionary.referencemap"

    /**
     * Metadata key for source items - the map of target items the source item "references"
     */
    public static final String METADATA_REFERENCES_KEY = "references"

    /**
     * Metadata key for target items - the map of source items this target item is "referenced in"
     */
    public static final String METADATA_REFERENCED_IN_KEY = "referencedIn"

    PathService pathService

    void buildReferenceMapsForSourceAndTargets(VersionedFolder dictionaryBranch, DataClass sourceItem) {
        if (!dictionaryBranch) {
            log.warn("No data dictionary branch provided")
            return
        }

        if (!sourceItem || !sourceItem.description || sourceItem.description.empty) {
            log.warn("Source item $sourceItem.domainType '$sourceItem.label' [$sourceItem.id] has no description")
            return
        }

        List<String> mauroPaths = getMauroPathsFromHtml(sourceItem.description)
        if (mauroPaths.empty) {
            log.info("Source item $sourceItem.domainType '$sourceItem.label' [$sourceItem.id] references no other Mauro items")
            return
        }

        List<MdmDomain> targetItems = getMauroItemsFromPaths(dictionaryBranch, mauroPaths)
        if (!targetItems || targetItems.empty) {
            log.error("Cannot find target items referenced by source item $sourceItem.domainType '$sourceItem.label' [$sourceItem.id]")
            return
        }

        // TODO: create map of target items and save to source item metadata
        def sourceItemMap = targetItems.collect { targetItem -> [
            id: targetItem.id,
            domainType: targetItem.domainType,
            path: targetItem.path.toString()
        ]}

        String sourceItemMapJson = JsonOutput.toJson(sourceItemMap)
        sourceItem.addToMetadata(METADATA_NAMESPACE, METADATA_REFERENCES_KEY, sourceItemMapJson)
        sourceItem.save([flush: true])

        // TODO: for each target item, update their reference maps to contain this source item


    }

    private static List<String> getMauroPathsFromHtml(String source) {
        Pattern pattern = ~/<a\s+(?:[^>]*?\s+)?href=(["'])(.*?)\1/
        Matcher matcher = pattern.matcher(source)

        def urlValidator = new UrlValidator()
        ArrayList mauroPaths = []

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

    private List<MdmDomain> getMauroItemsFromPaths(VersionedFolder dictionaryBranch, List<String> paths) {
        paths.collect { path -> pathService.findResourceByPathFromRootResource(dictionaryBranch, Path.from(path)) }
    }
}
