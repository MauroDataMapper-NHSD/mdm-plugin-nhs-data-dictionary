/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

import grails.artefact.Interceptor
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * An interceptor to detect when properties of an item change that will
 * require links to them being re-generated.  Works by intercepting both
 * before and after calls that update folders, models, classes and elements
 * then records the value of the "label" property before the update then
 * compares that value after the update.
 *
 * Currently does not consider access restrictions as it is assumed this has
 * already being taken care of.
 */
@CompileStatic
@Slf4j
class DataDictionaryItemChangeInterceptor implements MdmInterceptor, Interceptor {
    /**
     * The "before" handler saves the original label value against the id.
     * The id is assumed to be unique across all objects and that (currently)
     * we're only handling the value of the label.
     */
    private Map<String, String> originalValue = new HashMap<String, String>()

    DataDictionaryItemChangeInterceptor() {
        match controller: ~/(versionedFolder|folder|dataModel|dataClass|dataElement)/, action: 'update'
    }

    boolean before() {
        invokeHandler(false)
    }

    boolean after() {
        invokeHandler(true)
    }

    boolean invokeHandler(boolean isAfter) {
        final String methodName = "${controllerName}${actionName.capitalize()}"

        if (this.metaClass.respondsTo(this, methodName))
            return this.invokeMethod(methodName, isAfter)

        log.warn("${methodName}() not implemented")
        true
    }

    boolean versionedFolderUpdate(boolean isAfter) {
        String id = params["id"]

        if (isAfter) {
            final String original = originalValue.get(id)

            if (original != null) {
                Folder elm = Folder.get(id)
                if (elm.label != original) {
                    log.info("Folder instance ${id} changed label from \"${original}\" to \"${elm.label}\"")
                }
            }
        } else {
            originalValue.put(id, Folder.get(id).label)
        }
        true
    }

    // TODO: Does this need to be separate from versionedFolderUpdate?
    boolean folderUpdate(boolean isAfter) {
        String id = params["id"]

        if (isAfter) {
            final String original = originalValue.get(id)

            if (original != null) {
                Folder elm = Folder.get(id)
                if (elm.label != original) {
                    log.info("Folder instance ${id} changed label from \"${original}\" to \"${elm.label}\"")
                }
            }
        } else {
            originalValue.put(id, Folder.get(id).label)
        }
        true
    }

    boolean dataModelUpdate(boolean isAfter) {
        String id = params["id"]

        if (isAfter) {
            String original = originalValue.get(id)

            if (original != null) {
                DataModel elm = DataModel.get(id)
                if (elm.label != original) {
                    log.info("DataModel instance ${id} changed label from \"${original}\" to \"${elm.label}\"")
                }
            }
        } else {
            originalValue.put(id, DataModel.get(id).label)
        }
        true
    }

    boolean dataClassUpdate(boolean isAfter) {
        String id = params["id"]

        if (isAfter) {
            String original = originalValue.get(id)

            if (original != null) {
                DataClass elm = DataClass.get(id)
                if (elm.label != original) {
                    log.info("DataClass instance ${id} changed label from \"${original}\" to \"${elm.label}\"")
                }
            }
        } else {
            originalValue.put(id, DataClass.get(id).label)
        }
        true
    }

    boolean dataElementUpdate(boolean isAfter) {
        String id = params["id"]

        if (isAfter) {
            String original = originalValue.get(id)

            if (original != null) {
                DataElement elm = DataElement.get(id)
                if (elm.label != original) {
                    log.info("DataElement ${id} changed label from \"${original}\" to \"${elm.label}\"")
                }
            }
        } else {
            originalValue.put(id, DataElement.get(id).label)
        }
        true
    }

    void afterView() {
        // no-op
    }
}
