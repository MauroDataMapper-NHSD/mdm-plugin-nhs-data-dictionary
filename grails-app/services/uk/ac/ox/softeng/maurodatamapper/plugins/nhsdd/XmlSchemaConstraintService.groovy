package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd


import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import grails.gorm.transactions.Transactional
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionary

@Transactional
class XmlSchemaConstraintService extends DataDictionaryComponentService <Term> {

    @Override
    Map indexMap(Term catalogueItem) {
        [
            catalogueId: catalogueItem.id.toString(),
            name: catalogueItem.code,
            stereotype: "xmlSchemaConstraint"

        ]
    }

    @Override
    def show(String id) {
        Term xmlSchemaConstraint = termService.get(id)

        String description = xmlSchemaConstraint.description

        def result = [
                catalogueId: xmlSchemaConstraint.id.toString(),
                name: xmlSchemaConstraint.code,
                stereotype: "xmlSchemaConstraint",
                shortDescription: getShortDescription(xmlSchemaConstraint),
                description: description,
                alsoKnownAs: getAliases(xmlSchemaConstraint)

        ]
        return result
    }

    @Override
    Set<Term> getAll() {
        Terminology supDefTerminology = terminologyService.findCurrentMainBranchByLabel(DataDictionary.XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME)
        supDefTerminology.terms.findAll{
            (!it.metadata.find{md -> md.namespace == DDHelperFunctions.metadataNamespace + ".XML schema constraint" &&
                        md.key == "isRetired" &&
                        md.value == "true"
                })}

    }

    @Override
    Term getItem(UUID id) {
        termService.get(id)
    }

    @Override
    String getMetadataNamespace() {
        DDHelperFunctions.metadataNamespace + ".XML schema constraint"
    }

    String getShortDescription(Term catalogueItem) {
        return catalogueItem.code
    }


}
