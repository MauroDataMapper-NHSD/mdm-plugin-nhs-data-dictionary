package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import uk.nhs.digital.maurodatamapper.datadictionary.DDHelperFunctions
import uk.nhs.digital.maurodatamapper.datadictionary.DataDictionaryComponent

trait NhsDataDictionaryComponent {

    abstract String getStereotype()
    abstract String getStereotypeForPreview()

    CatalogueItem catalogueItem
    String catalogueItemModelId
    String catalogueItemParentId

    String name
    String definition

    Map<String, String> otherProperties = [:]


    abstract String calculateShortDescription()


    boolean isRetired() {
        "true" == otherProperties["isRetired"]
    }

    boolean isPreparatory() {
        "true" == otherProperties["isPreparatory"]
    }

    String getUin() {
        otherProperties["uin"]
    }

    String getShortDescription() {
        otherProperties["shortDescription"]
    }

    String getTitleCaseName() {
        otherProperties["titleCaseName"]
    }


    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        if(xml.name.text()) {
            this.name = xml.name.text().replace("_", " ")
        } else { // This should only apply for XmlSchemaConstraints
            this.name = xml."class".name.text()
        }

        /*  We're doing capitalised items now
        if(xml.TitleCaseName.text()) {
            this.name = xml.TitleCaseName[0].text()
        } else { // This should only apply for XmlSchemaConstraints
            this.name = xml."class".websitePageHeading.text()
        }*/

        definition = (DDHelperFunctions.parseHtml(xml.definition)).replace("\u00a0", " ")
        definition = definition.replaceAll("\\s+", " ")

        NhsDataDictionary.METADATA_FIELD_MAPPING.entrySet().each {entry ->
            String xmlValue = xml[entry.value][0].text()
            if(!xmlValue || xmlValue == "") {
                xmlValue = xml."class"[entry.value].text()
            }
            if(xmlValue && xmlValue != "") {
                otherProperties[entry.key] = xmlValue
            }
        }
    }

    boolean isValidXmlNode(def xmlNode) {
        return true
    }

    abstract String getXmlNodeName()

    boolean hasNoAliases() {
        return getAliases().size() == 0
    }

    Map<String, String> getAliases() {
        Map<String, String> aliases = [:]
        Set<String> aliasFields = NhsDataDictionary.getAllMetadataKeys().findAll {it.startsWith("alias")}

        aliasFields.each {aliasField ->
            String aliasValue = otherProperties[aliasField]
            if(aliasValue) {
                aliases[aliasField] = aliasValue
            }
        }
        return aliases
    }



    Map<String, String> getUrlReplacements() {
        String ddUrl = this.otherProperties["ddUrl"]
        return [
            (ddUrl) : this.getMauroPath()
        ]
    }

    abstract String getMauroPath()


}