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
package uk.ac.ox.softeng.maurodatamapper.plugins.nhsdd

import uk.ac.ox.softeng.maurodatamapper.util.Utils

import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TimeOutput {

    static void main(String[] args) {
        String startFirstStr = '2022-02-10 10:31:26,059'
        String startSecondStr = '2022-02-10 10:44:00,314'
        String startThirdStr = '2022-02-10 10:47:01,129'
        String endStr = '2022-02-10 10:47:54,358'


        LocalTime startFirst = LocalTime.parse(startFirstStr, DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss,SSS'))
        LocalTime startSecond = LocalTime.parse(startSecondStr, DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss,SSS'))
        LocalTime startThird = LocalTime.parse(startThirdStr, DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss,SSS'))
        LocalTime end = LocalTime.parse(endStr, DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss,SSS'))


        println "FIRST_PASS: " + Utils.getTimeString(Duration.between(startFirst, startSecond).toMillis())
        println "SECOND_PASS: " + Utils.getTimeString(Duration.between(startSecond, startThird).toMillis())
        println "THIRD_PASS: " + Utils.getTimeString(Duration.between(startThird, end).toMillis())
        println "TOTAL: " + Utils.getTimeString(Duration.between(startFirst, end).toMillis())
    }
}