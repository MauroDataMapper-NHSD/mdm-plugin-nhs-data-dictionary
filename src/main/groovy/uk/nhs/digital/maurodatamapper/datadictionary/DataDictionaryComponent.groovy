package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Body
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Html
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Metadata
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.OtherMeta
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Prolog
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STEntry
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STHead
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.STRow
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.SimpleTable
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.Topic

abstract class DataDictionaryComponent<C extends CatalogueItem> {

    List<String> stringFields = [
            "aliasAlsoKnownAs",
            "aliasFormerly",
            "aliasFullName",
            "aliasIndexName",
            "aliasODSXMLSchema",
            "aliasOid",
            "aliasPlural",
            "aliasPossessive",
            "aliasReportHeading",
            "aliasSchema",
            "aliasShortName",
            "aliasSnomedCTRefsetId",
            "aliasSnomedCTRefsetName",
            "base-uri",
            "baseVersion",
            "class",
            "clientRole",
            "doNotShowChangeLog",
            "error",
            "explanatoryPage",
            "extends",
            "format-length",
            "format-link",
            "isRetired",
            "isPrepatory",
            "link",
            "mod__abstract",
            "mod__final",
            "name",
            "navigationParent",
            "overview",
            "participant",
            "property",
            "readOnly",
            "referencedElement",
            "retiredDate",
            "type",
            "uin",
            "ultimate-path",
            "websitePageHeading",

            "TitleCaseName",
            "DD_URL",

            "item_order",
            "item_mro",
            "element_attributes"
    ]
    static Map<String, String> aliasFields = [
            "aliasAlsoKnownAs": "Also known as",
            "aliasFormerly": "Formerly",
            "aliasFullName": "Full name",
            "aliasIndexName": "Index name",
            "aliasODSXMLSchema": "ODS XML Schema",
            "aliasOid": "Object Identifier",
            "aliasPlural": "Plural",
            "aliasPossessive": "Possessive",
            "aliasReportHeading": "Report heading",
            "aliasSchema": "Schema",
            "aliasShortName": "Short name",
            "aliasSnomedCTRefsetId": "SNOMED CT Refset Id",
            "aliasSnomedCTRefsetName": "SNOMED CT Refset Name"]


    HashSet<DataDictionaryComponent> whereMentioned = []

    Map<String, String> stringValues = [:]

    String base_uri
    C catalogueItem
    def definition
    def overview

    UUID catalogueId
    UUID parentCatalogueId
    UUID containerCatalogueId

    String ditaTitle
    String ditaSearchTitle
    String uin

    String titleCaseName
    String ddUrl

    String name
    boolean isRetired = false
    boolean isPrepatory = false


    void fromXml (def xml) {
        stringFields.each { fieldName ->
            if(xml[fieldName]) {
                stringValues[fieldName] = xml[fieldName][0].text()
            }
        }
        setCoreAttributes()
        setDitaFields(xml)
    }

    void setCoreAttributes(){
        if(!name && stringValues["name"]) {
            name = stringValues["name"]
        }
        if(!uin && stringValues["uin"]) {
            uin = stringValues["uin"]
        }
        if(!base_uri && stringValues["base-uri"]) {
            base_uri = stringValues["base-uri"]
        }
        if(!titleCaseName && stringValues["TitleCaseName"]) {
            titleCaseName = stringValues["TitleCaseName"]
        }
        if(!ddUrl && stringValues["DD_URL"]) {
            ddUrl = stringValues["DD_URL"]
        }
        if(!overview && stringValues["overview"]) {
            overview = stringValues["overview"]
        }


        if(stringValues["isRetired"] == 'true') {
            isRetired = true
        }
        if(stringValues["isPrepatory"] == 'true') {
            isPrepatory = true
        }

        if(stringValues["ultimate-path"]) {
            stringValues["ultimate-path"] = stringValues["ultimate-path"].replace("http://www", "https://v3")
        }

    }

    void setDitaFields(def xml) {
        if(xml.dita.title) {
            ditaTitle = xml.dita.title.text()
        }
        if(xml.dita.titlealts.searchtitle) {
            ditaSearchTitle = xml.dita.titlealts.searchtitle.text()
        }
    }

    abstract C toCatalogueItem(DataDictionary dataDictionary)

    String getDataDictionaryUrl() {
        return stringValues

        //String id = base_uri.split("Data_Dictionary/")[1]
        //return "https://v3.datadictionary.nhs.uk/data_dictionary/"  + id.replaceAll("\\.txaClass20", "").toLowerCase()  + "_de.asp"
    }

