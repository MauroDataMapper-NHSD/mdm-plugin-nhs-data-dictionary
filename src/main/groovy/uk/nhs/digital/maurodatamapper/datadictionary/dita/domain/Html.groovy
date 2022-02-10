package uk.nhs.digital.maurodatamapper.datadictionary.dita.domain

import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import groovy.xml.slurpersupport.Node
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import org.w3c.tidy.Tidy
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.ColSpec
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.Entry
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.Row
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.TBody
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.TGroup
import uk.nhs.digital.maurodatamapper.datadictionary.dita.domain.calstable.Table

@Slf4j
class Html extends BodyElement{


    static Tidy tidy = new Tidy()


    static {
        Properties oProps = new Properties()
        //oProps.setProperty("new-empty-tags", "xref")
        oProps.setProperty("new-inline-tags", "xref, a, lq")
        // oProps.setProperty("new-pre-tags", "xref, a")
        // oProps.setProperty("vertical-space", "false")
        tidy.setDropFontTags(true)
        tidy.setConfigurationFromProps(oProps)
        tidy.setShowWarnings(false)
        tidy.setXmlTags(false)
        tidy.setInputEncoding("UTF-8")
        tidy.setOutputEncoding("UTF-8")
        tidy.setEncloseText(true)
        tidy.setEncloseBlockText(true)
        tidy.setXHTML(true)
        tidy.setMakeClean(true)
        tidy.setPrintBodyOnly(true)
        tidy.setWraplen(0)
        tidy.setQuiet(true)
        tidy.setNumEntities(true)
        tidy.setQuoteNbsp(false)

    }
    static XmlSlurper xmlSlurper = new XmlSlurper()

    static XmlParser xmlParser = XmlParser.newInstance()

    static HashMap<String, String> replacements = ["nbsp": "160",
                                        "rsquo": "8217",
                                        "lsquo": "8216",
                                        "rdquo": "8220",
                                        "ldquo": "8221",
                                        "reg:" : "174",
                                        "micro": "181",
                                        "sup2": "178",
                                        "ordm": "186",
                                        "ndash": "8211",
                                        "dagger": "8224",
                                        "deg": "176"
    ]

    String content

    @Override
    def writeAsXml(MarkupBuilder builder) {

        if(!content) {
            builder.mkp.yieldUnescaped("<p></p>")
        } else {
            try {
                String content2 = tidyAndClean(content)
                builder.mkp.yieldUnescaped(content2)

            } catch (Exception e) {
                log.error(content)
                e.printStackTrace()
                builder.mkp.yieldUnescaped("<p></p>")
            }

        }
    }


