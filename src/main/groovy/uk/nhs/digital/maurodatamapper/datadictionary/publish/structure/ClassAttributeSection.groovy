package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Body
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Strow

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishHelper
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishTarget
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change

class ClassAttributeSection extends Section {
    final ClassAttributeTable table

    ClassAttributeSection(DictionaryItem parent, ClassAttributeTable table) {
        this(parent, "Attributes", table)
    }

    ClassAttributeSection(DictionaryItem parent, List<ClassAttributeRow> rows) {
        this(parent, new ClassAttributeTable(rows))
    }

    ClassAttributeSection(DictionaryItem parent, String title, ClassAttributeTable table) {
        super(parent, "attributes", title)

        this.table = table
    }

    @Override
    Section produceDiff(Section previous) {
        ClassAttributeSection previousSection = previous as ClassAttributeSection
        ClassAttributeTable diffTable = this.table.produceDiff(previousSection?.table) as ClassAttributeTable
        if (!diffTable) {
            return null
        }

        new ClassAttributeSection(this.parent, Change.CHANGED_ATTRIBUTES_TYPE, diffTable)
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {

    }

    @Override
    protected Body generateBodyDita(PublishContext context) {
        return null
    }

    @Override
    protected Div generateDivDita(PublishContext context) {
        return null
    }
}

class ClassAttributeTable extends StandardTable<ClassAttributeRow> {
    static final List<StandardColumn> COLUMNS = [
        new StandardColumn("Key", "1*", "5%"),
        new StandardColumn("Attribute Name", "9*", "95%"),
    ]

    ClassAttributeTable(List<ClassAttributeRow> rows) {
        super(COLUMNS, rows)
    }

    @Override
    protected StandardTable cloneWithRows(List<ClassAttributeRow> rows) {
        new ClassAttributeTable(rows)
    }
}

class ClassAttributeRow extends StandardRow {
    final boolean isKey
    final ItemLink attributeLink

    ClassAttributeRow(boolean isKey, ItemLink attributeLink) {
        this(isKey, attributeLink, DiffStatus.NONE)
    }

    ClassAttributeRow(boolean isKey, ItemLink attributeLink, DiffStatus diffStatus) {
        super(diffStatus)

        this.isKey = isKey
        this.attributeLink = attributeLink
    }

    @Override
    String getDiscriminator() {
        "${isKey ? 'key:' : ''}${attributeLink.name}"
    }

    @Override
    Strow generateDita(PublishContext context) {
        String outputClass = PublishHelper.getDiffCssClass(diffStatus)

        Strow.build() {
            stentry(outputClass: outputClass) {
                txt isKey ? 'Key' : ''
            }
            stentry(outputClass: outputClass) {
                xRef attributeLink.generateDita(context)
            }
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String diffOutputClass = PublishHelper.getDiffCssClass(diffStatus)

        String rowCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE_ROW : null
        String entryCssClass = context.target == PublishTarget.WEBSITE
            ? HtmlConstants.CSS_TABLE_ENTRY
            : !diffOutputClass.empty
                                   ? diffOutputClass
                                   : null

        builder.tr(class: rowCssClass) {
            builder.td(class: entryCssClass) {
                mkp.yield(isKey ? 'Key' : '')
            }
            builder.td(class: entryCssClass) {
                attributeLink.buildHtml(context, builder)
            }
        }
    }

    @Override
    protected StandardRow cloneWithDiff(DiffStatus diffStatus) {
        new ClassAttributeRow(this.isKey, this.attributeLink, diffStatus)
    }
}