package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmController
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource

import grails.artefact.Controller
import grails.artefact.controller.RestResponder
import grails.gorm.transactions.Transactional

import static org.springframework.http.HttpStatus.NO_CONTENT

class GraphController implements MdmController, Controller, RestResponder {
    GraphService graphService
    VersionedFolderService versionedFolderService

    @Override
    Class getResource() {
        return null
    }

    def show() {
        def resource = graphService.findGraphResource(params.graphResourceClass, params.graphResourceId)

        if (!resource) {
            return notFound(params.graphResourceClass, params.graphResourceId)
        }

        GraphNode graphNode = graphService.getGraphNode(resource)
        respond(graphNode)
    }

    @Transactional
    def buildGraphNode() {
        VersionedFolder rootBranch = versionedFolderService.get(params.versionedFolderId)
        if (!rootBranch) {
            return notFound(VersionedFolder.class, params.versionedFolderId)
        }

        def resource = graphService.findGraphResource(params.graphResourceClass, params.graphResourceId)

        if (!resource) {
            return notFound(params.graphResourceClass, params.graphResourceId)
        }

        GraphNode graphNode = graphService.buildGraphNode(rootBranch, resource)
        respond(graphNode)
    }

    @Transactional
    def removeGraphNode() {
        def resource = graphService.findGraphResource(params.graphResourceClass, params.graphResourceId)

        if (!resource) {
            return notFound(params.graphResourceClass, params.graphResourceId)
        }

        graphService.deleteGraphNode(resource)
        request.withFormat {
            '*' {
                render status: NO_CONTENT
            }
        }
    }
}
