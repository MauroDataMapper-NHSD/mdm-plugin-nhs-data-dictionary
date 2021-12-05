package uk.nhs.digital.maurodatamapper.datadictionary

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

import groovy.util.logging.Slf4j

@Slf4j
class ClassLink {

    String uin
    String metaclass
    String clientRole
    String supplierRole
    String clientCardinality
    String supplierCardinality
    String name
    String partOfClientKey
    String partOfSupplierKey
    String supplierUin
    String clientUin
    String relationSupplierExclusivity
    String relationClientExclusivity
    String direction

    DataClass clientClass
    DataClass supplierClass



    ClassLink(def link) {
        uin = link."@uin".text()
        metaclass = link.metaclass.text()
        clientRole = link.clientRole.text()
        supplierRole = link.supplierRole.text()
        clientCardinality = link.clientCardinality.text()
        supplierCardinality = link.supplierCardinality.text()
        name = link.name.text()
        partOfClientKey = link.partOfClientKey.text()
        partOfSupplierKey = link.partOfSupplierKey.text()
        relationSupplierExclusivity = link.relationSupplierExclusivity.text()
        relationClientExclusivity = link.relationClientExclusivity.text()
        supplierUin = link.participant.find {it."@role".text() == "Supplier"}."@referencedUin".text()
        clientUin = link.participant.find {it."@role".text() == "Client"}."@referencedUin".text()
    }

    ClassLink(DataElement dataElement) {
        uin = DDHelperFunctions.getMetadataValue(dataElement, "uin")
        metaclass = DDHelperFunctions.getMetadataValue(dataElement, "metaclass")
        clientRole = DDHelperFunctions.getMetadataValue(dataElement, "clientRole")
        supplierRole = DDHelperFunctions.getMetadataValue(dataElement, "supplierRole")
        clientCardinality = DDHelperFunctions.getMetadataValue(dataElement, "clientCardinality")
        supplierCardinality = DDHelperFunctions.getMetadataValue(dataElement, "supplierCardinality")
        name = DDHelperFunctions.getMetadataValue(dataElement, "name")
        partOfClientKey = DDHelperFunctions.getMetadataValue(dataElement, "partOfClientKey")
        partOfSupplierKey = DDHelperFunctions.getMetadataValue(dataElement, "partOfSupplierKey")
        supplierUin = DDHelperFunctions.getMetadataValue(dataElement, "supplierUin")
        clientUin = DDHelperFunctions.getMetadataValue(dataElement, "clientUin")
        relationSupplierExclusivity = DDHelperFunctions.getMetadataValue(dataElement, "relationSupplierExclusivity")
        relationClientExclusivity = DDHelperFunctions.getMetadataValue(dataElement, "relationClientExclusivity")
        direction = DDHelperFunctions.getMetadataValue(dataElement, "direction")
//        this.dataElement = dataElement
    }

    String toTableText() {
        String cardinality = clientCardinality
        if(direction == "client") {
            cardinality = supplierCardinality
        }
        String role = clientRole
        if(direction == "client") {
            role = supplierRole
        }
        return getCardinalityText(cardinality, role)

    }

    void addMetadata(DataElement dataElement) {
        DDHelperFunctions.addMetadata(dataElement, "uin", uin)
        DDHelperFunctions.addMetadata(dataElement, "metaclass", metaclass)
        DDHelperFunctions.addMetadata(dataElement, "clientRole", clientRole)
        DDHelperFunctions.addMetadata(dataElement, "supplierRole", supplierRole)
        DDHelperFunctions.addMetadata(dataElement, "clientCardinality", clientCardinality)
        DDHelperFunctions.addMetadata(dataElement, "supplierCardinality", supplierCardinality)
        DDHelperFunctions.addMetadata(dataElement, "name", name)
        DDHelperFunctions.addMetadata(dataElement, "partOfClientKey", partOfClientKey)
        DDHelperFunctions.addMetadata(dataElement, "partOfSupplierKey", partOfSupplierKey)
        DDHelperFunctions.addMetadata(dataElement, "supplierUin", supplierUin)
        DDHelperFunctions.addMetadata(dataElement, "clientUin", clientUin)
        DDHelperFunctions.addMetadata(dataElement, "relationSupplierExclusivity", relationSupplierExclusivity)
        DDHelperFunctions.addMetadata(dataElement, "relationClientExclusivity", relationClientExclusivity)
        DDHelperFunctions.addMetadata(dataElement, "direction", direction)


    }

    static String getCardinalityText(String cardinality, String role) {
        switch(cardinality) {
            case "0..1":
                return "may be ${role} one and only one "
            case "0..*":
                return "may be ${role} one or more "
            case "1..*":
                return "must be ${role} one or more "
            case "1":
                return "must be ${role} one and only one "
            default: // we'll assume 1..1
                return "must be ${role} one and only one "
        }
    }

    void findTargetClass(NhsDataDictionary nhsDataDictionary) {
        supplierClass = nhsDataDictionary.classesByUin[supplierUin]
        if(!supplierClass) {
            log.error("Cannot match supplier class relationship: ${uin}")
            log.error("SupplierUin: ${supplierUin}")
            log.error(this.toString())
        }
        clientClass = nhsDataDictionary.classesByUin[clientUin]
        if(!clientClass) {
            log.error("Cannot match client class relationship: ${uin}")
            log.error("ClientUin: ${clientUin}")
            log.error(this.toString())
        }

    }

}
