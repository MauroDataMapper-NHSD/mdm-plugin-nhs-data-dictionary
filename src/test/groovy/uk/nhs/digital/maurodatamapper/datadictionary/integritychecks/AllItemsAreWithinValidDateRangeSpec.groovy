package uk.nhs.digital.maurodatamapper.datadictionary.integritychecks

import spock.lang.Specification
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute

    class AllItemsAreWithinValidDateRangeSpec extends Specification {

        def "validateDateRange returns false for valid dates"() {
            given:
            def checker = new AllItemsAreWithinValidDateRange()
            def component = new NhsDDAttribute()

            component.otherProperties = ["validFrom": "31/12/3030", "validTo": "01/01/3040"]

            expect:
            checker.validateDateRange(component) == false
        }

        def "validateDateRange returns true for todate set before fromdate "() {
            given:
            def checker = new AllItemsAreWithinValidDateRange()
            def component = new NhsDDAttribute()
            component.otherProperties = ["validFrom": "31/12/2030", "validTo": "01/12/2030"]


            expect:
            checker.validateDateRange(component) == true
        }

        def "validateDateRange returns true when todate is set and fromdate is not"() {
            given:
            def checker = new AllItemsAreWithinValidDateRange()
            def component = new NhsDDAttribute()
            component.otherProperties = ["validTo": "01/12/2030"]

            expect:
            checker.validateDateRange(component) == true
        }

        def "validateDateRange returns true when todate has expired"() {
            given:
            def checker = new AllItemsAreWithinValidDateRange()
            def component = new NhsDDAttribute()
            component.otherProperties = ["validFrom": "31/12/2020", "validTo": "01/01/2020"]

            expect:
            checker.validateDateRange(component) == true
        }
    }

