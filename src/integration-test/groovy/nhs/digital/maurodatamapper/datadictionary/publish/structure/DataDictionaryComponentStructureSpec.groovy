package nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.DataDictionaryComponentService
import uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd.NhsDataDictionaryService

import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDChangeLog
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent
import uk.nhs.digital.maurodatamapper.datadictionary.publish.ItemLinkScanner
import uk.nhs.digital.maurodatamapper.datadictionary.publish.NhsDataDictionaryComponentPathResolver
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishTarget

abstract class DataDictionaryComponentStructureSpec<T extends NhsDataDictionaryComponent> extends Specification {
    NhsDataDictionary dataDictionary

    UUID branchId
    String definition
    NhsDataDictionaryComponentPathResolver componentPathResolver
    MockCatalogueItemPathResolver catalogueItemPathResolver

    T activeItem
    T retiredItem
    T preparatoryItem

    T previousItemDescriptionChange
    T previousItemAliasesChange
    T previousItemAllChange

    PublishContext websiteDitaPublishContext
    PublishContext websiteHtmlPublishContext
    PublishContext changePaperDitaPublishContext
    PublishContext changePaperHtmlPublishContext

    def setup() {
        dataDictionary = createDataDictionary()

        branchId = UUID.fromString("782602d4-e153-45d8-a271-eb42396804da")

        definition = getDefinition()
        setupRelatedItems()

        setupActiveItem()
        setupRetiredItem()
        setupPreparatoryItem()
        setupPreviousItems()

        if (retiredItem) {
            retiredItem.otherProperties["isRetired"] = true.toString()
        }

        if (preparatoryItem) {
            preparatoryItem.otherProperties["isPreparatory"] = true.toString()
        }

        setupComponentPathResolver()
        setupCatalogueItemPathResolver()
        setupPublishContexts()
    }

    protected abstract NhsDataDictionary createDataDictionary()

    protected static <K, V> Map<K, V> copyMap(Map<K, V> properties) {
        Map<K, V> copy = [:]
        properties.each { key, value -> copy[key] = value }
        copy
    }

    abstract String getDefinition()

    abstract void setupRelatedItems()

    protected void setupComponentPathResolver() {
        componentPathResolver = new NhsDataDictionaryComponentPathResolver()

        // Self-reference this item
        componentPathResolver.add(activeItem.getMauroPath(), activeItem)
    }

    protected void setupCatalogueItemPathResolver() {
        catalogueItemPathResolver = new MockCatalogueItemPathResolver()

        // Self-reference this item
        catalogueItemPathResolver.add(activeItem.getMauroPath(), activeItem.catalogueItemId)
    }

    private void setupPublishContexts() {
        ItemLinkScanner ditaItemLinkScanner = ItemLinkScanner.createForDitaOutput(componentPathResolver)
        ItemLinkScanner htmlItemLinkScanner = ItemLinkScanner.createForHtmlPreview(branchId, catalogueItemPathResolver)

        websiteDitaPublishContext = new PublishContext(PublishTarget.WEBSITE)
        websiteDitaPublishContext.setItemLinkScanner(ditaItemLinkScanner)

        websiteHtmlPublishContext = new PublishContext(PublishTarget.WEBSITE)
        websiteHtmlPublishContext.setItemLinkScanner(htmlItemLinkScanner)

        changePaperDitaPublishContext = new PublishContext(PublishTarget.CHANGE_PAPER)
        changePaperDitaPublishContext.setItemLinkScanner(ditaItemLinkScanner)

        changePaperHtmlPublishContext = new PublishContext(PublishTarget.CHANGE_PAPER)
        changePaperHtmlPublishContext.setItemLinkScanner(htmlItemLinkScanner)
    }

    abstract void setupActiveItem()

    abstract void setupRetiredItem()

    abstract void setupPreparatoryItem()

    abstract void setupPreviousItems()

    protected void addWhereUsed(T currentItem, NhsDataDictionaryComponent otherItem) {
        String description = "references in description $currentItem.name"
        currentItem.addWhereUsed(otherItem, description)
    }

    protected void addChangeLog(T currentItem, NhsDDChangeLog... entries) {
        currentItem.changeLogHeaderText = "<p>Click on the links below to view the change requests this item is part of:</p>"
        currentItem.changeLogFooterText = """<p>Click <a class="- topic/xref xref" href="https://www.datadictionary.nhs.uk/archive" target="_blank" rel="external noopener">here</a> to see the Change Log Information for changes before January 2025.</p>"""

        entries.each { currentItem.changeLog.add(it) }
    }
}
