package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.interceptors


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.BaseDataDictionaryFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import grails.gorm.transactions.Transactional
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import spock.lang.Shared
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary

/**
 * Base specification for all of the custom interceptors for maintaining the data dictionary graph.
 */
@Slf4j
abstract class BaseDataDictionaryItemTrackerFunctionalSpec extends BaseDataDictionaryFunctionalSpec {
    // Trace the item layout of a test data dictionary
    @Shared
    VersionedFolder dictionaryBranch
    @Shared
    Folder dataSetsFolder
    @Shared
    Folder dataSetsSubFolder
    @Shared
    DataModel dataSetModel
    @Shared
    PrimitiveType dataSetType
    @Shared
    DataClass dataSetClass
    @Shared
    DataElement dataSetElement
    @Shared
    DataModel nhsClassesAndAttributes
    @Shared
    PrimitiveType nhsClassesAndAttributesType
    @Shared
    DataClass nhsClass
    @Shared
    DataElement nhsAttribute
    @Shared
    Terminology nhsBusinessDefinitions
    @Shared
    Term nhsBusinessTerm

    @RunOnce
    @Transactional
    def setup() {
        dictionaryBranch = given."there is a versioned folder"("NHS Data Dictionary")

        // Data Sets
        dataSetsFolder = given."there is a folder"(NhsDataDictionary.DATA_SETS_FOLDER_NAME, "", dictionaryBranch)
        dataSetsSubFolder = given."there is a folder"("Sample Data Sets", "", dataSetsFolder)
        dataSetModel = given."there is a data model"("Critical Care Data Set", dataSetsSubFolder)
        dataSetType = given."there is a primitive data type"("dataSetType", dataSetModel)
        dataSetClass = given."there is a data class"("Data Set Table", dataSetModel)
        dataSetElement = given."there is a data element"("Data Set Element", dataSetModel, dataSetClass, dataSetType)

        // Classes and Attributes
        nhsClassesAndAttributes = given."there is a data model"(NhsDataDictionary.CLASSES_MODEL_NAME, dictionaryBranch)
        nhsClassesAndAttributesType = given."there is a primitive data type"("classesAndAttributesType", nhsClassesAndAttributes)
        nhsClass = given."there is a data class"("APPOINTMENT", nhsClassesAndAttributes)
        nhsAttribute = given."there is a data element"("APPOINTMENT DATE", nhsClassesAndAttributes, nhsClass, nhsClassesAndAttributesType)

        // NHS Business Definitions
        nhsBusinessDefinitions = given."there is a terminology"(NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME, dictionaryBranch)
        nhsBusinessTerm = given."there is a term"("ABO System", "ABO System", nhsBusinessDefinitions)

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug("Cleanup specification")
        cleanUpResources(DataModel, PrimitiveType, DataClass, DataElement, Terminology, Term, Folder, VersionedFolder)
    }

    Map buildNewItemRequestBody(
        String domainType,
        String label,
        String description) {
        if (domainType == "terms") {
            return [
                code: label,
                definition: label,
                description: description
            ]
        }

        if (domainType == "dataElements") {
            return [
                label: label,
                dataType: dataSetType.id,   // Assume that the data element is being created under the Critical Care Data Set
                description: description
            ]
        }

        return [
            label: label,
            description: description
        ]
    }

    Map buildUpdateItemRequestBody(
        String domainType,
        String label) {
        if (domainType == "terms") {
            return [
                code: label,
                definition: label
            ]
        }

        return [
            label: label
        ]
    }
}
