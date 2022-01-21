package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.integritychecks

import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionaryComponent

import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
class BrokenLinks implements IntegrityCheck {

    String name = "Broken Links in description"

    String description = "Check that all external links in descriptions lead to a valid web page"

    static Pattern pattern = Pattern.compile("<a[\\s]*href=\"(http[^\"]*)\"[\\s]*>([^<]*)</a>")

    @Override
    List<NhsDataDictionaryComponent> runCheck(NhsDataDictionary dataDictionary) {
        Map<String, List<NhsDataDictionaryComponent>> linkComponentMap = [:]
        List<NhsDataDictionaryComponent> errorComponents = []
        dataDictionary.allComponents.
            findAll {!it.isRetired() }.
            each {component ->
                Matcher matcher = pattern.matcher(component.definition)
                while(matcher.find()) {
                    List<NhsDataDictionaryComponent> components = linkComponentMap[matcher.group(1)]
                    if(components) {
                        components.add(component)
                    } else {
                        linkComponentMap[matcher.group(1)] = [component]
                    }
                }
            }
        linkComponentMap.each {link, componentList ->
            try {
                def code = new URL(link).openConnection().with {
                    requestMethod = 'HEAD'
                    connect()
                    responseCode
                }
                if(code == 404) {
                    errorComponents.addAll(componentList)
                }
            } catch(Exception e) {
                //log.info("${link}: Exception")
                errorComponents.addAll(componentList)
            }
        }
        errors = (errorComponents.toSet()).toList()
    }

}
