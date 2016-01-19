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

package com.ait.tooling.server.rest

import groovy.transform.CompileStatic
import groovy.transform.Memoized

import org.springframework.stereotype.Service

import com.ait.tooling.common.api.java.util.StringOps
import com.ait.tooling.server.core.json.JSONObject
import com.ait.tooling.server.core.json.schema.JSONSchema
import com.ait.tooling.server.core.locking.IRateLimited.RateLimiterFactory
import com.ait.tooling.server.rest.support.RESTSupport
import com.google.common.util.concurrent.RateLimiter

@CompileStatic
public abstract class RESTServiceSupport extends RESTSupport implements IRESTService
{
    private RateLimiter m_ratelimit

    public RESTServiceSupport()
    {
        m_ratelimit = RateLimiterFactory.create(getClass())
    }

    @Override
    public void acquire()
    {
        if (m_ratelimit)
        {
            m_ratelimit.acquire()
        }
    }

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
        claz.getSimpleName()
    }

    @Memoized
    public String getRequestBinding()
    {
        final Class<?> claz = getClass()

        if (claz.isAnnotationPresent(RequestBinding))
        {
            return fixRequestBinding(StringOps.toTrimOrNull(claz.getAnnotation(RequestBinding).value()))
        }
        null
    }

    @Memoized
    public RequestMethodType getRequestMethodType()
    {
        final Class<?> claz = getClass()

        if (claz.isAnnotationPresent(RequestMethod))
        {
            return claz.getAnnotation(RequestMethod).value()
        }
        RequestMethodType.getDefaultRequestMethodType()
    }

    @Memoized
    public boolean isRequestTypeValid(RequestMethodType type)
    {
        getRequestMethodType() == type;
    }

    @Override
    public JSONObject getSchemas()
    {
        json(request: getRequestSchema(), response: getResponseSchema())
    }

    @Override
    public JSONSchema getRequestSchema()
    {
        jsonSchema(type: 'object', properties: [:])
    }

    @Override
    public JSONSchema getResponseSchema()
    {
        jsonSchema(type: 'object', properties: [:])
    }

    @Override
    public JSONObject getSwaggerAttributes()
    {
        json(path: getRequestBinding() ?: fixRequestBinding(getName()), method: getRequestMethodType(), schemas: getSchemas())
    }
}
