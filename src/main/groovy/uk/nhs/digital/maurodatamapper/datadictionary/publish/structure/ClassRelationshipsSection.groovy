package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Body
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Strow

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishHelper
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishTarget
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change

class ClassRelationshipSection extends Section {
    final ClassRelationshipTable table

    ClassRelationshipSection(DictionaryItem parent, ClassRelationshipTable table) {
        this(parent, "Relationships", table)
    }

    ClassRelationshipSection(DictionaryItem parent, List<ClassRelationshipRow> rows) {
        this(parent, new ClassRelationshipTable(rows))
    }

    ClassRelationshipSection(DictionaryItem parent, String title, ClassRelationshipTable table) {
        super(parent, "relationships", title)

        this.table = table
    }

    @Override
    Section produceDiff(Section previous) {
        ClassRelationshipSection previousSection = previous as ClassRelationshipSection
        ClassRelationshipTable diffTable = this.table.produceDiff(previousSection?.table) as ClassRelationshipTable
        if (!diffTable) {
            return null
        }

        new ClassRelationshipSection(this.parent, Change.CHANGED_RELATIONSHIPS_TYPE, diffTable)
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

class ClassRelationshipTable extends StandardTable<ClassRelationshipRow> {
    static final List<StandardColumn> COLUMNS = [
        new StandardColumn("Key", "1*", "10%"),
        new StandardColumn("Relationship", "5*", "45%"),
        new StandardColumn("Class", "5*", "45%"),
    ]

    ClassRelationshipTable(List<ClassRelationshipRow> rows) {
        super(COLUMNS, rows)
    }

    @Override
    protected StandardTable cloneWithRows(List<ClassRelationshipRow> rows) {
        new ClassRelationshipTable(rows)
    }
}

class ClassRelationshipRow extends StandardRow {
    final boolean isKey
    final String description
    final ItemLink classLink

    ClassRelationshipRow(boolean isKey, String description, ItemLink classLink) {
        this(isKey, description, classLink, DiffStatus.NONE)
    }

    ClassRelationshipRow(boolean isKey, String description, ItemLink classLink, DiffStatus diffStatus) {
        super(diffStatus)

        this.isKey = isKey
        this.description = description
        this.classLink = classLink
    }

    @Override
    String getDiscriminator() {
        "${isKey ? "Key: " : ""}${description} ${classLink.name}"
    }

    @Override
    Strow generateDita(PublishContext context) {
        String outputClass = PublishHelper.getDiffCssClass(diffStatus)

        Strow.build() {
            stentry(outputClass: outputClass) {
                txt isKey ? 'Key' : ''
            }
            stentry(outputClass: outputClass) {
                txt description
            }
            stentry(outputClass: outputClass) {
                xRef classLink.generateDita(context)
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
                mkp.yield(description)
            }
            builder.td(class: entryCssClass) {
                classLink.buildHtml(context, builder)
            }
        }
    }

    @Override
    protected StandardRow cloneWithDiff(DiffStatus diffStatus) {
        new ClassRelationshipRow(this.isKey, this.description, this.classLink, diffStatus)
    }
}
