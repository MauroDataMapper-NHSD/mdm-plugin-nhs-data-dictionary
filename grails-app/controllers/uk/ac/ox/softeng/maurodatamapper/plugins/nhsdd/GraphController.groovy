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

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmController
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.artefact.Controller
import grails.artefact.controller.RestResponder
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary

import static org.springframework.http.HttpStatus.NO_CONTENT

/**
 * Controller for endpoints to manage the Data Dictionary graph.
 */
@Slf4j
class GraphController implements MdmController, Controller, RestResponder {
    GraphService graphService
    VersionedFolderService versionedFolderService
    NhsDataDictionaryService nhsDataDictionaryService
    AsyncJobService asyncJobService

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

        if (params.asynchronous) {
            AsyncJob asyncJob = asyncJobService.createAndSaveAsyncJob(
                "Rebuild graph nodes for Data Dictionary branch '$rootBranch.label\$$rootBranch.branchName' [$rootBranch.id]",
                currentUser) {
                rootBranch.attach()
                buildAllGraphNodes(rootBranch)
            }

            return respond(asyncJob, view: '/asyncJob/show', status: HttpStatus.ACCEPTED)
        }

        List<GraphNode> graphNodes = buildAllGraphNodes(rootBranch)
        def returnValues = [count: graphNodes.size()]
        respond(returnValues)
    }

    private List<GraphNode> buildAllGraphNodes(VersionedFolder rootBranch) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(rootBranch)

        List<MdmDomain> catalogueItems = dataDictionary.allComponents.collect {component ->
            component.catalogueItem
        }

        List<GraphNode> graphNodes = graphService.buildAllGraphNodes(rootBranch, catalogueItems)
        graphNodes
    }
}
