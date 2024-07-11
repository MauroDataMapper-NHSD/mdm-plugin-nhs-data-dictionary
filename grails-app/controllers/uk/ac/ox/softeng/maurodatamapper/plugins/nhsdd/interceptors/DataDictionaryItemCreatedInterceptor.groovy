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
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.artefact.Interceptor
import groovy.util.logging.Slf4j

/**
 * Custom interceptor for tracking when individual data dictionary items have been created. Automatically
 * creates a new GraphNode for the new item.
 */
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
