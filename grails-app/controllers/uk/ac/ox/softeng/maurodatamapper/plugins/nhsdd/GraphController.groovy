package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmController
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.artefact.Controller
import grails.artefact.controller.RestResponder
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary

import static org.springframework.http.HttpStatus.NO_CONTENT

@Slf4j
class GraphController implements MdmController, Controller, RestResponder {
    GraphService graphService
    VersionedFolderService versionedFolderService
    NhsDataDictionaryService nhsDataDictionaryService

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

    @Transactional
    def rebuildGraphNodes() {
        VersionedFolder rootBranch = versionedFolderService.get(params.versionedFolderId)
        if (!rootBranch) {
            return notFound(VersionedFolder.class, params.versionedFolderId)
        }

        NhsDataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(rootBranch)
        List<MdmDomain> catalogueItems = dataDictionary.allComponents.collect {component -> component.catalogueItem }
        List<GraphNode> graphNodes = graphService.buildAllGraphNodes(rootBranch, catalogueItems)

        def returnValues = [count: graphNodes.size()]
        respond(returnValues)
    }
}
