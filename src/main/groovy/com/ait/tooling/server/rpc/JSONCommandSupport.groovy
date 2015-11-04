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

package com.ait.tooling.server.rpc

import groovy.transform.CompileStatic
import groovy.transform.Memoized

import org.springframework.stereotype.Service

import com.ait.tooling.common.api.java.util.StringOps
import com.ait.tooling.server.core.json.JSONObject
import com.ait.tooling.server.core.json.schema.JSONSchema
import com.ait.tooling.server.rpc.support.RPCSupport

@CompileStatic
public abstract class JSONCommandSupport extends RPCSupport implements IJSONCommand
{
    @Memoized
    public String getName()
    {
        final Class<?> claz = getClass()

        if (claz.isAnnotationPresent(Service))
        {
            final String name = StringOps.toTrimOrNull(claz.getAnnotation(Service).value())

            if (name)
            {
                return name
            }
        }
        claz.getSimpleName().trim()
    }

    @Memoized
    public String getRequestPath()
    {
        final Class<?> claz = getClass()

        if (claz.isAnnotationPresent(RequestPath))
        {
            final String path = StringOps.toTrimOrNull(claz.getAnnotation(RequestPath).value())

            if (path)
            {
                return path
            }
        }
        getName()
    }

    @Memoized
    public boolean isRequestOfType(final RequestType type)
    {
        final Class<?> claz = getClass()

        if (claz.isAnnotationPresent(RequestMethods))
        {
            return Arrays.asList(claz.getAnnotation(RequestMethods).value()).contains(type)
        }
        true
    }

    @Override
    public JSONObject getValidation()
    {
        json([request: false, response: false])
    }

    @Override
    public JSONObject getCommandMetaData()
    {
        json([name: getRequestPath(), validation: getValidation(), request: getRequestSchema(), response: getResponseSchema()])
    }

    @Override
    public JSONSchema getRequestSchema()
    {
        jsonSchema([title: getRequestPath(), description: 'Request Schema for ' + getRequestPath(), type: 'object', properties: [:]])
    }

    @Override
    public JSONSchema getResponseSchema()
    {
        jsonSchema([title: getRequestPath(), description: 'Response Schema for ' + getRequestPath(), type: 'object', properties: [:]])
    }
}
