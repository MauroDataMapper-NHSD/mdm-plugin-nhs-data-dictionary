package uk.nhs.digital.maurodatamapper.datadictionary

import spock.lang.Specification

class NhsDDClassRelationshipSpec extends Specification {
    void "should build the correct description"(
        String role,
        boolean hasMultiple,
        int minMultiplicity,
        int maxMultiplicity,
        boolean isChoice,
        String expected) {
        given: "a class relationship"
        def classRelationship = new NhsDDClassRelationship(
            role: role,
            hasMultiple: hasMultiple,
            minMultiplicity: minMultiplicity,
            maxMultiplicity: maxMultiplicity,
            isChoice: isChoice)

        when: "building the description"
        String actual = classRelationship.buildDescription()

        then: "the expected description is returned"
        verifyAll {
            actual == expected
        }

        where:
        role | hasMultiple | minMultiplicity | maxMultiplicity | isChoice | expected
        "supplied by" | false | 1 | 1 | false | "must be supplied by one and only one"
        "for" | false | 1 | 1 | false | "must be for one and only one"
        "located at" | false | 1 | 1 | true | "must be located at one and only one"
        "located at" | true | 1 | 1 | true | "or must be located at one and only one"
        "associated with" | false | 1 | -1 | false | "must be associated with one or more"
        "the subject of" | false | 0 | -1 | false | "may be the subject of one or more"
        "the source of" | false | 0 | 1 | false | "may be the source of one and only one"
    }
}
