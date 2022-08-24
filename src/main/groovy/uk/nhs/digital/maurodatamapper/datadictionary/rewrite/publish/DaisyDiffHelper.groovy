/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish

import org.outerj.daisy.diff.helper.NekoHtmlParser
import org.outerj.daisy.diff.html.HTMLDiffer
import org.outerj.daisy.diff.html.HtmlSaxDiffOutput
import org.outerj.daisy.diff.html.TextNodeComparator
import org.outerj.daisy.diff.html.dom.DomTreeBuilder
import org.xml.sax.ContentHandler
import org.xml.sax.InputSource

import javax.xml.transform.OutputKeys
import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.sax.TransformerHandler
import javax.xml.transform.stream.StreamResult

class DaisyDiffHelper {

    static String diff(String first, String second) throws Exception {

        StringWriter finalResult = new StringWriter()
        //SAXTransformerFactory tf = new org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl()
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance()
        TransformerHandler result = tf.newTransformerHandler()
        result.getTransformer().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        result.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes")
        result.getTransformer().setOutputProperty(OutputKeys.METHOD, "html")
        //result.getTransformer().setOutputProperty(OutputKeys.ENCODING, TestHelper.ENCODING);
        result.setResult(new StreamResult(finalResult))

        ContentHandler postProcess = result

        Locale locale = Locale.getDefault()
        String prefix = "diff"

        NekoHtmlParser cleaner = new NekoHtmlParser()

        InputSource oldSource = new InputSource(new StringReader(
                first))
        InputSource newSource = new InputSource(new StringReader(
                second))

        DomTreeBuilder oldHandler = new DomTreeBuilder();
        cleaner.parse(oldSource, oldHandler)
        TextNodeComparator leftComparator = new TextNodeComparator(
                oldHandler, locale)

        DomTreeBuilder newHandler = new DomTreeBuilder();
        cleaner.parse(newSource, newHandler)
        TextNodeComparator rightComparator = new TextNodeComparator(
                newHandler, locale)

        HtmlSaxDiffOutput output = new HtmlSaxDiffOutput(postProcess,
                prefix)

        //Debug code
//        LCSSettings settings = new LCSSettings();
//        settings.setUseGreedyMethod(false);
//        // settings.setPowLimit(1.5);
//        // settings.setTooLong(100000*100000);
//
//        RangeDifference[] differences = RangeDifferencer.findDifferences(
//                settings, leftComparator, rightComparator);
//        LOG.info(">>>>Number of diffs is "+differences.length);
        //End of debug code

        HTMLDiffer differ = new HTMLDiffer(output)
        differ.diff(leftComparator, rightComparator)

        return finalResult.toString().replaceAll(" changes=\"[^\"]*\"", "")

    }

}
