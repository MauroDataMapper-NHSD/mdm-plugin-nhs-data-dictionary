package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class PathChangeService {
    AsyncJobService asyncJobService

    AsyncJob asyncModifyRelatedItemsAfterPathChange(UUID versionedFolderId, Path originalPath, Path updatedPath, User startedByUser) {
        asyncJobService.createAndSaveAsyncJob(
            "Path change update from $originalPath to $updatedPath",
            startedByUser,
            {
                modifyRelatedItemsAfterPathChange(versionedFolderId, originalPath, updatedPath)
            })
    }

    void modifyRelatedItemsAfterPathChange(UUID versionedFolderId, Path originalPath, Path updatedPath) {
        // TODO
    }
}
