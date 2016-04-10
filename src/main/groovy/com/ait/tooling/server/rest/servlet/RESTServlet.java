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

package com.ait.tooling.server.rest.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.http.HttpMethod;

import com.ait.tooling.common.api.java.util.StringOps;
import com.ait.tooling.common.api.java.util.UUID;
import com.ait.tooling.common.server.io.NoSyncBufferedWriter;
import com.ait.tooling.server.core.json.JSONObject;
import com.ait.tooling.server.core.json.parser.JSONParser;
import com.ait.tooling.server.core.json.parser.JSONParserException;
import com.ait.tooling.server.core.security.AuthorizationResult;
import com.ait.tooling.server.core.security.session.IServerSession;
import com.ait.tooling.server.core.security.session.IServerSessionRepository;
import com.ait.tooling.server.core.servlet.HTTPServletBase;
import com.ait.tooling.server.rest.IRESTService;
import com.ait.tooling.server.rest.RESTRequestContext;
import com.ait.tooling.server.rest.support.spring.IRESTContext;
import com.ait.tooling.server.rest.support.spring.RESTContextInstance;

public class RESTServlet extends HTTPServletBase
{
    private static final long         serialVersionUID = 8890049936686095786L;

    private static final Logger       logger           = Logger.getLogger(RESTServlet.class);

    private static final List<String> ANONYMOUS        = Collections.unmodifiableList(Arrays.asList("ANONYMOUS"));

    public RESTServlet()
    {
    }

    protected RESTServlet(final double rate)
    {
        super(rate);
    }

    @Override
    public void doHead(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        doNoCache(response);

        response.setContentLength(0);

        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        doService(request, response, false, HttpMethod.GET, getJSONParametersFromRequest(request));
    }

    @Override
    public void doPut(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        doService(request, response, true, HttpMethod.PUT, null);
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        doService(request, response, true, HttpMethod.POST, null);
    }

    public void doPatch(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        doService(request, response, true, HttpMethod.PATCH, null);
    }

    @Override
    public void doDelete(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        doService(request, response, false, HttpMethod.DELETE, new JSONObject());
    }

    protected void doService(final HttpServletRequest request, final HttpServletResponse response, final boolean read, final HttpMethod type, JSONObject object) throws ServletException, IOException
    {
        if (false == isRunning())
        {
            logger.error("server is suspended, refused request");

            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

            return;
        }
        if (read)
        {
            object = parseJSON(request, type);
        }
        if (null == object)
        {
            logger.error("passed body is not a JSONObject");

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            return;
        }
        String name = null;

        boolean irpc = false;

        if ((read) && isCommandInBody())
        {
            irpc = true;

            name = StringOps.toTrimOrNull(object.getAsString("command"));

            if (null == name)
            {
                logger.error("no command keys found in body");

                response.setStatus(HttpServletResponse.SC_NOT_FOUND);

                return;
            }
        }
        else
        {
            name = StringOps.toTrimOrNull(request.getPathInfo());

            if (null != name)
            {
                int indx = name.indexOf("/");

                if (indx >= 0)
                {
                    name = StringOps.toTrimOrNull(name.substring(indx + 1));
                }
                if (null != name)
                {
                    if (name.contains(".rpc"))
                    {
                        irpc = true;
                    }
                    name = getRESTContext().fixRequestBinding(name);
                }
            }
            if (null == name)
            {
                logger.error("empty service path found");

                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                return;
            }
        }
        IRESTService service = getRESTContext().getService(name);

        if (null == service)
        {
            service = getRESTContext().getBinding(name);

            if (null == service)
            {
                logger.error("service or binding not found " + name);

                response.setStatus(HttpServletResponse.SC_NOT_FOUND);

                return;
            }
        }
        if (type != service.getRequestMethodType())
        {
            logger.error("service " + name + " not type " + type);

            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);

            return;
        }
        if ((read) && (irpc))
        {
            if (false == object.isDefined("request"))
            {
                logger.error("no request key found");

                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                return;
            }
            object = object.getAsObject("request");

            if (null == object)
            {
                logger.error("empty request key found");

                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                return;
            }
        }
        IServerSession session = null;

        List<String> uroles = ANONYMOUS;

        String userid = StringOps.toTrimOrNull(request.getHeader(X_USER_ID_HEADER));

        String sessid = StringOps.toTrimOrNull(request.getHeader(X_SESSION_ID_HEADER));

        String ctoken = StringOps.toTrimOrNull(request.getHeader(X_CLIENT_API_TOKEN_HEADER));

