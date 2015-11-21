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

import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ait.tooling.common.api.java.util.StringOps;
import com.ait.tooling.server.core.json.JSONObject;
import com.ait.tooling.server.core.servlet.HTTPServletBase;
import com.ait.tooling.server.rest.support.spring.IRESTContext;
import com.ait.tooling.server.rest.support.spring.RESTContextInstance;

public class RESTRequestContext implements IRESTRequestContext
{
    private static final long         serialVersionUID = -1336953145524645090L;

    private boolean                   m_closed;

    private final String              m_userid;

    private final String              m_sessid;

    private final RequestMethodType         m_reqtyp;

    private final boolean             m_admin;

    private final List<String>        m_roles;

    private final ServletContext      m_servlet_context;

    private final HttpServletRequest  m_servlet_request;

    private final HttpServletResponse m_servlet_response;

    public RESTRequestContext(String userid, String sessid, boolean admin, List<String> roles, ServletContext context, HttpServletRequest request, HttpServletResponse response, RequestMethodType reqtyp)
    {
        m_closed = false;

        m_reqtyp = reqtyp;

        m_userid = userid;

        m_sessid = sessid;

        m_admin = admin;

        m_roles = Collections.unmodifiableList(roles);

        m_servlet_context = context;

        m_servlet_request = request;

        m_servlet_response = response;
    }

    @Override
    public boolean isGet()
    {
        return (RequestMethodType.GET == getRequestType());
    }

    @Override
    public boolean isPut()
    {
        return (RequestMethodType.PUT == getRequestType());
    }

    @Override
    public boolean isPost()
    {
        return (RequestMethodType.POST == getRequestType());
    }

    @Override
    public boolean isHead()
    {
        return (RequestMethodType.HEAD == getRequestType());
    }

    @Override
    public boolean isDelete()
    {
        return (RequestMethodType.DELETE == getRequestType());
    }

    @Override
    public RequestMethodType getRequestType()
    {
        return m_reqtyp;
    }

    @Override
    public boolean isAdmin()
    {
        return m_admin;
    }

    @Override
    public IRESTContext getRESTContext()
    {
        return RESTContextInstance.getRESTContextInstance();
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

        if ((null != request) && (null != response) && (null != (name = StringOps.toTrimOrNull(name))))
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

                cookie.setMaxAge(60 * 60 * 24 * 365);// one year

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
    public List<String> getUserRoles()
    {
        return m_roles;
    }

    @Override
    public void close()
    {
        m_closed = true;
    }

    @Override
    public boolean isClosed()
    {
        return m_closed;
    }
}