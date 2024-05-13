package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.interceptors

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass


class RenameInterceptor implements MdmInterceptor {

    String previousLink
    String previousName
    String previousPath

    RenameInterceptor() {
        match(controller: 'dataClass', action: 'update')
    }



    boolean before() {
        if(controllerName == 'dataClass') {
            DataClass dataClass = DataClass.get(params.id)
            previousName = dataClass.label
            previousPath = dataClass.path
            previousLink = "<a href=\"${previousPath.replaceAll('\\$[^|]*|', "")}\">${previousName}</a>"
            System.err.println(previousLink)
        }
        true
    }

    boolean after() {
        System.err.println("After")
        DataClass dataClass = DataClass.get(params.id)
        String newName = dataClass.label
        String newLink = "<a href=\"${dataClass.path}\">${dataClass.label}</a>"

        if(actionName == 'update' && response.status == 200 && newName != previousName) {
            // Run this in the background

            String controller = params.controller

            System.err.println(request.properties)
            //System.err.println(request.getInputStream().toString())

            Thread thread = Thread.start {

                if(controller == "dataClass") {
                    System.err.println("Renamed a data class")
                    System.err.println(dataClass.description)
                    if(dataClass.description.contains(previousLink)) {
                        System.err.println(dataClass.description.replace(previousLink, newLink))
                    }
                }


                // Find out whether this is something we need to worry about
                // Go through all the places where it's mentioned and alter them
                Thread.sleep(10000)
                System.err.println("Finished")
            }
        }
        true
    }

    void afterView() {
        // no-op
    }
}
