package uk.nhs.digital.maurodatamapper.datadictionary.datasets.output.html

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSet
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetClass
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetElement

@Slf4j
class OtherDataSetToHtml {

    MarkupBuilder markupBuilder

    OtherDataSetToHtml(MarkupBuilder markupBuilder1) {
        this.markupBuilder = markupBuilder1
    }

    void outputAsHtml(NhsDDDataSet dataSet) {
        dataSet.dataSetClasses.sort {it.webOrder}.each {ddDataClass ->
            outputClassAsHtml(ddDataClass)
        }
    }

    void outputClassAsHtml(NhsDDDataSetClass ddDataClass) {
        if (ddDataClass.isChoice && ddDataClass.name.startsWith("Choice")) {
            markupBuilder.b 'One of the following options must be used:'
            ddDataClass.dataSetClasses.sort {it.webOrder}.
                eachWithIndex{childDataClass, int idx ->
                    if (idx != 0) {
                        markupBuilder.b 'Or'
                    }
                    outputClassAsHtml(childDataClass)
                }
        } else {
            markupBuilder.table (class: "simpletable table table-sm") {
                thead {
                    tr {
                        th(colspan: 2, class: "thead-light", 'text-align': 'center') {
                            b ddDataClass.name
                            if (ddDataClass.description) {
                                p ddDataClass.description
                            }
                        }
                    }
                }
                tbody {
                    if (ddDataClass.dataSetElements) {
                        markupBuilder.tr {
                            td(width: '20%', class: 'mandation-header') {
                                b "Mandation"

                            }
                            td(width: '80%', class: 'elements-header') {
                                b "Data Elements"
                            }
                        }
                    }

                    addAllChildRows(ddDataClass)
                }
            }
        }

    }

    void addAllChildRows(NhsDDDataSetClass ddDataClass) {
        if(ddDataClass.dataSetElements) {
            ddDataClass.dataSetElements.sort {it.webOrder}.each {dataElement ->
                addChildRow(dataElement)
            }
        } else { // No data elements
            ddDataClass.dataSetClasses.sort {it.webOrder}.eachWithIndex {childClass, idx ->
                if (ddDataClass.isChoice && idx != 0) {
                    markupBuilder.tr {
                        td(colspan: 2, class: 'or-header') {
                            b 'Or'
                        }
                    }
                } else if (idx != 0) {
                    markupBuilder.tr {
                        td(colspan: 2) { }
                    }
                }
                addSubClass(childClass)
            }
        }
    }

    void addChildRow(NhsDDDataSetElement dataSetElement) {
        markupBuilder.tr {
            td (width: '20%', class: 'mandation') {
                p dataSetElement.mandation

            }
            td (width: '80%') {
                dataSetElement.createLink(markupBuilder)
                if(dataSetElement.maxMultiplicity == '-1') {
                    p 'Multiple occurrences of this item are permitted'
                }
            }
        }
    }

    void addSubClass(NhsDDDataSetClass dataSetClass) {
        if(dataSetClass.name != 'Choice') {
            markupBuilder.tr(class: 'table-primary') {
                td(width: '20%', class: 'mandation-header') {
                    b 'Mandation'
                }
                td(width: '80%') {
                    p {
                        b dataSetClass.name
                    }
                    if(dataSetClass.description) {
                        markupBuilder.p dataSetClass.description
                    }
                }
            }
        }
        addAllChildRows(dataSetClass)
    }

}
