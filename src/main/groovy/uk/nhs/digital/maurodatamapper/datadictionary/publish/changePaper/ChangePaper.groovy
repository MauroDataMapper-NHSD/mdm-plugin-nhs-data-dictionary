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
package uk.nhs.digital.maurodatamapper.datadictionary.publish.changePaper

import groovy.xml.MarkupBuilder
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Div
import uk.ac.ox.softeng.maurodatamapper.dita.helpers.HtmlHelper
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDAttribute
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSet
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDDataSetElement
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDElement
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionary
import uk.nhs.digital.maurodatamapper.datadictionary.NhsDataDictionaryComponent

import java.text.SimpleDateFormat

class ChangePaper {

    static String example1 = "<p>Here is some text</p>"

    static String example2 = "<p>Here is some more text</p>"



    static String example3 = "<p>       <strong>Introduction</strong>    </p>    <p> The purpose of the     <a href=\"fo:Central Return Data " +
            "Setsdm:Venous" +
            " " +
            "Thromboembolism Risk Assessment Data Set\">Venous Thromboembolism Risk Assessment Data Set</a>  is to quantify the number of " +
            "    <a href=\"dm:NHS Data Dictionary - Core|dc:Classes|dc:PATIENT\">PATIENTS</a>  (aged 16 and over) admitted to hospital, " +
            "who are risk assessed for Venous Thromboembolism using the     <a href=\"te:NHS Business Definitions|tm:Venous " +
            "Thromboembolism Risk Assessment Tool\">Venous Thromboembolism Risk Assessment Tool</a>  to allow appropriate preventative " +
            "treatment based on guidance from the     <a href=\"te:NHS Business Definitions|tm:National Institute for Health and Care " +
            "Excellence\">National Institute for Health and Care Excellence</a>  (    <a href=\"te:NHS Business Definitions|tm:National " +
            "Institute for Health and Care Excellence\">NICE</a> ).  </p>    <p>       <strong>Collection and submission</strong>    </p> " +
            "   <p> All providers of NHS funded acute hospital care (including     <a href=\"te:NHS Business Definitions|tm:NHS Foundation" +
            " Trust\">NHS Foundation Trusts</a>  and     <a href=\"te:NHS Business Definitions|tm:Independent Provider\">Independent " +
            "Providers</a>  of acute NHS services) must complete this data collection.  </p>    <p> Data on Venous Thromboembolism risk " +
            "assessments is uploaded onto the     <a href=\"te:Supporting Information|tm:Strategic Data Collection Service\">Strategic " +
            "Data Collection Service</a>  (    <a href=\"te:Supporting Information|tm:Strategic Data Collection Service\">SDCS</a> ) each " +
            "month no later than 20 working days after the month end. Revisions to the data set before the cut off date are allowed, " +
            "however revisions made after the cut off date must be made in liaison with     <a href=\"te:NHS Business Definitions|tm:NHS " +
            "England\">NHS England</a> .</p>    <p>       " +
            "<strong>Further guidance</strong>    </p>    <p> For further guidance on the     <a href=\"fo:Central Return Data " +
            "Setsdm:Venous Thromboembolism Risk Assessment Data Set\">Venous Thromboembolism Risk Assessment Data Set</a> , see the:  </p>" +
            "    <ul>       <li>          <div>             <a href=\"te:NHS Business Definitions|tm:NHS England\">NHS England</a>  " +
            "website at         <a href=\"https://www.england.nhs.uk/statistics/statistical-work-areas/vte/\" target=\"_blank\">Venous " +
            "thromboembolism (VTE) risk assessment</a>          </div>       </li>       <li>          <div>             <a href=\"te:NHS " +
            "Business Definitions|tm:National Institute for Health and Care Excellence\">National Institute for Health and Care " +
            "Excellence</a>  website at         <a href=\"https://www.nice.org.uk/guidance/CG92\" target=\"_blank\">Venous thromboembolism" +
            " - reducing the risk</a> .      </div>       </li>    </ul>    <p>       <strong>Mandation</strong>    </p>    <p>The " +
            "Mandation column indicates the recommendation for the inclusion of data.</p>    <ul>       <li>          <div>M = Mandatory: " +
            "this data element is mandatory and the technical process (e.g. submission of the data set, production of output etc) cannot " +
            "be completed without this data element being present.</div>       </li>    </ul>"

