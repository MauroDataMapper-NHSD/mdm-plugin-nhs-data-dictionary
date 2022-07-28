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

        StringWriter finalResult = new StringWriter();
        //SAXTransformerFactory tf = new org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl()
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        System.err.println(tf.class)
        TransformerHandler result = tf.newTransformerHandler();
        result.getTransformer().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        result.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
        result.getTransformer().setOutputProperty(OutputKeys.METHOD, "html");
        //result.getTransformer().setOutputProperty(OutputKeys.ENCODING, TestHelper.ENCODING);
        result.setResult(new StreamResult(finalResult));

        ContentHandler postProcess = result;

        Locale locale = Locale.getDefault();
        String prefix = "diff";

        NekoHtmlParser cleaner = new NekoHtmlParser();

        InputSource oldSource = new InputSource(new StringReader(
                first));
        InputSource newSource = new InputSource(new StringReader(
                second));

        DomTreeBuilder oldHandler = new DomTreeBuilder();
        cleaner.parse(oldSource, oldHandler);
        TextNodeComparator leftComparator = new TextNodeComparator(
                oldHandler, locale);

        DomTreeBuilder newHandler = new DomTreeBuilder();
        cleaner.parse(newSource, newHandler);
        TextNodeComparator rightComparator = new TextNodeComparator(
                newHandler, locale);

        HtmlSaxDiffOutput output = new HtmlSaxDiffOutput(postProcess,
                prefix);

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

        HTMLDiffer differ = new HTMLDiffer(output);
        differ.diff(leftComparator, rightComparator);

        return finalResult.toString();

    }

}
