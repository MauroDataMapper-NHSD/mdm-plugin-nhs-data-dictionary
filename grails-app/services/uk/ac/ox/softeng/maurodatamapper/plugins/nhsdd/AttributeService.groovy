package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ModelDataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDDCode
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary

@Slf4j
@Transactional
class AttributeService extends DataDictionaryComponentService<DataElement, NhsDDAttribute> {


    @Override
    def show(String branch, String id) {
        DataElement dataElement = dataElementService.get(id)

        String description = convertLinksInDescription(branch, dataElement.description)
        String shortDesc = replaceLinksInShortDescription(getShortDescription(dataElement, null))
        def result = [
            catalogueId     : dataElement.id.toString(),
            name            : dataElement.label,
            stereotype      : "attribute",
            shortDescription: shortDesc,
            description     : description,
            alsoKnownAs     : getAliases(dataElement)
        ]

        if (dataElement.dataType instanceof EnumerationType) {

            List<Map> enumerationValues = []

            ((EnumerationType) dataElement.dataType).enumerationValues.sort {
                enumValue ->
                    String webOrderString = DDHelperFunctions.getMetadataValue(enumValue, "Web Order")
                    if (webOrderString) {
                        return Integer.parseInt(webOrderString)
                    } else return 0

            }.each {enumValue ->

                Map enumValueMap = [:]

                enumValueMap["code"] = enumValue.key
                enumValueMap["description"] = enumValue.value
                String webPresentation = DDHelperFunctions.getMetadataValue(enumValue, "Web Presentation")
                if (webPresentation && webPresentation != "") {
                    enumValueMap["description"] = webPresentation
                }

                if (DDHelperFunctions.getMetadataValue(enumValue, "Status") == "retired") {
                    enumValueMap["retired"] = "true"
                }
                enumerationValues.add(enumValueMap)
            }
            result["nationalCodes"] = enumerationValues
        } else if (dataElement.dataType instanceof PrimitiveType && dataElement.dataType.label != "Default String") {
            result["type"] = ((PrimitiveType) dataElement.dataType).label
        }

        List<Map> dataElements = []

        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(NhsDataDictionary.CORE_MODEL_NAME)
        String uin = DDHelperFunctions.getMetadataValue(dataElement, "uin")
        coreModel.dataClasses.find {it.label == NhsDataDictionary.DATA_FIELD_NOTES_CLASS_NAME}.dataElements.each {relatedDataElement ->
            String elementAttributes = DDHelperFunctions.getMetadataValue(relatedDataElement, "element_attributes")
            if (elementAttributes && elementAttributes.contains(uin)) {
                Map relatedElement = [
                    catalogueId: relatedDataElement.id.toString(),
                    name       : relatedDataElement.label,
                    stereotype : "element",
                ]
                dataElements.add(relatedElement)
            }
        }

        result["dataElements"] = dataElements

        return result
    }

    @Override
    Set<DataElement> getAll(UUID versionedFolderId, boolean includeRetired = false) {

        DataModel coreModel = nhsDataDictionaryService.getCoreModel(versionedFolderId)
        DataClass attributesClass = coreModel.dataClasses.find {it.label == NhsDataDictionary.ATTRIBUTES_CLASS_NAME}

        List<UUID> classIds = [attributesClass.id]
        if(includeRetired) {
            DataClass retiredAttributesClass = attributesClass.dataClasses.find {it.label == "Retired"}
            classIds.add(retiredAttributesClass.id)
        }
        List<DataElement> attributes = DataElement.by().inList('dataClass.id', classIds).list()

        attributes.findAll {dataElement ->
            includeRetired || !catalogueItemIsRetired(dataElement)
        }

    }

    @Override
    DataElement getItem(UUID id) {
        dataElementService.get(id)
    }

    @Override
    String getMetadataNamespace() {
        NhsDataDictionary.METADATA_NAMESPACE + ".attribute"
    }

    @Override
    NhsDDAttribute getNhsDataDictionaryComponentFromCatalogueItem(DataElement catalogueItem, NhsDataDictionary dataDictionary) {
        NhsDDAttribute attribute = new NhsDDAttribute()
        nhsDataDictionaryComponentFromItem(catalogueItem, attribute)
        if (catalogueItem.dataType instanceof ModelDataType) {
            List<Term> terms = termService.findAllByTerminologyId(((ModelDataType) catalogueItem.dataType).modelResourceId)
            List<Metadata> allRelevantMetadata = Metadata
                .byMultiFacetAwareItemIdInList(terms.collect {it.id})
                .inList('key', ['publishDate', 'webOrder', 'webPresentation'])
                .list()
            attribute.codes = terms
                .collect {term ->
                    new NhsDDCode(attribute).tap {
                        code = term.code
                        definition = term.definition
                        publishDate = allRelevantMetadata.find {it.multiFacetAwareItemId == term.id && it.key == 'publishDate'}?.value
                        webOrder = allRelevantMetadata.find {it.multiFacetAwareItemId == term.id && it.key == 'webOrder'}?.value
                        webPresentation = allRelevantMetadata.find {it.multiFacetAwareItemId == term.id && it.key == 'webPresentation'}?.value
                    }
                }
        }
        return attribute

    }

