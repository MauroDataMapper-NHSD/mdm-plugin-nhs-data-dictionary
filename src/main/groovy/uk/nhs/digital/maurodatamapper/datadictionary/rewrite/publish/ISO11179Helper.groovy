package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish

import uk.ac.ox.softeng.maurodatamapper.iso11179.domain.DataElement
import uk.ac.ox.softeng.maurodatamapper.iso11179.domain.MessageConformanceLevel
import uk.ac.ox.softeng.maurodatamapper.iso11179.domain.MessageStandard
import uk.ac.ox.softeng.maurodatamapper.iso11179.domain.MetadataBundle
import uk.ac.ox.softeng.maurodatamapper.iso11179.domain.MetadataItem
import uk.ac.ox.softeng.maurodatamapper.iso11179.domain.RegistrySpecification
import uk.ac.ox.softeng.maurodatamapper.iso11179.domain.ScopedIdentifier
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary

import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar
import java.time.LocalDateTime

class ISO11179Helper {

    static MetadataBundle generateMetadataBundle(NhsDataDictionary nhsDataDictionary) {
        MetadataBundle metadataBundle = new MetadataBundle().tap {
            sourceRegistry = generateRegistrySpecification(nhsDataDictionary)

            messageStandard.add(MessageStandard.ISO_IEC_19583_26)
            messageConformanceLevel = MessageConformanceLevel.BASIC
            final GregorianCalendar now = new GregorianCalendar()
            exportedOn = DatatypeFactory.newInstance().newXMLGregorianCalendar(now)

            content = generateContent(nhsDataDictionary)
        }

    }

    static RegistrySpecification generateRegistrySpecification(NhsDataDictionary nhsDataDictionary) {
        return new RegistrySpecification().tap {
            name = "NHS Data Dictionary"
            webAddress = "https://www.datadictionary.nhs.uk"
            standard = "ISO/IEC 19583-26:2023(Basic)"
            conformanceLevel = "Basic"
            characterRepertoire = "UTF-8"
            referenceDocumentIdentifierForm = "uri"
        }

    }

    static MetadataBundle.Content generateContent(NhsDataDictionary nhsDataDictionary) {
        return new MetadataBundle.Content().tap {content ->
            nhsDataDictionary.elements.values().sort {it.name }.each {nhsDDElement ->
                content.dataElement.add(
                        new DataElement().tap {isoDataElement ->
                            itemMetadata = new MetadataItem().tap {
                                identifier.add(new ScopedIdentifier().tap {
                                    scope = ""
                                    identifier = nhsDDElement.name
                                    version = nhsDataDictionary.folderName

                                })


                            }

                        }
                )
            }
        }
    }


}
