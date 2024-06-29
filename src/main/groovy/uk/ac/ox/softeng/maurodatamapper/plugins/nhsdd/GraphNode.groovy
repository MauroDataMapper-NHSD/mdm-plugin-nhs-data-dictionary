package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import groovy.json.JsonOutput

class GraphNode {
    List<String> predecessors = []
    List<String> successors = []

    String toJson() {
        JsonOutput.toJson(this)
    }
}
