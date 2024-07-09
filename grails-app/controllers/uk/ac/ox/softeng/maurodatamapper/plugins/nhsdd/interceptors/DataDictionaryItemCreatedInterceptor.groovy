package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.interceptors

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.artefact.Interceptor
import groovy.util.logging.Slf4j

@Slf4j
class DataDictionaryItemCreatedInterceptor extends DataDictionaryItemTrackerInterceptor implements Interceptor {
    DataDictionaryItemCreatedInterceptor() {
        match controller: CONTROLLER_PATTERN, action: 'save'
    }

    @Override
    boolean after() {
        MdmDomain item = getMauroItemFromModel()
        if (!item) {
            return true
        }

        UUID versionedFolderId = getRootVersionedFolderId(item)
        if (!versionedFolderId) {
            // Won't track this item - assume not part of the NHS Data Dictionary
            return true
        }

        log.info("after: Creating graph node for '$item.path'")
        AsyncJob asyncJob = dataDictionaryItemTrackerService.asyncBuildGraphNode(versionedFolderId, item.domainType, item.id, currentUser)

        if (!asyncJob) {
            log.error("after: Failed to create async job")
        }
        else {
            log.info("after: Created new item and graph node build async job [$asyncJob.id] - status: $asyncJob.status")
        }

        true
    }
}
