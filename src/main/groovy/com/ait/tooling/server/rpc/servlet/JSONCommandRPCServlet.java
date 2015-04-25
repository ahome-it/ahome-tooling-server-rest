/*
   Copyright (c) 2014,2015 Ahome' Innovation Technologies. All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ait.tooling.server.rpc.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import com.ait.tooling.common.api.java.util.StringOps;
import com.ait.tooling.common.api.java.util.UUID;
import com.ait.tooling.json.JSONObject;
import com.ait.tooling.json.parser.JSONParser;
import com.ait.tooling.json.parser.JSONParserException;
import com.ait.tooling.server.core.security.AuthorizationResult;
import com.ait.tooling.server.core.servlet.HTTPServletBase;
import com.ait.tooling.server.rpc.IJSONCommand;
import com.ait.tooling.server.rpc.JSONRequestContext;
import com.ait.tooling.server.rpc.support.spring.IRPCContext;
import com.ait.tooling.server.rpc.support.spring.RPCContextInstance;

@SuppressWarnings("serial")
public class JSONCommandRPCServlet extends HTTPServletBase
{
    private static final Logger logger = Logger.getLogger(JSONCommandRPCServlet.class);

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

        MDC.put("user", ((userid == null) ? UNKNOWN_USER : userid) + "-" + ((sessid == null) ? NULL_SESSION : sessid));

        JSONObject object = parseJSON(request);

        if (null == object)
        {
            logger.error("passed body is not a JSONObject");

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            return;
        }
        if (false == object.isDefined("command"))
        {
            logger.error("no command key found");

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            return;
        }
        final String name = StringOps.toTrimOrNull(object.getAsString("command"));

        if (null == name)
        {
            logger.error("empty command key found");

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            return;
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
        final AuthorizationResult resp = isAuthorized(command, getUserPrincipalsFromRequest(request, getServerContext().getPrincipalsKeys()));

        if (false == resp.isAuthorized())
        {
            logger.error("service authorization failed " + name + " for user " + userid + " code " + resp.getText());

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            return;
        }
        final JSONRequestContext context = new JSONRequestContext(userid, sessid, resp.isAdmin(), getServletContext(), request, response);

        try
        {
            final long tick = System.currentTimeMillis();

            final JSONObject result = command.execute(context, object);

            long done = System.currentTimeMillis();

            logger.info("calling command " + name + " took " + (done - tick) + "ms");

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

        output.writeJSONString(response.getWriter());

        response.getWriter().flush();
    }

    protected boolean isRunning()
    {
        return getRPCContext().getServerManager().isRunning();
    }
    
    protected final IRPCContext getRPCContext()
    {
        return RPCContextInstance.get();
    }
}