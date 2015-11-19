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

package com.ait.tooling.server.rest;

import java.io.Closeable;

import com.ait.tooling.common.api.types.INamed;
import com.ait.tooling.server.core.json.JSONObject;
import com.ait.tooling.server.core.json.schema.JSONSchema;
import com.ait.tooling.server.core.locking.IRateLimited;

public interface IRESTService extends INamed, IRateLimited, Closeable
{
    public String getRequestBinding();

    public JSONObject execute(IRESTRequestContext context, JSONObject object) throws Exception;

    public JSONObject getSchemas();
    
    public JSONSchema getRequestSchema();

    public JSONSchema getResponseSchema();
    
    public boolean isRequestTypeValid(RequestType type);
    
    public RequestType[] getRequestTypes();
    
    public JSONObject getSwaggerAttributes();
}
