/*
 * Copyright (c) 2014,2015,2016 Ahome' Innovation Technologies. All rights reserved.
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

package com.ait.tooling.server.rest.system.services

import groovy.transform.CompileStatic

import org.springframework.stereotype.Service

import com.ait.tooling.common.api.java.util.StringOps
import com.ait.tooling.server.core.json.JSONObject
import com.ait.tooling.server.rest.*

@Service
@CompileStatic
@RequestMethods(RequestType.POST)
@RequestBinding('/system/service/schema')
public class GetServiceSchema extends RESTServiceSupport
{
    @Override
    public JSONObject execute(final IRESTRequestContext context, final JSONObject object) throws Exception
    {
        final IRESTService service = getService(StringOps.requireTrimOrNull(object.getAsString('service'), 'Field [service] missing, null, or empty in request'))

        if (service)
        {
            return json(schemas: service.getSchemas())
        }
        json(error: object.getAsString('service'))
    }
}
