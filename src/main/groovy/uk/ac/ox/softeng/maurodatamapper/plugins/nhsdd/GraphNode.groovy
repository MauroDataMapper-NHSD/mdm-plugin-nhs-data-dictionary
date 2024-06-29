package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.path.Path

import groovy.json.JsonOutput

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
        successors.add(path.toString())
    }

    void addPredecessor(String path) {
        predecessors.add(path)
    }

    void addPredecessor(Path path) {
        predecessors.add(path.toString())
    }
}