        if (null != sessid)
        {
            final IServerSessionRepository repository = getServerContext().getServerSessionRepository(getSessionProviderDomainName());

            if (null != repository)
            {
                session = repository.getSession(sessid);

                if ((null != session) && (false == session.isExpired()))
                {
                    uroles = session.getRoles();

                    sessid = StringOps.toTrimOrNull(session.getId());

                    userid = StringOps.toTrimOrNull(session.getUserId());
                }
                else
                {
                    logger.error("unknown or expired session " + sessid);

                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                    return;
                }
            }
        }
        else if (null != ctoken)
        {
            final IServerSessionRepository repository = getServerContext().getServerSessionRepository(getSessionProviderDomainName());

            if (null != repository)
            {
                session = repository.createSession(new JSONObject(X_CLIENT_API_TOKEN_HEADER, ctoken));

                if ((null != session) && (false == session.isExpired()))
                {
                    uroles = session.getRoles();

                    sessid = StringOps.toTrimOrNull(session.getId());

                    userid = StringOps.toTrimOrNull(session.getUserId());
                }
                else
                {
                    logger.error("unknown or expired token " + ctoken);

                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                    return;
                }
            }
        }
        if ((null == uroles) || (uroles.isEmpty()))
        {
            uroles = ANONYMOUS;
        }
        final AuthorizationResult resp = isAuthorized(service, uroles);

        if (false == resp.isAuthorized())
        {
            if (null == userid)
            {
                userid = UNKNOWN_USER;
            }
            logger.error("service authorization failed " + name + " for user " + userid + " code " + resp.getText());

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            return;
        }
        final RESTRequestContext context = new RESTRequestContext(session, userid, sessid, resp.isAdmin(), uroles, getServletContext(), request, response, type);

        try
        {
            final long tick = System.currentTimeMillis();

            final long time = System.nanoTime();

            service.acquire();

            final JSONObject result = service.execute(context, object);

            final long fast = System.nanoTime() - time;

            final long done = System.currentTimeMillis() - tick;

            if (done < 1)
            {
                logger.info("calling service " + name + " took " + fast + " nano's");
            }
            else
            {
                logger.info("calling service " + name + " took " + done + " ms's");
            }
            if (false == context.isClosed())
            {
                if (irpc)
                {
                    writeJSON(response, new JSONObject("result", result));
                }
                else
                {
                    writeJSON(response, result);
                }
            }
        }
        catch (Throwable e)
        {
            final String uuid = UUID.uuid();

            logger.error("calling service " + name + " ERROR UUID=" + uuid, e);

            if (false == context.isClosed())
            {
                final JSONObject output = new JSONObject("error", "A severe error occured with UUID=" + uuid + " , Please contact support.");

                writeJSON(response, output);
            }
        }
    }

    protected boolean isCommandInBody()
    {
        return false;
    }

    protected JSONObject parseJSON(final HttpServletRequest request, final HttpMethod type)
    {
        if (isMethodJSON(type))
        {
            JSONObject object = null;

            final int leng = request.getContentLength();

            if (leng > 0)
            {
                final JSONParser parser = new JSONParser();

                try
                {
                    final Object parsed = parser.parse(request.getReader());

                    if (parsed instanceof JSONObject)
                    {
                        object = ((JSONObject) parsed);
                    }
                }
                catch (JSONParserException e)
                {
                    logger.error("JSONParserException", e);
                }
                catch (IOException e)
                {
                    logger.error("IOException", e);
                }
            }
            if (((null == object) || (leng == 0)))
            {
                logger.error("empty body on " + type.name());

                object = new JSONObject();
            }
            return object;
        }
        return new JSONObject();
    }

    private boolean isMethodJSON(final HttpMethod type)
    {
        if (type == HttpMethod.GET)
        {
            return false;
        }
        if (type == HttpMethod.POST)
        {
            return true;
        }
        if (type == HttpMethod.PUT)
        {
            return true;
        }
        if (type == HttpMethod.PATCH)
        {
            return true;
        }
        if (type == HttpMethod.DELETE)
        {
            return false;
        }
        return false;
    }

    protected void writeJSON(final HttpServletResponse response, final JSONObject output) throws IOException
    {
        doNoCache(response);

        response.setStatus(HttpServletResponse.SC_OK);

        response.setContentType(CONTENT_TYPE_APPLICATION_JSON);

        response.getWriter().flush();

        final NoSyncBufferedWriter buff = new NoSyncBufferedWriter(response.getWriter(), 1024);

        output.writeJSONString(buff, true);

        buff.flush();
    }

    protected final IRESTContext getRESTContext()
    {
        return RESTContextInstance.getRESTContextInstance();
    }

    @Override
    public void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        if ("PATCH".equalsIgnoreCase(request.getMethod()))
        {
            doPatch(request, response);

            return;
        }
        super.service(request, response);
    }
}