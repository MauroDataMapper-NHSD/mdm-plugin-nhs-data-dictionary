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
package uk.nhs.digital.maurodatamapper.datadictionary.rewrite.publish

import uk.ac.ox.softeng.maurodatamapper.dita.DitaProject

import net.lingala.zip4j.ZipFile
import uk.nhs.digital.maurodatamapper.datadictionary.rewrite.NhsDataDictionary

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class WebsiteUtility {

    static final String GITHUB_BRANCH_URL = "https://github.com/NHSDigital/DataDictionaryPublication/archive/refs/heads/master.zip"

    static void generateWebsite(NhsDataDictionary dataDictionary, String outputPath) {

        DitaProject ditaProject = new DitaProject().tap {
            title = "NHS Data Dictionary"
            filename = "changePaper"
        }

        //ditaProject.writeToDirectory(outputPath)

        InputStream inputStream = new URL(GITHUB_BRANCH_URL).openStream()
        String sourceFile = outputPath + "/github_download.zip"
        Files.copy(inputStream, Paths.get(sourceFile), StandardCopyOption.REPLACE_EXISTING)
        ZipFile zipFile = new ZipFile(sourceFile)
        zipFile.extractFile("DataDictionaryPublication-master/Website/", outputPath)

        Files.list(new File(outputPath + "/DataDictionaryPublication-master/Website/").toPath()).forEach {path ->
            Files.move(path, new File(outputPath).toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        Files.delete(new File(sourceFile).toPath())
    }


    //    try {
    //        // source & destination directories
    //        Path src = Paths.get("dir");
    //        Path dest = Paths.get("dir-new");
    //
    //        // create stream for `src`
    //        Stream<Path> files = Files.walk(src);
    //
    //        // copy all files and folders from `src` to `dest`
    //        files.forEach(file -> {
    //            try {
    //                Files.copy(file, dest.resolve(src.relativize(file)),
    //                           StandardCopyOption.REPLACE_EXISTING);
    //            } catch (IOException e) {
    //                e.printStackTrace();
    //            }
    //        });
    //
    //        // close the stream
    //        files.close();
    //
    //    } catch (IOException ex) {
    //        ex.printStackTrace();
    //    }


}