    static String example4 = "<p>       <strong>Introduction</strong>    </p>    <p> The purpose of the     <a href=\"fo:Central Return Data " +
                       "Setsdm:Venous" +
                      " " +
                     "Thromboembolism Risk Assessment Data Set\">Venous Thromboembolism Risk Assessment Data Set</a>  is to quantify the number of " +
                     "    <a href=\"dm:NHS Data Dictionary - Core|dc:Classes|dc:PATIENT\">PATIENTS</a>  (aged 16 and over) admitted to hospital, " +
                     "who are risk assessed for Venous Thromboembolism using the     <a href=\"te:NHS Business Definitions|tm:Venous " +
                     "Thromboembolism Risk Assessment Tool\">Venous Thromboembolism Risk Assessment Tool</a>  to allow appropriate preventative " +
                     "treatment based on guidance from the     <a href=\"te:NHS Business Definitions|tm:National Institute for Health and Care " +
                     "Excellence\">National Institute for Health and Care Excellence</a>  (    <a href=\"te:NHS Business Definitions|tm:National " +
                     "Institute for Health and Care Excellence\">NICE</a> ).  </p>    <p>       <strong>Collection and submission</strong>    </p> " +
                     "   <p> All providers of NHS funded acute hospital care (including     <a href=\"te:NHS Business Definitions|tm:NHS Foundation" +
                     " Trust\">NHS Foundation Trusts</a>  and     <a href=\"te:NHS Business Definitions|tm:Independent Provider\">Independent " +
                     "Providers</a>  of acute NHS services) must complete this data collection.  </p>    <p> Data on Venous Thromboembolism risk " +
                     "assessments is uploaded onto the     <a href=\"te:Supporting Information|tm:Strategic Data Collection Service\">Strategic " +
                     "Data Collection Service</a>  (    <a href=\"te:Supporting Information|tm:Strategic Data Collection Service\">SDCS</a> ) each " +
                     "month no later than 20 working days after the month end. Revisions to the data set before the cut off date are allowed, " +
                     "however revisions made after the cut off date must be made in liaison with     <a href=\"te:NHS Business Definitions|tm:NHS " +
                     "England\">NHS England</a> .</p><p>  Here is an extra sentence added as part of the CR12345 change.</p>    <p>       " +
                     "<strong>Further guidance</strong>    </p>    <p> For further guidance on the     <a href=\"fo:Central Return Data " +
                     "Setsdm:Venous Thromboembolism Risk Assessment Data Set\">Venous Thromboembolism Risk Assessment Data Set</a> , see the:  </p>" +
                     "    <ul>       <li>          <div>             <a href=\"te:NHS Business Definitions|tm:NHS England\">NHS England</a>  " +
                     "website at         <a href=\"https://www.england.nhs.uk/statistics/statistical-work-areas/vte/\" target=\"_blank\">Venous " +
                     "thromboembolism (VTE) risk assessment</a>          </div>       </li>       <li>          <div>             <a href=\"te:NHS " +
                     "Business Definitions|tm:National Institute for Health and Care Excellence\">National Institute for Health and Care " +
                     "Excellence</a>  website at         <a href=\"https://www.nice.org.uk/guidance/CG92\" target=\"_blank\">Venous thromboembolism" +
                     " - reducing the risk</a> .      </div>       </li>    </ul>    <p>       <strong>Mandation</strong>    </p>    <p>The " +
                     "Mandation column indicates the recommendation for the inclusion of data.</p>    <ul>       <li>          <div>M = Mandatory: " +
                     "this data element is mandatory and the technical process (e.g. submission of the data set, production of output etc) cannot " +
                     "be completed without this data element being present.</div>       </li>    </ul>"

/*    static void main(String[] args) {

        XmlParser xmlParser = new XmlParser(false, false)
        //Node node1 = xmlParser.parseText(example1)
        //Node node2 = xmlParser.parseText(example2)


        String str3 = HtmlHelper.applyJTidy("<div>" + example3 + "</div>").toString()
        String str4 = HtmlHelper.applyJTidy("<div>" + example4 + "</div>").toString()

        System.err.println(diff(str3, str4))


        Source source = Input.fromString(str3).build()
        Source target = Input.fromString(str4).build()
        Diff myDiff = DiffBuilder.compare(source).withTest(target)
                //.checkForSimilar()
                .checkForIdentical()
                .ignoreWhitespace()

                .build()

        XPathEngine xPathEngine = new JAXPXPathEngine()


        myDiff.differences.each {difference ->
            xPathEngine.selectNodes(difference.comparison.testDetails.XPath, source).each { node ->
                Element replacementNode = node.getOwnerDocument().createElement("div")
                replacementNode.setAttribute("class", "deleted")
                node.getParentNode().replaceChild(replacementNode, node)
            }
        }
        System.err.println(myDiff.testSource)


        nodeDiff(node1, node2).each {
            System.err.println(XmlUtil.serialize(it))
        }

    }
*/

/*
    static List<String> recurseNodes = ["div", "ol", "ul", "table", "tbody", "thead"]

    static List<Node> nodeDiff(Node a, Node b) {
        List<Node> returnNodes = []

        if(nodeEquals(a,b)) {
            returnNodes.add(a)
        } else {
            if(a.name() == b.name() && recurseNodes.contains(a.name())) {
                Node replacementA = new Node(a.parent(), a.name(), a.attributes())

                nodeDiff(a.children(), b.children()).each {
                    replacementA.append(it)
                }

                returnNodes.add(replacementA)

            } else {
                if(a."@class") {
                    a."@class" = a."@class".toString() + " new"
                } else {
                    a."@class" = "new"
                }
                if(b."@class") {
                    b."@class" = b."@class".toString() + " old"
                } else {
                    b."@class" = "old"
                }
                returnNodes.add(a)
                returnNodes.add(b)
            }
        }
        return returnNodes
    }


    List<Node> nodeDiff(List<Node> aList, List<Node> bList) {
        if(aList.size() == 0) {
            return bList
        }
        if(bList.size() == 0) {
            return aList
        }
        Node a = aList[0]
        Node b = bList[0]
        if(nodeEquals(a,b)) {
            List<Node> returnNodes = [a]
            aList.remove(0)
            bList.remove(0)
            returnNodes.addAll(nodeDiff(aList, bList))
            return returnNodes
        } else {

        }

    }

    boolean nodeEquals(Node a, Node b) {
        return (a.name() == b.name() && a.text() == b.text())
    }

*/