    static String tidyAndClean(String newContent) {

        String content2 = newContent

        // replace mal-formed tr/td tags
        //content2 = content2.replaceAll("<td([^/]*)/></td>", "<td></td>")
        content2 = content2.replaceAll("<td([^/>]*)/>", "<td\$1></td>")
        content2 = content2.replaceAll("(<a[^>]*)/\\s*>", "\$1></a>")

        replacements.each {replacement ->
            content2 = content2.replace("&" + replacement.key + ";", "&#" + replacement.value + ";")
        }

        //log.debug("content: " + content2)
        ByteArrayOutputStream baos = new ByteArrayOutputStream()

        try {
            tidy.parse(new ByteArrayInputStream(content2.getBytes()), baos)

        } catch(Exception e) {
            log.error("Couldn't tidy: " + content2.getBytes())
        }

        //log.debug(node)
        //tidy.pprint(node, System.err)
        //log.debug(baos.toString())

        //log.debug("content2 : " + baos.toString())
        GPathResult xml
        try {
            xml = xmlSlurper.parseText("<xml>" + baos.toString() + "</xml>")
        } catch(Exception e) {
            log.error("Couldn't tidy: " + baos.toString())
            xml = xmlSlurper.parseText("<xml></xml>")
        }

        // replace top-level br tags, as they won't do anything
        xml.br.each {br -> br.replaceNode {}}

        GPathResult newTopNode = xmlSlurper.parseText("<xml/>")

        xml.childNodes().each {childNode ->
            if (childNode.name() == "p") {
                def newPs = (splitList(childNode.children(), {ch ->
                    ch instanceof Node && ch.name() == "br"
                }))
                newPs.each {para ->
                    def newNode = xmlSlurper.parseText("<p/>")
                    para.each {
                        newNode.appendNode(it)
                    }
                    newTopNode.appendNode(newNode)
                }

            } else {
                newTopNode.appendNode(childNode)
            }

        }

        // xml = xmlSlurper.parseText(XmlUtil.serialize(newTopNode).replaceFirst("<\\?xml version=\"1.0\".*\\?>", ""))

        groovy.util.Node node = xmlParser.parseText(XmlUtil.serialize(newTopNode).replaceFirst("<\\?xml version=\"1.0\".*\\?>", ""))
        // further processing here...

        node.depthFirst().findAll{ n ->
            n instanceof groovy.util.Node && n.name() == "a" && !n.attributes()["href"]
        }.each {
            n -> removeNodeKeepChildren(n)
        }

        replaceNode(node, "a", "xref", ["scope": "external", format: "html"], ["target", "uin", "alias", "name", "title"])
        replaceNode(node, "em", "i")
        replaceNode(node, "ul", "ul", [:], ["class"])
        replaceNode(node, "h5", "p")
        replaceNode(node, "h4", "p")
        replaceNode(node, "h3", "p")
        replaceNode(node, "strong", "b")
        replaceNode(node, "blockquote", "lq")
//        replaceNode(node, "table", "simpletable", ["outputclass":"simpletable table table-striped table-sm"], ["border", "cellpadding", "width",
//                                                                                                      "cellspacing", "style", "class"])
//        replaceNode(node, "tr", "strow", [:], ["border", "cellpadding", "width", "cellspacing", "style", "valign", "align"])
//        replaceNode(node,"th", "stentry", ["outputclass":"thead-light"], ["border", "cellpadding", "width", "cellspacing", "style", "valign", "align", "rowspan", "colspan",
//                                                "class", "height", "bgcolor"])
//        replaceNode(node, "td", "stentry", [:], ["border", "cellpadding", "width", "cellspacing", "style", "valign", "align", "rowspan", "colspan",
//                                                 "class", "height", "bgcolor"])
        replaceNode(node, "li", "li", [:], ["style", "type","value"])
        replaceNode(node, "ol", "ol", [:], ["type"])
        //replaceNode(node, "span", "div", [:], ["style", "class"])
        replaceNode(node, "div", "div", [:], ["style"])
        replaceNode(node, "p", "p", [:], ["style"])
        replaceNode(node, "font", "ph", [:], ["color"])
        replaceNode(node, "xref", "xref", [:], ["class"])
        //replaceNode(node, "code", "pre", [:], ["class"])

        node.depthFirst().findAll() {
            n -> n instanceof  groovy.util.Node && (n.name() == "tbody" || n.name() == "thead")
        }.each {
            n -> removeNodeKeepChildren(n)
        }

        node.depthFirst().findAll {
            n -> n instanceof groovy.util.Node && (n.name() == "br" || n.name() == "hr" || n.name() == "img")
        }.each {n -> n.replaceNode {}}

        node.depthFirst().findAll {
            n -> n instanceof groovy.util.Node && n.name() == "li" && n.children().size() == 0
        }.each {n -> n.replaceNode {}}

        node.depthFirst().findAll {
            n -> n instanceof groovy.util.Node && n.name() == "code" && n.text() == "TermServer"
        }.each {n -> n.replaceNode {}}


        node.depthFirst().findAll{ n ->
            n instanceof groovy.util.Node && n.name() == "span"
        }.each {
            n -> removeNodeKeepChildren(n)
        }

        node = xmlParser.parseText(XmlUtil.serialize(node).replaceFirst("<\\?xml version=\"1.0\".*\\?>", ""))

        node.depthFirst().findAll{ n ->
            n instanceof groovy.util.Node && n.name() == "span"
        }.each {
            n -> removeNodeKeepChildren(n)
        }


        node.depthFirst().findAll { n ->
            n instanceof groovy.util.Node && n.name() == "table"
        }.each { n ->
            n.replaceNode (replaceTable(n))
        }


/*
        node.table.each { n ->
            n.replaceNode {
                "p"([], )
            }
            n.appendNode(n, "HTML Table temporarily redacted")
        }

        */

        node.depthFirst().findAll {
            n -> n instanceof groovy.util.Node && n.name() == "lq"
        }.each {
            groovy.util.Node n ->
                removeNodeKeepChildren(n)
        }

        node.'**'.findAll {
            n -> n instanceof groovy.util.Node && (n.name() == "a" && !n.attributes()["href"])
        } .each { n ->
            log.info("Found it!! " + n.toString())
            log.info(n.attributes())
                n.replaceNode {}
        }

        //log.debug(XmlUtil.serialize(xml))
        String ret = XmlUtil.serialize(node).replaceFirst("<\\?xml version=\"1.0\".*\\?>", "")
        ret = ret.replace("<xml>", "")
        ret = ret.replace("</xml>", "")
        ret = ret.replace("<xml/>", "")
        ret = ret.replaceAll("\n\n", "\n")
        ret = ret.replaceAll(">[\\r\\n\\s]+\\.", ">.")
        ret = ret.replaceAll(">[\\r\\n\\s]+,", ">,")
        ret = ret.replaceAll(">[\\r\\n\\s]+\\'", ">'")
        ret = ret.replaceAll("\\'[\\r\\n\\s]+<", "'<")
        ret = ret.replaceAll(">[\\r\\n\\s]+\\)", ">)")
        ret = ret.replaceAll("\\([\\r\\n\\s]+<", "(<")
        ret = ret.replaceAll(">[\\r\\n\\s]+:", ">:")
        ret = ret.replaceAll(">[\\r\\n\\s]+;", ">;")
        ret = ret.replaceAll(">[\\r\\n\\s]+/", ">/")
        ret = ret.replaceAll(">[\\r\\n\\s]+\"", ">\"")
        return ret

    }

