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

package com.ait.tooling.server.rpc;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ait.tooling.json.JSONObject;
import com.ait.tooling.server.core.support.spring.IServerContext;

public interface IJSONRequestContext
{
    public boolean isAdmin();

    public String getUserID();

    public String getSessionID();

    public IServerContext getServerContext();

    public ServletContext getServletContext();

    public HttpServletRequest getServletRequest();

    public HttpServletResponse getServletResponse();

    public void setCookie(String name, String value);

    public void doResetMDC();

    public JSONObject getJSONHeaders();

    public JSONObject getJSONParameters();

    public List<String> getUserRoles();
}