    List<StereotypedChange> stereotypedChanges = []

    String reference
    String type
    String versionNo
    String subject
    String effectiveDate
    String reasonForChange
    String publicationDate
    String background
    String sponsor

    ChangePaper(NhsDataDictionary nhsDataDictionary, NhsDataDictionary oldDataDictionary, boolean includeDataSets = false) {
        reference = nhsDataDictionary.workItemDetails['reference'] ?: 'CRXXXX'
        type = nhsDataDictionary.workItemDetails['type'] ?: 'Change Request'
        versionNo = nhsDataDictionary.workItemDetails['versionNo'] ?: '1.0'
        subject = nhsDataDictionary.workItemDetails['subject'] ?: 'NHS England and NHS Improvement'
        effectiveDate = nhsDataDictionary.workItemDetails['effectiveDate'] ?: 'Immediate'
        reasonForChange = nhsDataDictionary.workItemDetails['reasonForChange'] ?: 'Change to definitions'
        background = nhsDataDictionary.workItemDetails['backgroundText'] ?: backgroundText()
        sponsor = nhsDataDictionary.workItemDetails['sponsor'] ?: 'NHS England'

        SimpleDateFormat publicationDateFormat = new SimpleDateFormat("dd MMM yyyy")
        publicationDate = publicationDateFormat.format(new Date())

        stereotypedChanges = calculateChanges(nhsDataDictionary, oldDataDictionary, includeDataSets)
    }

    Map<String, String> propertiesAsMap() {

        Map<String, String> response = [:]

        response['Reference'] = reference
        response['Type'] = type
        response['Version no.'] = versionNo
        response['Subject'] = subject
        response['Effective date'] = effectiveDate
        response['Reason for change'] = reasonForChange
        response['Publication Date'] = publicationDate
        response['Background'] = background
        response['Sponsor'] = sponsor

        return response
    }