    static <T> List<List<T>> splitList(List<T> list, Closure condition) {

        ArrayList<List<T>> ret = []
        int i=0
        list.each { it ->
            if(condition(it)) {
                if(ret.getAt(i)) {
                    i++
                }
            } else {
                if(ret.size() == i) {
                    List<T> newList = [it]
                    ret.add(newList)
                } else {
                    ret.getAt(i).add(it)
                }
            }
        }
        return ret
    }


    static void replaceNode(groovy.util.Node xml, String oldNodeName, String newNodeName, java.util.Map<String, String> newAttributes = [:],
                            List<String> removeAttributes = []) {
        xml.breadthFirst().findAll { n ->
            n instanceof groovy.util.Node && n.name() == oldNodeName
        }.each {n ->
            newAttributes.each { newAtt -> n.attributes()[newAtt.key] = newAtt.value }
            removeAttributes.each { oldAtt -> n.attributes().remove(oldAtt) }

            if(oldNodeName != newNodeName) {
                try{
                    n.replaceNode {
                        "${newNodeName}"(n.attributes(), n.children())
                    }
                } catch (Exception e) {
                    // log.debug("Cannot replace node")
                    // e.printStackTrace(System.err)
                    // log.debug(n)
                    replaceNode(n, new groovy.util.Node(null, "${newNodeName}", n.attributes(), n.children()))
                    // log.debug(n.parent())
                    // log.debug(n.parent().children().indexOf)
                    // log.debug(newNodeName)
                }
                //log.debug(XmlUtil.serialize(n))
            }
        }


    }

    static void removeNodeKeepChildren(groovy.util.Node node) {
        groovy.util.Node parent = node.parent()
        if(!parent) {
            log.error("null parent!")
            log.error(node.name().toString())
            log.error(node.toString())
        }
        int location = parent.children().indexOf(node)
        List<Node> nodes = node.children()
        parent.remove(node)
        nodes.remove(node)
        nodes.reverse().each { cn -> parent.children().add(location, cn)}
    }

    static void replaceNode(def oldNode, groovy.util.Node newNode) {
        groovy.util.Node parent = oldNode.parent()
        if(!parent) {
            log.error("null parent!")
            log.error(oldNode.name().toString())
            log.error(oldNode.toString())
        }
        int location = parent.children().indexOf(oldNode)
        //List<Node> nodes = parent.children()
        parent.remove(oldNode)
        parent.children().add(location, newNode)
    }



    static groovy.util.Node replaceTable(groovy.util.Node originalTable) {
        Table table = new Table()
        TGroup tGroup = new TGroup()
        table.tGroups.add(tGroup)
        int maxCols = 0
        List<Node> ths = originalTable.children().findAll{ it instanceof groovy.util.Node && it.name().equalsIgnoreCase("th")}
        List<Node> trs = originalTable.children().findAll{ it instanceof groovy.util.Node && it.name().equalsIgnoreCase("tr")}
        if(trs) {
            tGroup.tBody = new TBody()
            trs.each {row ->
                Row newRow = new Row()
                tGroup.tBody.rows.add(newRow)
                List<Node> tds = row.children().findAll {it instanceof groovy.util.Node &&
                        (it.name().equalsIgnoreCase("td") || it.name().equalsIgnoreCase("th"))}
                int position = 0
                int cols = 0
                for(int i=0;i<tds.size();i++) {
                    Entry entry = new Entry()
                    groovy.util.Node td = tds[i]
                    if(td.attributes()["rowspan"]) {
                        int moreRows = Integer.parseInt(td.attributes()["rowspan"].toString()) - 1
                        entry.moreRows = moreRows
                    }
                    if(td.attributes()["colspan"]) {
                        int colspan = Integer.parseInt(td.attributes()["colspan"].toString())
                        entry.nameSt = "col${position+1}"
                        position+= (colspan - 1)
                        cols += colspan
                        entry.nameEnd = "col${position+1}"
                    } else {
                        cols++
                    }
                    position++

                    if(td.name().equalsIgnoreCase("th") || td.attributes()["class"].toString().contains("duckblue")) {
                        entry.scope="col"
                    }

                    td.children().each { it ->
                        if(it instanceof groovy.util.Node) {
                            entry.contents.add(new Html(content: XmlUtil.serialize(it).replaceFirst("<\\?xml version=\"1.0\".*\\?>", "")))
                        } else {
                            entry.contents.add(new Html(content: "<p>${it}</p>"))
                        }

                    }

                    newRow.entries.add(entry)
                }
                if(cols > maxCols) {
                    maxCols = cols
                }
            }
        }
        tGroup.cols = maxCols
        for(int i=0;i<maxCols;i++) {
            tGroup.colSpecs.add(new ColSpec(colName: "col${i+ 1}", colWidth: "1*"))
        }
        return table.toXmlNode()
    }


}
