/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 */
package uk.nhs.digital.maurodatamapper.datadictionary.rewrite

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder

class NhsDDDataSetFolder implements NhsDataDictionaryComponent <Folder> {

    List<String> folderPath = []

    Map<String, NhsDDDataSetFolder> childFolders = [:]
    Map<String, NhsDDDataSet> dataSets = [:]

    @Override
    String getStereotype() {
        "Data Set Folder"
    }

    @Override
    String getStereotypeForPreview() {
        "dataSet"
    }

    @Override
    boolean isValidXmlNode(def xmlNode) {
        return xmlNode."name".text().contains("Introduction") &&
               xmlNode."base-uri".text().contains("Messages")
    }


    @Override
    String calculateShortDescription() {
        return null
    }

    @Override
    String getXmlNodeName() {
        "DDWebPage"
    }

    @Override
    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        NhsDataDictionaryComponent.super.fromXml(xml, dataDictionary)
        //dataDictionary.webPagesByUin[getUin()] = this
        if(otherProperties["baseUri"].contains("Messages")) {
            folderPath = getWebPath()
        }
    }

    String getMauroPath() {
        return ""
    }

    List<String> getDitaFolderPath() {
        folderPath.collect {
            it.replaceAll("[^A-Za-z0-9 ]", "").replace(" ", "_")
        }
    }


}