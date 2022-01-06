package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd


class NhsDataDictionaryInterceptor {

    // TODO: Need to be an administrator before most methods can be called.

    boolean before() { true }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
