package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor

import grails.artefact.Interceptor

class GraphInterceptor implements MdmInterceptor, Interceptor {
    GraphService graphService

    @Override
    boolean before() {
        mapDomainTypeToClass("graphResource", true)

        if (!graphService.isAcceptableGraphResourceClass(params.graphResourceClass)) {
            return notFound(params.graphResourceClass, params.graphResourceId)
        }

        params.versionedFolderId = UUID.fromString(params.versionedFolderId)

        if (!params.containsKey("graphResourceClass") || !params.containsKey("graphResourceId")) {
            return notFound(params.graphResourceClass, params.graphResourceId)
        }

        // TODO: return to this, how do I set proper security checks on this interceptor?
//        return currentUserSecurityPolicyManager.userCanReadSecuredResourceId(params.graphResourceClass, params.graphResourceId)
//            ?: notFound(params.graphResourceClass, params.graphResourceId)

        true
    }
}