    void fromCatalogueExport(DataDictionary dataDictionary, def catalogueXml, UUID parentId, UUID containerId, boolean isXml = false) {

        stringFields.each { fieldName ->
            String value = ""
            if(isXml) {
                value = DDHelperFunctions.findFromMetadataXml(catalogueXml, fieldName)
            } else {
                value = DDHelperFunctions.findFromMetadata(catalogueXml, fieldName)
            }
            if(value) {
                stringValues[fieldName] = value
            }
        }
        setCoreAttributes()
    }

    void fromCatalogueItem(DataDictionary dataDictionary, C catalogueItem, UUID parentId, UUID containerId, MetadataService metadataService) {
        List<uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata> metadata =
                metadataService.findAllByCatalogueItemId(catalogueItem.id)
        if(metadata.size() == 0) {
            metadata = catalogueItem.metadata as List
        }
        stringFields.each { fieldName ->
            uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata md = metadata.find {
                it.namespace.contains(DDHelperFunctions.metadataNamespace) &&
                        it.key == fieldName}
            if(md) {
                stringValues[fieldName] = md.value
            }
        }
        setCoreAttributes()
    }

    // abstract void updateDescriptionInCatalogue(BindingMauroDataMapperClient catalogueClient)

    abstract String getDitaKey()

    abstract String getOutputClassForLink()

    abstract String getOutputClassForTable()

    void addAllMetadata(MetadataAware element) {
        stringValues.each { stringValue ->
            DDHelperFunctions.addMetadata(element, stringValue.key, stringValue.value)
        }
        DDHelperFunctions.addMetadata(element, "ditaTitle", ditaTitle)
        DDHelperFunctions.addMetadata(element, "ditaSearchTitle", ditaSearchTitle)
    }

    SimpleTable generateAliasTable() {
        Map<String, String> aliasValues = [:]
        aliasFields.keySet().each { aliasType ->
            if(stringValues[aliasType]) {
                aliasValues[aliasFields[aliasType]] = stringValues[aliasType]
            }
        }
        if(aliasValues.size() > 0) {
            SimpleTable aliasTable = new SimpleTable(relColWidth: "1* 2*")
            aliasTable.stHead = new STHead(stEntries: [new STEntry(value: "Context"), new STEntry(value: "Alias")])
            aliasValues.each { entry ->
                aliasTable.stRows.add(new STRow(stEntries: [new STEntry(value: entry.key), new STEntry(value: entry.value)]))
            }
            return aliasTable
        } else {
            return null
        }


    }

    Topic generateWhereUsedTopic(DataDictionary dataDictionary) {
        Set<DataDictionaryComponent> whereMentionedNotRetired = whereMentioned.findAll{ !it.isRetired }

        SimpleTable whereUsedTable = new SimpleTable(relColWidth: "1* 3* 2*")
        whereUsedTable.stHead = new STHead(stEntries: [new STEntry(value: "Type"), new STEntry(value: "Link"), new STEntry(value: "How used")])

        if(this instanceof DDElement) {
            dataDictionary.dataSets.values().each { dataSet ->
                if(dataSet.catalogueItem.getAllDataElements().find { elem -> elem.label == this.name}) {
                    whereUsedTable.stRows.add(new STRow(stEntries: [new STEntry(value: dataSet.outputClassForTable),
                                                                    new STEntry(xref: dataSet.getXRef()),
                                                                    new STEntry(value: "references in description ${DDHelperFunctions.tidyLabel(name)}")
                    ]))
                }

            }
        }

        if(this instanceof DDAttribute) {
            dataDictionary.classes.values().each {clazz ->
                if(clazz.attributes.keySet().find{att -> att.name == this.name }) {
                    whereUsedTable.stRows.add(new STRow(stEntries: [new STEntry(value: clazz.outputClassForTable),
                                                                    new STEntry(xref: clazz.getXRef()),
                                                                    new STEntry(value: "has an attribute ${name} of type ${name}")
                    ]))
                }

            }

            dataDictionary.elements.values().each {element ->
                if(element.stringValues["element_attributes"] && element.stringValues["element_attributes"].contains(this.uin)) {
                    whereUsedTable.stRows.add(new STRow(stEntries: [new STEntry(value: element.outputClassForTable),
                                                                    new STEntry(xref: element.getXRef()),
                                                                    new STEntry(value: "is the data element of ${name}")
                    ]))
                }

            }
        }

        if(whereMentionedNotRetired.size() > 0) {

            whereMentionedNotRetired.sort{it.name}.each { ddc ->
                whereUsedTable.stRows.add(new STRow(stEntries: [new STEntry(value: ddc.outputClassForTable),
                                                                new STEntry(xref: ddc.getXRef()),
                                                                new STEntry(value: "references in description ${DDHelperFunctions.tidyLabel(name)}")
                ]))
            }

        }
        if(whereUsedTable.stRows.size() > 0) {
            Topic whereUsedTopic = createSubTopic("Where Used")
            whereUsedTopic.body.bodyElements.add(whereUsedTable)

            return whereUsedTopic
        }

        else return null
    }

