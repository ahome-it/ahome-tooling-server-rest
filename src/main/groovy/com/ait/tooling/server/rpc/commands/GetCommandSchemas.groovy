/*
 * Copyright (c) 2014,2015 Ahome' Innovation Technologies. All rights reserved.
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
 */

package com.ait.tooling.server.rpc.commands

import groovy.transform.CompileStatic

import org.springframework.stereotype.Service

import com.ait.tooling.common.api.java.util.StringOps
import com.ait.tooling.server.core.json.JSONObject
import com.ait.tooling.server.core.security.AuthorizationResult
import com.ait.tooling.server.core.security.Authorized
import com.ait.tooling.server.rpc.IJSONCommand
import com.ait.tooling.server.rpc.IJSONRequestContext
import com.ait.tooling.server.rpc.JSONCommandSupport

@Service
@Authorized
@CompileStatic
public class GetCommandSchemas extends JSONCommandSupport
{
    @Override
    public JSONObject execute(final IJSONRequestContext context, final JSONObject object) throws Exception
    {
        final IJSONCommand command = getCommand(StringOps.requireTrimOrNull(object.getAsString('name'), 'Field [name] missing, null, or empty in request'))

        if (command)
        {
            final AuthorizationResult auth = isAuthorized(command, context.getUserRoles())

            if (auth)
            {
                if (auth.isAuthorized())
                {
                    return json([schemas: [name: command.getName(), request: command.getRequestSchema(), response: command.getResponseSchema()]])
                }
                else
                {
                    return json(error: auth.getText(), name: command.getName())
                }
            }
            else
            {
                return json(error: 'Missing or null AuthroizationResult', name: command.getName())
            }
        }
        json(error: 'Not found', name: command.getName())
    }
}
