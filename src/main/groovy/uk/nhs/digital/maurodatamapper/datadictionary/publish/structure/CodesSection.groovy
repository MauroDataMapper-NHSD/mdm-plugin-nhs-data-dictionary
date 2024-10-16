package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Body
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Strow
import uk.ac.ox.softeng.maurodatamapper.dita.helpers.HtmlHelper

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishHelper
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishTarget
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.Change

class CodesSection extends Section {
    static final String NATIONAL_CODES_TYPE = "nationalCodes"
    static final String DEFAULT_CODES_TYPE = "defaultCodes"

    final CodesTable table

    CodesSection(DictionaryItem parent, String type, String title, CodesTable table) {
        super(parent, type, title)

        this.table = table
    }

    CodesSection(DictionaryItem parent, String type, String title,  List<CodesRow> rows) {
        this(parent, type, title, new CodesTable(rows))
    }

    static CodesSection createNationalCodes(DictionaryItem parent, List<CodesRow> rows) {
        new CodesSection(parent, NATIONAL_CODES_TYPE, "National Codes", rows)
    }

    static CodesSection createDefaultCodes(DictionaryItem parent, List<CodesRow> rows) {
        new CodesSection(parent, DEFAULT_CODES_TYPE, "Default Codes", rows)
    }

    @Override
    Section produceDiff(Section previous) {
        CodesSection previousSection = previous as CodesSection
        CodesTable diffTable = this.table.produceDiff(previousSection?.table) as CodesTable
        if (!diffTable) {
            return null
        }

        String diffTitle = this.type == NATIONAL_CODES_TYPE ? Change.NATIONAL_CODES_TYPE : Change.DEFAULT_CODES_TYPE
        new CodesSection(this.parent, this.type, diffTitle, diffTable)
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        table.buildHtml(context, builder)
    }

    @Override
    protected Body generateBodyDita(PublishContext context) {
        Body.build() {
            simpletable table.generateDita(context)
        }
    }

    @Override
    protected Div generateDivDita(PublishContext context) {
        Div.build() {
            simpletable table.generateDita(context)
        }
    }
}

class CodesTable extends StandardTable<CodesRow> {
    static final List<StandardColumn> COLUMNS = [
        new StandardColumn("Code", "1*", "20%"),
        new StandardColumn("Description", "4*", "80%"),
    ]

    CodesTable(List<CodesRow> rows) {
        super(COLUMNS, rows)
    }

    @Override
    protected StandardTable<CodesRow> cloneWithRows(List<CodesRow> rows) {
        new CodesTable(rows)
    }
}

class CodesRow extends StandardRow {
    final String code
    final String description
    final boolean isHtml

    CodesRow(String code, String description, boolean isHtml) {
        this(code, description, isHtml, DiffStatus.NONE)
    }

    CodesRow(String code, String description, boolean isHtml, DiffStatus diffStatus) {
        super(diffStatus)

        this.code = code
        this.description = description
        this.isHtml = isHtml
    }

    @Override
    String getDiscriminator() {
        "${code}: ${description}"
    }

    @Override
    Strow generateDita(PublishContext context) {
        String outputClass = PublishHelper.getDiffCssClass(diffStatus)

        Strow.build() {
            stentry(outputClass: outputClass) {
                txt this.code
            }
            stentry(outputClass: outputClass) {
                if (isHtml) {
                    div HtmlHelper.replaceHtmlWithDita(description)
                }
                else {
                    txt description
                }
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
                mkp.yield(code)
            }
            builder.td(class: entryCssClass) {
                this.isHtml ? mkp.yieldUnescaped(description) : mkp.yield(description)
            }
        }
    }

    @Override
    protected StandardRow cloneWithDiff(DiffStatus diffStatus) {
        new CodesRow(this.code, this.description, this.isHtml, diffStatus)
    }
}