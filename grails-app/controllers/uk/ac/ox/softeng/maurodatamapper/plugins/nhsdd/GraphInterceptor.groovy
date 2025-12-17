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

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor

import grails.artefact.Interceptor

class GraphInterceptor implements MdmInterceptor, Interceptor {
    GraphService graphService

    @Override
    boolean before() {
        mapDomainTypeToClass("graphResource", false)

        params.asynchronous = params.containsKey('asynchronous') ? params.boolean('asynchronous') : false

        params.versionedFolderId = UUID.fromString(params.versionedFolderId)

        def canContinue = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(VersionedFolder.class, params.versionedFolderId)
            ?: notFound(params.graphResourceClass, params.graphResourceId)

        if (!canContinue) {
            return false
        }

        if (actionName == "rebuildGraphNodes") {
            return true
        }

        mapDomainTypeToClass("graphResource", true)

        if (!graphService.isAcceptableGraphResourceClass(params.graphResourceClass)) {
            return notFound(params.graphResourceClass, params.graphResourceId)
        }

        if (!params.containsKey("graphResourceClass") || !params.containsKey("graphResourceId")) {
            return notFound(params.graphResourceClass, params.graphResourceId)
        }

        true
    }
}
