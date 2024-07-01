package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor

import grails.artefact.Interceptor

class GraphInterceptor implements MdmInterceptor, Interceptor {
    @Override
    boolean before() {
        mapDomainTypeToClass("catalogueItem", true)

        params.versionedFolderId = UUID.fromString(params.versionedFolderId)

        if (params.containsKey("catalogueItemResourceId")) {
            return currentUserSecurityPolicyManager.userCanReadSecuredResourceId(params.securableResourceClass, params.securableResourceId)
                ?: notFound(params.securableResourceClass, params.securableResourceId)
        }

        true
    }
}
