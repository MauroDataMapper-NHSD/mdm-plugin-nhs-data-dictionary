package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class RenameInterceptorSpec extends Specification implements InterceptorUnitTest<RenameInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void "Test rename interceptor matching"() {
        when:"A request matches the interceptor"
        withRequest(controller:"rename")

        then:"The interceptor does match"
        interceptor.doesMatch()
    }
}
