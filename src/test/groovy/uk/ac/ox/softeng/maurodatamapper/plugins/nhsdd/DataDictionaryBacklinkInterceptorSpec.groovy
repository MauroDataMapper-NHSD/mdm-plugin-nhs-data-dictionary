package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class DataDictionaryBacklinkInterceptorSpec extends Specification implements InterceptorUnitTest<DataDictionaryBacklinkInterceptor> {
    void "should match against '#controller' and the '#action' action"(String controller, String action) {
        when:
        withRequest(controller: controller, action: action)

        then:
        interceptor.doesMatch()

        where:
        controller      | action
        "folder"        | "save"
        "dataModel"     | "save"
        "dataClass"     | "save"
        "dataElement"   | "save"
        "term"          | "save"
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
        "folder"        | "show"
        "dataModel"     | "show"
        "dataClass"     | "show"
        "dataElement"   | "show"
        "term"          | "show"
    }
}
