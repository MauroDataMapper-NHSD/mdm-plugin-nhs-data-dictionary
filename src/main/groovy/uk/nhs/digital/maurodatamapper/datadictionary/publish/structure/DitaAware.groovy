package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.meta.DitaElement

interface DitaAware<T extends DitaElement> {
    T generateDita()
}