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

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.MDC;

import com.ait.tooling.json.JSONObject;
import com.ait.tooling.server.core.servlet.HTTPServletBase;
import com.ait.tooling.server.core.support.spring.IServerContext;
import com.ait.tooling.server.core.support.spring.ServerContextInstance;

public class JSONRequestContext implements IJSONRequestContext
{
    private final String              m_userid;

    private final String              m_sessid;

    private final boolean             m_admin;

    private final Iterable<String>    m_roles;

    private final ServletContext      m_servlet_context;

    private final HttpServletRequest  m_servlet_request;

    private final HttpServletResponse m_servlet_response;

    public JSONRequestContext(String userid, String sessid, boolean admin, Iterable<String> roles, ServletContext context, HttpServletRequest request, HttpServletResponse response)
    {
        m_userid = userid;

        m_sessid = sessid;

        m_admin = admin;

        m_roles = roles;

        m_servlet_context = context;

        m_servlet_request = request;

        m_servlet_response = response;
    }

    @Override
    public boolean isAdmin()
    {
        return m_admin;
    }

    public IServerContext getServerContext()
    {
        return ServerContextInstance.getServerContextInstance();
    }

    @Override
    public ServletContext getServletContext()
    {
        return m_servlet_context;
    }

    @Override
    public HttpServletRequest getServletRequest()
    {
        return m_servlet_request;
    }

    @Override
    public HttpServletResponse getServletResponse()
    {
        return m_servlet_response;
    }

    @Override
    public String getSessionID()
    {
        return m_sessid;
    }

    @Override
    public String getUserID()
    {
        return m_userid;
    }

    @Override
    public void setCookie(String name, String value)
    {
        HttpServletRequest request = getServletRequest();

        HttpServletResponse response = getServletResponse();

        if ((null != request) && (null != response) && (null != name) && (false == (name = name.trim()).isEmpty()))
        {
            if (null == value)
            {
                Cookie cookie = new Cookie(name, "");

                cookie.setMaxAge(0);

                String ruri = request.getHeader("Referer");

                if (null != ruri)
                {
                    if (ruri.startsWith("https"))
                    {
                        cookie.setSecure(true);
                    }
                }
                response.addCookie(cookie);
            }
            else
            {
                Cookie cookie = new Cookie(name, value);

                cookie.setMaxAge(60 * 60 * 24 * 365); // one year

                String ruri = request.getHeader("Referer");

                if (null != ruri)
                {
                    if (ruri.startsWith("https"))
                    {
                        cookie.setSecure(true);
                    }
                }
                response.addCookie(cookie);
            }
        }
    }

    @Override
    public void doResetMDC()
    {
        MDC.put("session", ((m_userid == null) ? "no-userid" : m_userid) + "-" + ((m_sessid == null) ? "no-sessid" : m_sessid));
    }

    @Override
    public JSONObject getJSONHeaders()
    {
        return HTTPServletBase.getJSONHeadersFromRequest(getServletRequest());
    }

    @Override
    public JSONObject getJSONParameters()
    {
        return HTTPServletBase.getJSONParametersFromRequest(getServletRequest());
    }

    @Override
    public Iterable<String> getUserRoles()
    {
        return m_roles;
    }
}
