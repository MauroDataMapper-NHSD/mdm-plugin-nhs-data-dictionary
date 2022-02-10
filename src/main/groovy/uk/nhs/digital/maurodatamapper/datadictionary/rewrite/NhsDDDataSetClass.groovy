package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.datasets.DataSetParser
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.ColSpec
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.Entry
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.Row
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.TBody
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.TGroup
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.THead
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.Table

class NhsDDDataSetClass {

    String mandation
    String minMultiplicity
    String maxMultiplicity
    String constraints
    Boolean isChoice

    List<NhsDDDataSetClass> dataSetClasses
    List<NhsDDDataSetElement> dataSetElements



}
