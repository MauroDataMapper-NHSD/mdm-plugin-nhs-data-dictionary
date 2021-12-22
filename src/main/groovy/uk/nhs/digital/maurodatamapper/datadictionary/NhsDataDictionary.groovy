package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

@Deprecated
class NhsDataDictionary {



    DataModel coreDataModel = null
    User currentUser = null

    Map<String, Terminology> attributeTerminologiesByName = [:]
    Map<String, DataElement> attributeElementsByUin = [:]
    Map<String, DataElement> attributeElementsByName = [:]

    Map<String, DataElement> elementsByUrl = [:]
    Map<String, DataElement> elementsByName = [:]

    Map<String, DataClass> classesByUin = [:]
    Map<String, DataClass> classesByName = [:]

    List<ClassLink> classLinks = []
    Map<String, ReferenceType> classReferenceTypesByName = [:]

    Map<String, DataModel> dataSetsByName = [:]

    Map<String, Term> businessDefinitionsByName = [:]
    Map<String, Term> supportingInformationByName = [:]
    Map<String, Term> xmlSchemaConstraintsByName = [:]


    static XmlParser xmlParser = XmlParser.newInstance()


}
