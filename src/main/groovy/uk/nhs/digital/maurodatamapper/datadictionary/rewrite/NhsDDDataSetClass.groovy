package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

class NhsDDDataSetClass {

    String mandation
    String minMultiplicity
    String maxMultiplicity
    String constraints

    List<NhsDDDataSetClass> dataSetClasses
    List<NhsDDDataSetElement> dataSetElements

}
