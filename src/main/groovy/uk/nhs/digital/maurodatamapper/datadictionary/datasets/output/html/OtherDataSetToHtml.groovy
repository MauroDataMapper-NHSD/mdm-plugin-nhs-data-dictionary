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
            markupBuilder.b {
                "One of the following options must be used:"
            }
            ddDataClass.dataSetClasses.sort {it.webOrder}.
                eachWithIndex{childDataClass, int idx ->
                    if (idx != 0) {
                        markupBuilder.b {
                            "Or"
                        }
                    }
                    outputClassAsHtml(childDataClass)
                }
        } else {
            markupBuilder.table (class:"simpletable table table-sm") {
                thead {
                    tr {
                        th(colspan: 2, class: "thead-light", align: 'center') {
                            b {
                                ddDataClass.name
                            }
                            if (ddDataClass.description) {
                                p {
                                    ddDataClass.description
                                }
                            }
                        }
                    }
                }
                tbody {
                    if (ddDataClass.dataSetElements) {
                        markupBuilder.tr {
                            td(style: "width: 20%; text-align: center;") {
                                b {
                                    "Mandation"
                                }
                            }
                            td(style: "width: 80%; text-align: center;") {
                                b {
                                    "Data Elements"
                                }
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
                if (childClass.isChoice && idx != 0) {
                    markupBuilder.tr {
                        td(colspan: 2, align: 'center') {
                            b {
                                'Or'
                            }
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
            td (width: '20%', align: 'center') {
                p {
                    dataSetElement.mandation
                }
            }
            td (width: '80%') {
                dataSetElement.createLink(markupBuilder)
            }
        }
    }

    void addSubClass(NhsDDDataSetClass dataSetClass) {
        if(dataSetClass.name != 'Choice') {
            markupBuilder.tr(class: 'table-primary') {
                td(style: "width: 20%;") {b "Mandation"}
                td(style: "width: 80%;") {
                    p {
                        b {
                            dataSetClass.name
                        }
                    }
                    if(dataSetClass.description) {
                        markupBuilder.p {
                            dataSetClass.description
                        }
                    }
                }
            }
        }
        addAllChildRows(dataSetClass)
    }



}
