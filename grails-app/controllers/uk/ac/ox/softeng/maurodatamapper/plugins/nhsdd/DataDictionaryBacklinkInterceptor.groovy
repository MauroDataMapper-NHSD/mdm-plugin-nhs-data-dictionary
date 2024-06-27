package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor

import grails.artefact.Interceptor
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

/**
 * A custom interceptor to detect when properties of an item change. Once detected, any
 * Mauro paths to other items contained in description fields are traced and followed to
 * their target, then a reference to the source item is maintained in the target item - this
 * is the "backlink". Backlinks will be used for mapping through the data dictionary and speed
 * up certain operations when items are changed.
 */
@Slf4j
class DataDictionaryBacklinkInterceptor implements MdmInterceptor, Interceptor {
    DataDictionaryBacklinkInterceptor() {
        Pattern controllerPattern = ~/(folder|dataModel|dataClass|dataElement|term)/

        // Capture any requests for these domains which create items, then trace their links
        match controller: controllerPattern, action: 'save'

        // Capture any requests for these domains which update their description
        match controller: controllerPattern, action: 'update'

        // TODO: track deletion of items?
    }

    @Override
    boolean after() {
        true
    }
}
