package uk.nhs.digital.maurodatamapper.datadictionary.integritychecks

import spock.lang.Specification
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute

class AllItemsAreWithinValidDateRangeSpec extends Specification {

    def "validateDateRange returns false for valid dates"() {
        given:
        def checker = new AllItemsAreWithinValidDateRange()
        def component = new NhsDDAttribute()

        component.otherProperties = ["validFrom": "3030-12-31", "validTo": "3040-01-01"]

        expect:
        checker.validateDateRange(component) == false
    }

    def "validateDateRange returns true for todate set before fromdate "() {
        given:
        def checker = new AllItemsAreWithinValidDateRange()
        def component = new NhsDDAttribute()
        component.otherProperties = ["validFrom": "2030-12-31", "validTo": "2030-12-01"]

        expect:
        checker.validateDateRange(component) == true
    }

    def "validateDateRange returns true when todate is set and fromdate is not"() {
        given:
        def checker = new AllItemsAreWithinValidDateRange()
        def component = new NhsDDAttribute()
        component.otherProperties = ["validTo": "2030-12-01"]

        expect:
        checker.validateDateRange(component) == true
    }

    def "validateDateRange returns true when todate has expired"() {
        given:
        def checker = new AllItemsAreWithinValidDateRange()
        def component = new NhsDDAttribute()
        component.otherProperties = ["validFrom": "2020-12-31", "validTo": "2020-01-01"]

        expect:
        checker.validateDateRange(component) == true
    }
}