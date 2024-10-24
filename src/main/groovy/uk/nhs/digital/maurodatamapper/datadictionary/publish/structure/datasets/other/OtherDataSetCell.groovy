/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.datasets.other

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.P
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Scope

import groovy.xml.MarkupBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishContext
import uk.nhs.digital.maurodatamapper.datadictionary.publish.PublishHelper
import uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper.ChangeAware
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.DitaAware
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.ExternalLink
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.HtmlBuilder
import uk.nhs.digital.maurodatamapper.datadictionary.publish.structure.ItemLink

abstract class OtherDataSetCell implements DitaAware<List<P>>, HtmlBuilder, ChangeAware {
}

class OtherDataSetItemLinkCell extends OtherDataSetCell {
    static final String MULTIPLE_OCCURRENCES_TEXT = "Multiple occurrences of this item are permitted"

    final String name
    final ItemLink itemLink
    final boolean allowMultiple

    OtherDataSetItemLinkCell(String name, ItemLink itemLink) {
        this(name, itemLink, false)
    }

    OtherDataSetItemLinkCell(String name, ItemLink itemLink, boolean allowMultiple) {
        this.name = name
        this.itemLink = itemLink
        this.allowMultiple = allowMultiple
    }

    @Override
    String getDiscriminator() {
        "${name}-multiple:${allowMultiple}"
    }

    @Override
    List<P> generateDita(PublishContext context) {
        List<P> paragraphs = []

        if (itemLink) {
            paragraphs.add(P.build() {
                xRef itemLink.generateDita(context)
            })
        }
        else {
            paragraphs.add(P.build() {
                txt this.name
            })
        }

        if (allowMultiple) {
            paragraphs.add(P.build() {
                txt MULTIPLE_OCCURRENCES_TEXT
            })
        }

        paragraphs
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        if (itemLink) {
            builder.p(class: context.paragraphCssClass) {
                itemLink.buildHtml(context, builder)
            }
        }
        else {
            PublishHelper.buildHtmlParagraph(context, builder, this.name)
        }

        if (allowMultiple) {
            PublishHelper.buildHtmlParagraph(context, builder, MULTIPLE_OCCURRENCES_TEXT)
        }
    }
}

class OtherDataSetChoiceCell extends OtherDataSetCell {
    static final String AND_OPERATOR = "And"
    static final String OR_OPERATOR = "Or"
    static final String AND_OR_OPERATOR = "And/Or"

    final String operator
    final List<OtherDataSetItemLinkCell> cells

    OtherDataSetChoiceCell(String operator, List<OtherDataSetItemLinkCell> cells) {
        this.operator = operator
        this.cells = cells
    }

    @Override
    String getDiscriminator() {
        cells
            .collect { cell -> cell.discriminator }
            .join("_${operator}_")
    }

    @Override
    List<P> generateDita(PublishContext context) {
        List<P> paragraphs = []

        cells.eachWithIndex { OtherDataSetItemLinkCell cell, int index ->
            if (index != 0) {
                paragraphs.add(P.build() {
                    txt this.operator
                })
            }

            paragraphs.addAll(cell.generateDita(context))
        }

        paragraphs
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        cells.eachWithIndex { OtherDataSetItemLinkCell cell, int index ->
            if (index != 0) {
                PublishHelper.buildHtmlParagraph(context, builder, this.operator)
            }

            cell.buildHtml(context, builder)
        }
    }
}

class OtherDataSetAddressCell extends OtherDataSetCell {
    final ItemLink element
    final ExternalLink address1
    final ExternalLink address2

    OtherDataSetAddressCell(ItemLink element, ExternalLink address1, ExternalLink address2) {
        this.element = element
        this.address1 = address1
        this.address2 = address2
    }

    @Override
    String getDiscriminator() {
        "${element.discriminator}_with_address"
    }

    @Override
    List<P> generateDita(PublishContext context) {
        List<P> paragraphs = []

        paragraphs.add(P.build() {
            xRef element.generateDita(context)
            text " - "
            xRef address1.generateDita(context)
        })

        paragraphs.add(P.build() {
            txt "Or"
        })

        paragraphs.add(P.build() {
            xRef element.generateDita(context)
            text " - "
            xRef address2.generateDita(context)
        })

        paragraphs
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        builder.p(class: context.paragraphCssClass) {
            element.buildHtml(context, builder)
            mkp.yield(" - ")
            address1.buildHtml(context, builder)
        }

        PublishHelper.buildHtmlParagraph(context, builder, "Or")

        builder.p(class: context.paragraphCssClass) {
            element.buildHtml(context, builder)
            mkp.yield(" - ")
            address2.buildHtml(context, builder)
        }
    }
}