    Topic getAliasTopic() {
        SimpleTable aliasTable = generateAliasTable()
        if (aliasTable) {
            Topic aliasTopic = createSubTopic("Also Known As")
            aliasTopic.body.bodyElements.add(new Html(content: "<p>This ${getTypeText()} " +
                                                                                                                         "is also known by these names: </p>"))
            aliasTopic.body.bodyElements.add(aliasTable)
            return aliasTopic
        }
        return null
    }

    abstract String getTypeText()
    abstract String getTypeTitle()

    uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.XRef getXRef() {
        uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.XRef xref = new uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.XRef
            (outputclass: getOutputClassForLink(),
                                                                                                                                                 keyref: getDitaKey())
        return xref
    }

    String getTopicTitle() {
        String title = DDHelperFunctions.tidyLabel(name)
        if (stringValues["isRetired"] == 'true') {
            title += " (Retired)"
        }
        return title
    }

    Prolog getProlog(String otherMetaName) {
        Prolog prolog = new Prolog(metadata: new Metadata())
        prolog.metadata.otherMeta.add(new OtherMeta(name: "name", content: otherMetaName))
        //prolog.metadata.otherMeta.add(new OtherMeta(name: "metaclass", content: metaClass))
        if(stringValues["baseVersion"] && stringValues["baseVersion"]!= "") {
            prolog.metadata.otherMeta.add(new OtherMeta(name: "Data_Dictionary_Profile_x__y_DD_Model_x__y_baseVersion", content:
                    stringValues["baseVersion"]))
        }
        prolog.metadata.otherMeta.add(new OtherMeta(name: "stereotype", content: getTypeTitle()))
        //prolog.metadata.otherMeta.add(new OtherMeta(name: "sourceURI", content: base_uri))
        prolog.metadata.otherMeta.add(new OtherMeta(name: "sourceVersion", content: new
            Date().toString()))
        prolog.metadata.otherMeta.add(new OtherMeta(name: "sourceType", content: "DerivedDD_html"))
        //prolog.metadata.otherMeta.add(new OtherMeta(name: "topicCreationDate", content: "??"))
        //prolog.metadata.otherMeta.add(new OtherMeta(name: "topicVersion", content: "??"))
        prolog.metadata.otherMeta.add(new OtherMeta(name: "originalCase", content: name))
        prolog.metadata.otherMeta.add(new OtherMeta(name: "retired", content: isRetired))

        prolog.metadata.keywords = name.split("\\W+")

        return prolog

    }

    Topic getParentTopic(DataDictionary dataDictionary) {
        Topic topic = new Topic
            (id: getDitaKey(),
                                                                                                                                                    title: getTopicTitle(), titleClass: getOutputClassForLink())
        topic.prolog = getProlog(getTopicTitle())
        topic.shortDesc = getShortDesc(dataDictionary)
        return topic
    }

    Topic createSubTopic(String pageTitle) {
        String ditaPageTitle = DDHelperFunctions.makeValidDitaName(pageTitle)
        Topic subTopic = new Topic(id: "${getDitaKey()}.${ditaPageTitle}", title: pageTitle, searchTitle: "${getTypeTitle()}: ${getTopicTitle()} - ${pageTitle}")
        // For now, we'll take the prolog off all the sub pages
        //subTopic.prolog = getProlog(pageTitle)
        subTopic.body = new Body()
        return subTopic
    }

    List<String> getPath() {
        List<String> path = []
        if(isRetired) {
            path.add("Retired")
        }
        return path
    }

    abstract List<Topic> toDitaTopics(DataDictionary dataDictionary)

    String getShortDesc(DataDictionary dataDictionary) {
        if(isPrepatory) {
            return "This item is being used for development purposes and has not yet been approved."
        } else {
            return getTopicTitle()
        }
    }

    Topic createDescriptionTopic() {
        Topic descriptionTopic = createSubTopic("Description")
        if(isPrepatory) {
            descriptionTopic.body.bodyElements.add(new Html(content: "<div><p><b>This item is being used for development purposes and has not yet been approved.</b></p></div>"))
        } else {
            descriptionTopic.body.bodyElements.add(new Html(content: "<div><p>" + DDHelperFunctions.parseHtml(definition) + "</p></div>"))
        }
        return descriptionTopic
    }

    boolean hasNoAliases() {
        List<String> aliasFields = stringFields.findAll{it.startsWith("alias")}

        return aliasFields.every{ aliasField ->
            stringValues[aliasField] == "" || stringValues[aliasField] == null
        }

    }

}