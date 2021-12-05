package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology

class NhsDataDictionary {

    static String FOLDER_NAME = "NHS Data Dictionary"
    static String CORE_MODEL_NAME = "NHS Data Dictionary - Core"
    static String ATTRIBUTES_CLASS_NAME = "Attributes"
    static String DATA_FIELD_NOTES_CLASS_NAME = "Data Field Notes"
    static String DATA_CLASSES_CLASS_NAME = "Classes"
    static String DATA_SETS_FOLDER_NAME = "Data Sets"
    static String BUSINESS_DEFINITIONS_TERMINOLOGY_NAME = "NHS Business Definitions"
    static String SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME = "Supporting Information"
    static String XML_SCHEMA_CONSTRAINTS_TERMINOLOGY_NAME = "XML Schema Constraints"
    static String WEB_PAGES_TERMINOLOGY_NAME = "Web Pages"

    DataModel coreDataModel = null
    User currentUser = null

    Map<String, Terminology> attributeTerminologiesByName = [:]
    Map<String, DataElement> attributeElementsByUin = [:]

    Map<String, DataElement> elementsByUrl = [:]

    Map<String, DataClass> classesByUin = [:]
    List<ClassLink> classLinks = []

    Map<String, ReferenceType> classReferenceTypesByName = [:]

    static XmlParser xmlParser = XmlParser.newInstance()


}
