package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.interceptors.DataDictionaryItemUpdatedInterceptor

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class DataDictionaryItemUpdatedInterceptorSpec extends Specification implements InterceptorUnitTest<DataDictionaryItemUpdatedInterceptor> {
    void "should match against '#controller' and the '#action' action"(String controller, String action) {
        when:
        withRequest(controller: controller, action: action)

        then:
        interceptor.doesMatch()

        where:
        controller      | action
        "folder"        | "update"
        "dataModel"     | "update"
        "dataClass"     | "update"
        "dataElement"   | "update"
        "term"          | "update"
    }

    void "should not match against '#controller' and the '#action' action"(String controller, String action) {
        when:
        withRequest(controller: controller, action: action)

        then:
        !interceptor.doesMatch()

        where:
        controller      | action
        "folder"        | "delete"
        "dataModel"     | "delete"
        "dataClass"     | "delete"
        "dataElement"   | "delete"
        "term"          | "delete"
        "folder"        | "save"
        "dataModel"     | "save"
        "dataClass"     | "save"
        "dataElement"   | "save"
        "term"          | "save"
        "folder"        | "show"
        "dataModel"     | "show"
        "dataClass"     | "show"
        "dataElement"   | "show"
        "term"          | "show"
    }
}
