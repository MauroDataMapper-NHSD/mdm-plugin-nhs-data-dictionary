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

import uk.ac.ox.softeng.maurodatamapper.path.Path

import groovy.json.JsonOutput

/**
 * Represents a node of a graph.
 *
 * This is used as part of tracing all items within a Data Dictionary branch, where each item in the
 * dictionary represents a "graph node", linked to successors and predecessors.
 *
 * For example, given these items and relationships: A --> B
 * - B is a "successor" to A
 * - A is a "predecessor" to B
 *
 * All successors and predecessors are represented as a set of Mauro paths e.g. "dm:Model$branch|dc:Class|de:Element"
 */
class GraphNode {
    Set<String> predecessors = []
    Set<String> successors = []

    String toJson() {
        JsonOutput.toJson(this)
    }

    boolean hasSuccessor(String path) {
        successors.contains(path)
    }

    boolean hasSuccessor(Path path) {
        hasSuccessor(path.toString())
    }

    boolean hasPredecessor(String path) {
        predecessors.contains(path)
    }

    boolean hasPredecessor(Path path) {
        hasPredecessor(path.toString())
    }

    void addSuccessor(String path) {
        successors.add(path)
    }

    void addSuccessor(Path path) {
        addSuccessor(path.toString())
    }

    void addPredecessor(String path) {
        predecessors.add(path)
    }

    void addPredecessor(Path path) {
        addPredecessor(path.toString())
    }

    void removeSuccessor(String path) {
        successors.remove(path)
    }

    void removeSuccessor(Path path) {
        removeSuccessor(path.toString())
    }

    void removePredecessor(String path) {
        predecessors.remove(path)
    }

    void removePredecessor(Path path) {
        removePredecessor(path.toString())
    }

    void clearSuccessors() {
        successors.clear()
    }

    void clearPredecessors() {
        predecessors.clear()
    }

    void replaceSuccessor(String original, String replacement) {
        removeSuccessor(original)
        addSuccessor(replacement)
    }

    void replacePredecessor(String original, String replacement) {
        removePredecessor(original)
        addPredecessor(replacement)
    }
}