    void persistAttributes(NhsDataDictionary dataDictionary,
                           VersionedFolder dictionaryFolder, DataModel coreDataModel, String currentUserEmailAddress,
                           Map<String, Terminology> attributeTerminologiesByName, Map<String, DataElement> attributeElementsByName) {

        Folder attributeTerminologiesFolder =
            new Folder(label: "Attribute Terminologies", createdBy: currentUserEmailAddress)
        dictionaryFolder.addToChildFolders(attributeTerminologiesFolder)

        if (!folderService.validate(attributeTerminologiesFolder)) {
            throw new ApiInvalidModelException('NHSDD', 'Invalid model', attributeTerminologiesFolder.errors)
        }
        folderService.save(attributeTerminologiesFolder)

        DataClass allAttributesClass = new DataClass(label: NhsDataDictionary.ATTRIBUTES_CLASS_NAME, createdBy: currentUserEmailAddress)

        DataClass retiredAttributesClass = new DataClass(label: "Retired", createdBy: currentUserEmailAddress,
                                                         parentDataClass: allAttributesClass)
        allAttributesClass.addToDataClasses(retiredAttributesClass)
        coreDataModel.addToDataClasses(allAttributesClass)
        coreDataModel.addToDataClasses(retiredAttributesClass)


        PrimitiveType stringDataType = coreDataModel.getPrimitiveTypes().find {it.label == "String"}
        if (!stringDataType) {
            stringDataType = new PrimitiveType(label: "String", createdBy: currentUserEmailAddress)
            coreDataModel.addToDataTypes(stringDataType)
        }

        Map<String, PrimitiveType> primitiveTypes = [:]
        Map<String, Folder> folders = [:]
        int idx = 0
        dataDictionary.attributes.each {name, attribute ->
            DataType dataType

            if (attribute.codes.size() > 0) {
                String folderName = attribute.name.substring(0, 1).toUpperCase()
                Folder subFolder = folders[folderName]
                if (!subFolder) {
                    subFolder = new Folder(label: folderName, createdBy: currentUserEmailAddress)
                    attributeTerminologiesFolder.addToChildFolders(subFolder)
                    if (!folderService.validate(subFolder)) {
                        throw new ApiInvalidModelException('NHSDD', 'Invalid model', subFolder.errors)
                    }
                    folderService.save(subFolder)
                    folders[folderName] = subFolder
                }
                // attributeTerminologiesFolder.save()

                Terminology terminology = new Terminology(
                    label: name,
                    folder: subFolder,
                    createdBy: currentUserEmailAddress,
                    authority: authorityService.defaultAuthority)
                terminology.addToMetadata(new Metadata(namespace: "uk.nhs.datadictionary.terminology", key: "version", value: attribute.codesVersion))

                attribute.codes.each {code ->
                    Term term = new Term(
                        code: code.code,
                        definition: code.definition,
                        createdBy: currentUserEmailAddress,
                        label: "${code.code} : ${code.definition}",
                        depth: 1,
                        terminology: terminology
                    )

                    code.propertiesAsMap().each {key, value ->
                        term.addToMetadata(new Metadata(namespace: "uk.nhs.datadictionary.term",
                                                        key: key,
                                                        value: value,
                                                        createdBy: currentUserEmailAddress))
                    }
                    terminology.addToTerms(term)

                }
                if (terminology.validate()) {
                    terminology = terminologyService.saveModelWithContent(terminology)
                    terminology.terms.size() // Required to reload the terms back into the session
                    attributeTerminologiesByName[name] = terminology
                } else {
                    GormUtils.outputDomainErrors(messageSource, terminology) // TODO throw exception???
                    log.error("Cannot save terminology: ${name}")
                }

                //nhsDataDictionary.attributeTerminologiesByName[name] = terminology
                dataType = new ModelDataType(label: "${name} Attribute Type",
                                             modelResourceDomainType: terminology.getDomainType(),
                                             modelResourceId: terminology.id,
                                             createdBy: currentUserEmailAddress)
                coreDataModel.addToDataTypes(dataType)
            } else {
                // no "code-system" nodes
                dataType = stringDataType
            }

            DataElement attributeDataElement = new DataElement(
                label: name,
                description: attribute.definition,
                createdBy: currentUserEmailAddress,
                dataType: dataType,
                index: idx++)

            addMetadataFromComponent(attributeDataElement, attribute, currentUserEmailAddress)

            if (attribute.isRetired()) {
                retiredAttributesClass.addToDataElements(attributeDataElement)
            } else {
                allAttributesClass.addToDataElements(attributeDataElement)
            }
            attributeElementsByName[name] = attributeDataElement
        }
    }

}
