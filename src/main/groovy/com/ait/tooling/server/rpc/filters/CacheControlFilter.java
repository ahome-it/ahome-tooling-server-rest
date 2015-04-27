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

package com.ait.tooling.server.rpc.filters;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ait.tooling.common.api.java.util.StringOps;
import com.ait.tooling.server.core.filter.AbstractHTTPFilter;

public class CacheControlFilter extends AbstractHTTPFilter
{
    private String m_no_cache_regex = null;

    private String m_do_cache_regex = null;

    @Override
    public void doFilter(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException
    {
        final String url = StringOps.toTrimOrNull(request.getRequestURI());

        if (null != url)
        {
            final String no_cache_regex = StringOps.toTrimOrNull(getNoCacheRegex());

            final String do_cache_regex = StringOps.toTrimOrNull(getDoCacheRegex());

            if ((null != no_cache_regex) && (url.matches(no_cache_regex)))
            {
                doNoCache(request, response);
            }
            else if ((null != do_cache_regex) && (url.matches(do_cache_regex)))
            {
                doFarFuture(request, response);
            }
            else
            {
                doCacheHeaders(url, request, response);
            }
        }
        chain.doFilter(request, response);
    }

    protected String getNoCacheRegex()
    {
        return m_no_cache_regex;
    }

    protected String getDoCacheRegex()
    {
        return m_do_cache_regex;
    }

    protected void doCacheHeaders(String url, final HttpServletRequest request, final HttpServletResponse response)
    {
        url = StringOps.toTrimOrNull(url);

        if (null != url)
        {
            if (url.endsWith(".rpc"))
            {
                doNoCache(request, response);
            }
            else if (url.indexOf(".nocache.") > 0)
            {
                doNoCache(request, response);
            }
            else if (url.indexOf(".cache.") > 0)
            {
                doFarFuture(request, response);
            }
            else if (url.endsWith(".js"))
            {
                doWeekFuture(request, response);
            }
            else if (url.endsWith(".jpg"))
            {
                doWeekFuture(request, response);
            }
            else if (url.endsWith(".png"))
            {
                doWeekFuture(request, response);
            }
            else if (url.endsWith(".gif"))
            {
                doWeekFuture(request, response);
            }
            else if (url.endsWith(".jsp"))
            {
                doNoCache(request, response);
            }
            else if (url.endsWith(".css"))
            {
                doWeekFuture(request, response);
            }
            else if (url.endsWith(".swf"))
            {
                doWeekFuture(request, response);
            }
            else if (url.endsWith(".html"))
            {
                // let it check every request
            }
            else if (url.endsWith(".htm"))
            {
                // let it check every request
            }
            else
            {
                doNoCache(request, response);
            }
        }
    }

    protected void doNoCache(final HttpServletRequest request, final HttpServletResponse response)
    {
        final long time = System.currentTimeMillis();

        response.setDateHeader(DATE_HEADER, time);

        response.setDateHeader(EXPIRES_HEADER, time - YEAR_IN_SECONDS);

        response.setHeader(PRAGMA_HEADER, "no-cache");

        response.setHeader(CACHE_CONTROL_HEADER, "no-cache, no-store, must-revalidate");
    }

    protected void doFarFuture(final HttpServletRequest request, final HttpServletResponse response)
    {
        response.setHeader(CACHE_CONTROL_HEADER, "max-age=" + YEAR_IN_SECONDS);

        response.setDateHeader(EXPIRES_HEADER, System.currentTimeMillis() + YEAR_IN_MILLISECONDS);
    }

    protected void doWeekFuture(final HttpServletRequest request, final HttpServletResponse response)
    {
        response.setHeader(CACHE_CONTROL_HEADER, "max-age=" + WEEK_IN_SECONDS);

        response.setDateHeader(EXPIRES_HEADER, System.currentTimeMillis() + WEEK_IN_MILLISECONDS);
    }

    @Override
    public void init(final FilterConfig config) throws ServletException
    {
        m_no_cache_regex = StringOps.toTrimOrNull(config.getInitParameter("no-cache-regex"));

        m_do_cache_regex = StringOps.toTrimOrNull(config.getInitParameter("do-cache-regex"));
    }
}
