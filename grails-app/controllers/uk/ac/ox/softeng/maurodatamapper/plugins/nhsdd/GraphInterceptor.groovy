package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor

import grails.artefact.Interceptor

class GraphInterceptor implements MdmInterceptor, Interceptor {
    GraphService graphService

    @Override
    boolean before() {
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