    private List<StereotypedChange> calculateChanges(NhsDataDictionary thisDataDictionary, NhsDataDictionary previousDataDictionary, boolean includeDataSets = false) {
        List<StereotypedChange> changedItems = []
        changedItems += new StereotypedChange(stereotypeName: "NHS Business Definition",
              changedItems: compareMaps(thisDataDictionary.businessDefinitions, previousDataDictionary?.businessDefinitions))
        changedItems += new StereotypedChange(stereotypeName: "Supporting Information",
              changedItems: compareMaps(thisDataDictionary.supportingInformation, previousDataDictionary?.supportingInformation))

        StereotypedChange attributeChange = new StereotypedChange(stereotypeName: "Attribute",
                changedItems: compareMaps(thisDataDictionary.attributes, previousDataDictionary?.attributes))

        changedItems += attributeChange

        StereotypedChange elementChange = new StereotypedChange(stereotypeName: "Data Element",
                changedItems: compareMaps(thisDataDictionary.elements, previousDataDictionary?.elements))

        changedItems += elementChange

        changedItems += new StereotypedChange(stereotypeName: "Class",
              changedItems: compareMaps(thisDataDictionary.classes, previousDataDictionary?.classes))

        List<ChangedItem> dataSetChanges = compareMaps(thisDataDictionary.dataSets, previousDataDictionary?.dataSets, includeDataSets)

        changedItems += new StereotypedChange(stereotypeName: "Data Set",
              changedItems: dataSetChanges)
        changedItems += new StereotypedChange(stereotypeName: "Data Set Constraint",
              changedItems: compareMaps(thisDataDictionary.dataSetConstraints, previousDataDictionary?.dataSetConstraints))

        if(includeDataSets && dataSetChanges.size() > 0) {
            Set<NhsDDDataSet> changedDataSets = [] as Set<NhsDDDataSet>
            dataSetChanges.each { changedItem ->
                changedDataSets.add((NhsDDDataSet)changedItem.dictionaryComponent)
            }
            changedDataSets.each { dataSet ->
                Set<NhsDDDataSetElement> dataSetElements = dataSet.getAllElements()
                Set<NhsDDAttribute> dataSetAttributes = [] as Set<NhsDDAttribute>
                dataSetElements.each {dataSetElement ->

                    elementChange.changedItems.add(new ChangedItem(
                            dictionaryComponent: dataSetElement.reuseElement,
                            changes: [new Change(
                                    changeType: "Unchanged Item",
                                    stereotype: dataSetElement.reuseElement.stereotype,
                                    oldItem: null,
                                    newItem: dataSetElement.reuseElement,
                                    htmlDetail: dataSetElement.reuseElement.description,
                                    ditaDetail: Div.build {
                                        div HtmlHelper.replaceHtmlWithDita(dataSetElement.reuseElement.description)
                                    })]
                    ))
                    dataSetAttributes.addAll(((NhsDDElement)dataSetElement.reuseElement).instantiatesAttributes)
                }
                dataSetAttributes.each { attribute ->

                    attributeChange.changedItems.add(new ChangedItem(
                            dictionaryComponent: attribute,
                            changes: [new Change(
                                    changeType: "Unchanged Item",
                                    stereotype: attribute.stereotype,
                                    oldItem: null,
                                    newItem: attribute,
                                    htmlDetail: attribute.description,
                                    ditaDetail: Div.build {
                                        div HtmlHelper.replaceHtmlWithDita(attribute.description)
                                    })]
                    ))
                }
            }
        }

        return changedItems
    }

    private static List<ChangedItem> compareMaps(
        Map<String, NhsDataDictionaryComponent> newList,
        Map<String, NhsDataDictionaryComponent> oldList,
        boolean includeDataSets = false) {
        List<ChangedItem> changedItems = []
        newList.sort{ it.key }.each {name, component ->
            NhsDataDictionaryComponent previousComponent = oldList?[name]
            List<Change> changes = component.getChanges(previousComponent, includeDataSets)
            if (changes) {
                changedItems.add(new ChangedItem(
                    dictionaryComponent: component,
                    changes: changes
                ))
            }
        }
        return changedItems
    }

    static String backgroundText() {
        StringWriter writer = new StringWriter()
        MarkupBuilder markupBuilder = new MarkupBuilder(writer)
        markupBuilder.div {
            p "This Change Request adds the ???? Data Set and supporting definitions to the NHS Data Model and Dictionary to support the Information Standard."
            p {
                mkp.yield "A short demonstration is available which describes \"How to Read an NHS Data Model and Dictionary Change Request\", " +
                    "in an easy to understand screen capture including a voice over and readable captions. This demonstration can be viewed at: "

                a (href: "https://datadictionary.nhs.uk/elearning/change_request/index.html",
                   "https://datadictionary.nhs.uk/elearning/change_request/index.html.")
                mkp.yield "."
            }

            p {
                mkp.yield "Note: if the web page does not open, please copy the link and paste into the web browser. A guide to how to use the " +
                  "demonstration can be found at: "
                a(href: "https://datadictionary.nhs.uk/help/demonstrations.html", "Demonstrations")
                mkp.yield "."
            }
        }
        return writer.toString()
    }
/*
    Map<String, List<Change>> getStereotypedChanges() {

        Map<String, List<Change>> stereotypedChangesResult = [:]

        changes.each { change ->
            List<Change> changesForStereotype = stereotypedChangesResult[change.stereotype]
            if(!changesForStereotype) {
                changesForStereotype = [change]
                stereotypedChangesResult[change.stereotype] = changesForStereotype

            } else {
                changesForStereotype.add(change)
            }
        }
        return stereotypedChangesResult
    }
*/
}
