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
package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd


import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @since 22/12/2021
 */
class MinifyXml {

    static String minify(String absolutePathToDirectory, String xmlFileName, String monthRelease) {
        Path directory = Paths.get(absolutePathToDirectory)
        Path testFilePath = directory.resolve(xmlFileName)
        assert Files.exists(testFilePath)
        Document document = DocumentHelper.parseText(Files.readString(testFilePath))
        OutputFormat format = OutputFormat.createCompactFormat()
        StringWriter stringWriter = new StringWriter()
        XMLWriter writer = new XMLWriter(stringWriter, format)
        writer.write(document)
        String resultStr = stringWriter.toString()

        resultStr = resultStr.replaceAll("file:/C:/Users/lige1/git/data_dictionary_codes/src/main/resources/xslt/$monthRelease", 'file:/root')

        Path outputDirectory = Paths.get('src/integration-test/resources')
        Files.write(outputDirectory.resolve('minified.xml'), resultStr.bytes)
    }

    static void main(String[] args) {

        //minify('/Users/oliverfreeman/Downloads', 'output8_sept_2021.xml', 'September_2021')
    }
}
