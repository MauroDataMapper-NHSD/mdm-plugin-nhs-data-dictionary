package uk.nhs.digital.maurodatamapper.datadictionary.publish.structure

import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.helpers.HtmlHelper

class Text implements ContentAware {
    final String value

    Text(String value) {
        this.value = value
    }

    @Override
    Div generateDita() {
        HtmlHelper.replaceHtmlWithDita(value)
    }
}
