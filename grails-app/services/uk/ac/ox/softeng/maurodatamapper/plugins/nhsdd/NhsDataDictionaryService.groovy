package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService

import grails.gorm.transactions.Transactional

@Transactional
class NhsDataDictionaryService {

    TerminologyService terminologyService
    DataModelService dataModelService

    def statistics(String branchName) {

        Terminology busDefTerminology = terminologyService.findCurrentMainBranchByLabel("NHS Business Definitions")
        Terminology supDefTerminology = terminologyService.findCurrentMainBranchByLabel("Supporting Information")
        Terminology xmlSchemaConstraintsTerminology = terminologyService.findCurrentMainBranchByLabel("Supporting Information")

        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel("NHS Data Dictionary - Core")

        DataClass classesClass = coreModel.dataClasses.find{ it.label == "Classes"}
        DataClass retiredClassesClass = classesClass.dataClasses.find{ it.label == "Retired"}

        DataClass attributesClass = coreModel.dataClasses.find{ it.label == "Attributes"}
        DataClass retiredAttributesClass = attributesClass.dataClasses.find{ it.label == "Retired"}

        DataClass elementsClass = coreModel.dataClasses.find{ it.label == "Data Field Notes"}
        DataClass retiredElementsClass = elementsClass.dataClasses.find{ it.label == "Retired"}


        return [
                "Attributes" : [
                        "Total" : attributesClass.dataElements.size() + retiredAttributesClass.dataElements.size(),
                        "Retired" : retiredAttributesClass.dataElements.size()
                ],
                "Data Field Notes" : [
                        "Total" : elementsClass.dataElements.size() + retiredElementsClass.dataElements.size(),
                        "Retired" : retiredElementsClass.dataElements.size()
                ],
                "Classes" : [
                        "Total" : classesClass.dataClasses.size() - 1 + retiredClassesClass.dataClasses.size(),
                        "Retired" : retiredClassesClass.dataClasses.size()
                ],
                "Business Definitions" : [
                        "Total" : busDefTerminology.terms.size(),
                        "Retired" : busDefTerminology.terms.count {it.metadata.find {it.key == "isRetired" && it.value == "true"}}
                ],
                "Supporting Information" : [
                        "Total" : supDefTerminology.terms.size(),
                        "Retired" : supDefTerminology.terms.count {it.metadata.find {it.key == "isRetired" && it.value == "true"}}
                ],
                "XML Schema Constraints" : [
                        "Total" : xmlSchemaConstraintsTerminology.terms.size(),
                        "Retired" : xmlSchemaConstraintsTerminology.terms.count {it.metadata.find {it.key == "isRetired" && it.value == "true"}}
                ]



        ]


    }
}
