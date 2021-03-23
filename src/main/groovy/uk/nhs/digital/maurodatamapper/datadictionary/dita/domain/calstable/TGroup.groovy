package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.DitaElement

class TGroup extends DitaElement {

    int cols
    List<ColSpec> colSpecs = []
    THead tHead
    TBody tBody = new TBody()

    @Override
    def writeAsXml(MarkupBuilder builder) {
        builder.tgroup (outputclass: outputClass, cols: "" + cols){
            colSpecs.each { colSpec ->
                colSpec.writeAsXml(builder)
            }
            if(tHead) {
                tHead.writeAsXml(builder)
            }
            tBody.writeAsXml(builder)
        }
    }
}
