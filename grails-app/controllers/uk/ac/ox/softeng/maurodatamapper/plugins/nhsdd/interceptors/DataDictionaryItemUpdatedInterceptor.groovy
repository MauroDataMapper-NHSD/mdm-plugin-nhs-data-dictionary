package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.interceptors

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor

import grails.artefact.Interceptor
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

@Slf4j
class DataDictionaryItemUpdatedInterceptor implements MdmInterceptor, Interceptor {
    static Pattern CONTROLLER_PATTERN = ~/(folder|dataModel|dataClass|dataElement|term)/

    DataDictionaryItemUpdatedInterceptor() {
        match controller: CONTROLLER_PATTERN, action: 'update'
    }

    @Override
    boolean before() {
        true
    }

    @Override
    boolean after() {
        true
    }
}
