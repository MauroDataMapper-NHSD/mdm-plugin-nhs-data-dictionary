package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.ui

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.ui.providers.TabViewUIProviderService

class DataDictionaryStatisticsTabView extends TabViewUIProviderService {

    VersionedFolderService versionedFolderService

    @Override
    List<String> providedTabViews(String domainType, UUID itemId) {

        System.err.println(domainType)
        System.err.println(itemId)

        if(domainType == 'versionedFolders') {
            VersionedFolder versionedFolder = VersionedFolder.get(itemId)
            if(versionedFolder && versionedFolder.label.contains('NHS Data Dictionary')) {
                System.err.println(versionedFolder.label)
                return ['Statistics', 'Preview']
            }
            else {
                return []
            }
        }
        return []
    }

    @Override
    byte[] getJSWebComponent(String s, UUID uuid, String s1) {
        return new byte[0]
    }

    @Override
    String getDisplayName() {
        return "NHS Data Dictionary UI Tab Provider"
    }

    @Override
    String getVersion() {
        getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
    }
}