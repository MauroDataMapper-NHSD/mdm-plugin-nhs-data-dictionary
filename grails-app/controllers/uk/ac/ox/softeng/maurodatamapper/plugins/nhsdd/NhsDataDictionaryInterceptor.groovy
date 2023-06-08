/*
 * Copyright 2020-2023 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup


class NhsDataDictionaryInterceptor implements MdmInterceptor {

    public static final TERMINOLOGY_SERVER_ADMINISTRATORS_USER_GROUP_NAME = "Terminology Server Administrators"
    public static final WEBSITE_PREVIEW_GENERATORS_USER_GROUP_NAME = "Website Preview Generators"

    static final List<String> adminActions = [
            'ingest'
    ]


    boolean before() {
        //User currentUser = getCurrentUser()

        if (actionName in adminActions) {
            return currentUserSecurityPolicyManager.isApplicationAdministrator() ?: forbiddenDueToNotApplicationAdministrator()
        }


        if(invalidUserGroup(["codeSystemValidateBundle","valueSetValidateBundle"],
                TERMINOLOGY_SERVER_ADMINISTRATORS_USER_GROUP_NAME)) {
            return forbiddenDueToPermissions()
        }
        if(invalidUserGroup(["generateWebsite"],
                WEBSITE_PREVIEW_GENERATORS_USER_GROUP_NAME)) {
            return forbiddenDueToPermissions()
        }
        return true
    }

    boolean invalidUserGroup(List<String> actionNames, String userGroupName) {
        return actionNames.contains(actionName) &&
                !currentUser.groups.find { UserGroup userGroup ->
                    userGroup.name == userGroupName
                }

    }


    boolean after() { true }

    void afterView() {
        // no-op
    }
}
