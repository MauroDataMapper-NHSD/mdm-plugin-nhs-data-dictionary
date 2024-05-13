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

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ModelDataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService

import uk.nhs.digital.maurodatamapper.datadictionary.NhsDDElement


class GoogleSheetsController implements ResourcelessMdmController {

	VersionedFolderService versionedFolderService

	ElementService elementService
	CodeSetService codeSetService

	def dataElement() {
		VersionedFolder latestDictionary = versionedFolderService.findLatestFinalisedFolderByLabel("NHS Data Dictionary")
		Set<DataElement> allElements = elementService.getAll(latestDictionary.id, true)
		String responseString = ""
		if(params.dataElementName.toString().toLowerCase().replace("_", " ") == "data elements") {
			responseString = allElements.sort {it.label}.collect {it.label}.join("\n")
		} else {

			DataElement dataElement = allElements.find {
				it.label.toLowerCase() == params.dataElementName.toString().toLowerCase().replace("_", " ")
			}

			if (!dataElement) {
				// return empty string
			} else {
				if (dataElement.dataType instanceof ModelDataType) {
					ModelDataType modelDataType = (ModelDataType) dataElement.dataType
					CodeSet codeSet = codeSetService.get(modelDataType.modelResourceId)
					responseString =
						codeSet.terms.sort {
							it.code
						}.collect { term ->
								"${term.code} (${term.definition})"
							}.join("\n")

				} else if (dataElement.dataType instanceof PrimitiveType) {
					responseString = dataElement.metadata.find() {
						it.key == "formatLength"
					}?.value
				}
			}
		}
		response.setContentType("application/txt")
		render(responseString)
	}

}
