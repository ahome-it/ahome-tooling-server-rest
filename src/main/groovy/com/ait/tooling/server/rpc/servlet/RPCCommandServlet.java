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

package com.ait.tooling.server.rpc.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

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
import com.ait.tooling.server.core.support.CoreGroovySupport;
import com.ait.tooling.server.rpc.IJSONCommand;
import com.ait.tooling.server.rpc.JSONRequestContext;
import com.ait.tooling.server.rpc.support.spring.IRPCContext;
import com.ait.tooling.server.rpc.support.spring.RPCContextInstance;

public class RPCCommandServlet extends HTTPServletBase
{
    private static final long   serialVersionUID = 8890049936686095786L;

    private static final Logger logger           = Logger.getLogger(RPCCommandServlet.class);

    public RPCCommandServlet()
    {
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException
    {
        if (false == isRunning())
        {
            logger.error("server is suspended, refuse command request");

            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

            return;
        }
        final String userid = StringOps.toTrimOrNull(request.getHeader(X_USER_ID_HEADER));

        final String sessid = StringOps.toTrimOrNull(request.getHeader(X_SESSION_ID_HEADER));

        MDC.put("session", ((userid == null) ? "no-userid" : userid) + "-" + ((sessid == null) ? "no-sessid" : sessid));

        JSONObject object = parseJSON(request);

        if (null == object)
        {
            logger.error("passed body is not a JSONObject");

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            return;
        }
        String name = null;

        if (isCommandInBody())
        {
            if (false == object.isDefined("command"))
            {
                logger.error("no command key found");

                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                return;
            }
            name = StringOps.toTrimOrNull(object.getAsString("command"));

            if (null == name)
            {
                logger.error("empty command key found");

                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                return;
            }
        }
        else
        {
            name = StringOps.toTrimOrNull(request.getPathInfo());

            if (null != name)
            {
                int indx = name.lastIndexOf("/");

                if (indx >= 0)
                {
                    name = StringOps.toTrimOrNull(name.substring(indx + 1));
                }
                if (null != name)
                {
                    if (name.endsWith(".rpc"))
                    {
                        name = StringOps.toTrimOrNull(name.substring(0, name.length() - 4));
                    }
                }
            }
            if (null == name)
            {
                logger.error("empty command path found");

                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                return;
            }
        }
        final IJSONCommand command = getRPCContext().getCommand(name);

        if (null == command)
        {
            logger.error("command not found " + name);

            response.setStatus(HttpServletResponse.SC_NOT_FOUND);

            return;
        }
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
        List<String> roles = null;

        if (null != sessid)
        {
            final String domain_name = StringOps.toTrimOrNull(getServletConfig().getInitParameter("session_domain_name"));

            final IServerSessionRepository repository = getServerContext().getServerSessionRepository((domain_name == null) ? "default" : domain_name);

            if (null != repository)
            {
                final IServerSession session = repository.getSession(sessid);

                if ((null != session) && (false == session.isExpired()))
                {
                    roles = session.getRoles();
                }
            }
        }
        if (null == roles)
        {
            roles = new ArrayList<String>(0);
        }
        final AuthorizationResult resp = isAuthorized(command, roles);

        if (false == resp.isAuthorized())
        {
            logger.error("service authorization failed " + name + " for user " + userid + " code " + resp.getText());

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            return;
        }
        final JSONRequestContext context = new JSONRequestContext(userid, sessid, resp.isAdmin(), roles, getServletContext(), request, response);

        try
        {
            final long tick = System.currentTimeMillis();

            final long time = System.nanoTime();

            final JSONObject result = command.execute(context, object);

            final long fast = System.nanoTime() - time;

            final long done = System.currentTimeMillis() - tick;

            if (done < 1)
            {
                logger.info("calling command " + name + " took " + fast + "nano's");
            }
            else
            {
                logger.info("calling command " + name + " took " + done + "ms's");
            }
            final JSONObject output = new JSONObject("result", result);

            writeJSON(response, output);
        }
        catch (Throwable e)
        {
            final String uuid = UUID.uuid();

            logger.error("calling command " + name + " ERROR UUID=" + uuid, e);

            final JSONObject output = new JSONObject("error", "A severe error occured with UUID=" + uuid + " , Please contact support.");

            writeJSON(response, output);
        }
    }

    protected boolean isCommandInBody()
    {
        return true;
    }

    protected JSONObject parseJSON(final HttpServletRequest request)
    {
        JSONObject object = null;

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
        return object;
    }

    protected void writeJSON(final HttpServletResponse response, final JSONObject output) throws IOException
    {
        response.setStatus(HttpServletResponse.SC_OK);

        response.setContentType(CONTENT_TYPE_APPLICATION_JSON);

        response.getWriter().flush();

        final NoSyncBufferedWriter buff = new NoSyncBufferedWriter(response.getWriter(), 1024);

        output.writeJSONString(buff, true);

        buff.flush();
    }

    protected boolean isRunning()
    {
        return CoreGroovySupport.getCoreGroovySupport().getCoreServerManager().isRunning();
    }

    protected final IRPCContext getRPCContext()
    {
        return RPCContextInstance.getRPCContextInstance();
    }
}