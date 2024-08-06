package uk.nhs.digital.maurodatamapper.datadictionary.publish

import grails.util.BuildSettings
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class DaisyDiffHelperSpec extends Specification {
    @Shared
    Path resourcesPath

    def setup() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'test', 'resources', 'html')
    }

    void "should diff HTML with basic markup"() {
        given: "there is a source html content"
        Path leftPath = resourcesPath.resolve("basic/left.html")
        String leftHtml = Files.readString(leftPath)

        and: "there is a target html content"
        Path rightPath = resourcesPath.resolve("basic/right.html")
        String rightHtml = Files.readString(rightPath)

        when: "html is diffed"
        String diffHtml = DaisyDiffHelper.diff(leftHtml, rightHtml)

        then: "the expected html is returned"
        String expected = "<p>The name of an area of work.</p><p>An area of work is an area, function <span class=\"diff-html-removed\" id=\"removed-diff-0\" previous=\"first-diff\" changeId=\"removed-diff-0\" next=\"last-diff\">or specialty </span>where work activity takes place.</p>"
        verifyAll {
            diffHtml.trim() == expected.trim()
        }
    }

    void "should not diff HTML which includes tables"() {
        given: "there is a source html content"
        Path leftPath = resourcesPath.resolve("complexHtmlTable/left.html")
        String leftHtml = Files.readString(leftPath)

        and: "there is a target html content"
        Path rightPath = resourcesPath.resolve("complexHtmlTable/right.html")
        String rightHtml = Files.readString(rightPath)

        when: "html is checked"
        boolean leftContainsHtmlTable = DaisyDiffHelper.containsHtmlTable(leftHtml)
        boolean rightContainsHtmlTable = DaisyDiffHelper.containsHtmlTable(rightHtml)

        then: "the expected html is returned"
        verifyAll {
            leftContainsHtmlTable
            rightContainsHtmlTable
        }
    }
}
