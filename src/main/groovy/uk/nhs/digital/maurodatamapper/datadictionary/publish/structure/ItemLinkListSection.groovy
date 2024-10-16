package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Body
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishHelper
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishTarget
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.ChangeFunctions

class ItemLinkListSection extends Section {
    static final String ATTRIBUTES_TYPE = "attributes"
    static final String ELEMENTS_TYPE = "elements"

    final List<ItemLink> itemLinks

    ItemLinkListSection(DictionaryItem parent, String type, String title, List<ItemLink> itemLinks) {
        super(parent, type, title)

        this.itemLinks = itemLinks
    }

    static ItemLinkListSection createElementsListSection(DictionaryItem parent, List<ItemLink> itemLinks) {
        new ItemLinkListSection(parent, ELEMENTS_TYPE, "Data Elements", itemLinks)
    }

    static ItemLinkListSection createAttributesListSection(DictionaryItem parent, List<ItemLink> itemLinks) {
        String title = itemLinks.size() > 1 ? "Attributes" : "Attribute"
        new ItemLinkListSection(parent, ATTRIBUTES_TYPE, title, itemLinks)
    }

    @Override
    Section produceDiff(Section previous) {
        ItemLinkListSection previousSection = previous as ItemLinkListSection

        List<ItemLink> currentLinks = this.itemLinks
        List<ItemLink> previousLinks = previousSection?.itemLinks ?: []

        if (currentLinks.empty && previousLinks.empty) {
            return null
        }

        if (ChangeFunctions.areEqual(currentLinks, previousLinks)) {
            return null
        }

        List<ItemLink> newLinks = ChangeFunctions.getDifferences(currentLinks, previousLinks)
        List<ItemLink> removedLinks = ChangeFunctions.getDifferences(previousLinks, currentLinks)

        if (newLinks.empty && removedLinks.empty) {
            return null
        }

        List<ItemLink> diffLinks = []
        currentLinks.each { row ->
            if (newLinks.any { it.discriminator == row.discriminator }) {
                diffLinks.add(row.cloneWithDiff(DiffStatus.NEW))
            }
            else {
                diffLinks.add(row)
            }
        }
        removedLinks.each { row ->
            diffLinks.add(row.cloneWithDiff(DiffStatus.REMOVED))
        }

        String diffTitle = this.type == ELEMENTS_TYPE ? Change.CHANGED_ELEMENTS_TYPE : Change.CHANGED_ATTRIBUTES_TYPE
        new ItemLinkListSection(parent, type, diffTitle, diffLinks)
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String paragraphOutputClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_PARAGRAPH : ""
        String listCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_UNORDERED_LIST : null
        String itemCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_LIST_ITEM : null

        builder.div {
            if (context.target == PublishTarget.CHANGE_PAPER) {
                builder.p(class: paragraphOutputClass) {
                    b title
                }
            }
            builder.ul(class: listCssClass) {
                itemLinks.each {itemLink ->
                    String diffOutputClass = PublishHelper.getDiffCssClass(itemLink.diffStatus)

                    String itemOutputClass = context.target == PublishTarget.WEBSITE
                        ? itemCssClass
                        : !diffOutputClass.empty
                        ? diffOutputClass
                        : null

                    builder.li(class: itemOutputClass) {
                        itemLink.buildHtml(context, builder)
                    }
                }
            }
        }
    }

    @Override
    protected Body generateBodyDita(PublishContext context) {
        Body.build() {
            ul {
                itemLinks.each { itemLink ->
                    String outputClass = PublishHelper.getDiffCssClass(itemLink.diffStatus)

                    li(outputClass: outputClass) {
                        xRef itemLink.generateDita(context)
                    }
                }
            }
        }
    }

    @Override
    protected Div generateDivDita(PublishContext context) {
        Div.build() {
            ul {
                itemLinks.each { itemLink ->
                    String outputClass = PublishHelper.getDiffCssClass(itemLink.diffStatus)

                    li(outputClass: outputClass) {
                        xRef itemLink.generateDita(context)
                    }
                }
            }
        }
    }
}