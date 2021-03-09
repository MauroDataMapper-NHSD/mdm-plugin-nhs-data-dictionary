package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class NhsDataDictionaryInterceptorSpec extends Specification implements InterceptorUnitTest<NhsDataDictionaryInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void "Test nhsDataDictionary interceptor matching"() {
        when:"A request matches the interceptor"
        withRequest(controller:"nhsDataDictionary")

        then:"The interceptor does match"
        interceptor.doesMatch()
    }
}
