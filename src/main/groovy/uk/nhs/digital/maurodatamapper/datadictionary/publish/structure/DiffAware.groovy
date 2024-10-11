package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

interface DiffAware<T, D> {
    D produceDiff(T previous)
}