package uk.ac.ox.softeng.maurodatamapper.plugins

import uk.ac.ox.softeng.maurodatamapper.core.logback.MdmConfigurator

import static ch.qos.logback.classic.Level.INFO

/**
 * @since 16/02/2022
 */
class NhsddConfigurator extends MdmConfigurator {

    @Override
    void configureLogLevels() {
        super.configureLogLevels()
        addInfo("Adding additional logger levels for NHSDD testing")
        setLoggerLevel 'uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService', INFO
        setLoggerLevel 'uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService', INFO
        setLoggerLevel 'uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService', INFO
        setLoggerLevel 'uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTreeService', INFO
        setLoggerLevel 'uk.ac.ox.softeng.maurodatamapper.core.model.ModelService', INFO
    }
}
