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

package com.ait.tooling.server.rpc.support.spring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.ait.tooling.common.api.java.util.StringOps;
import com.ait.tooling.server.rpc.IJSONCommand;

/**
 * CommandRegistry - Registry of all IJSONCommand services found in the application.
 */
@ManagedResource
public class CommandRegistry implements ICommandRegistry, BeanFactoryAware
{
    private static final long                         serialVersionUID = -8612763973722004339L;

    private static final Logger                       logger           = Logger.getLogger(CommandRegistry.class);

    private final LinkedHashMap<String, IJSONCommand> m_commands       = new LinkedHashMap<String, IJSONCommand>();

    private final LinkedHashMap<String, IJSONCommand> m_bindings       = new LinkedHashMap<String, IJSONCommand>();

    public CommandRegistry()
    {
    }

    protected void addCommand(final IJSONCommand command)
    {
        if (null != command)
        {
            String name = StringOps.toTrimOrNull(command.getName());

            if (null != name)
            {
                if (null == m_commands.get(name))
                {
                    m_commands.put(name, command);

                    logger.info("CommandRegistry.addCommand(" + name + ") Command Registered");
                }
                else
                {
                    logger.error("CommandRegistry.addCommand(" + name + ") Duplicate command ignored");
                }
            }
            else
            {
                logger.error("CommandRegistry.addCommand(" + command.getClass().getSimpleName() + ") has null or empty name.");
            }
            String bind = StringOps.toTrimOrNull(command.getRequestBinding());

            if (null != bind)
            {
                if (null == m_bindings.get(bind))
                {
                    m_bindings.put(bind, command);

                    logger.info("CommandRegistry.addCommand(" + bind + ") Binding Registered");
                }
                else
                {
                    logger.error("CommandRegistry.addCommand(" + bind + ") Duplicate binding ignored");
                }
            }
            if ((null != name) && (null == bind))
            {
                bind = "/" + name;

                if (null == m_bindings.get(bind))
                {
                    m_bindings.put(bind, command);

                    logger.info("CommandRegistry.addCommand(" + bind + ") Binding Registered");
                }
                else
                {
                    logger.error("CommandRegistry.addCommand(" + bind + ") Duplicate binding ignored");
                }
            }
        }
        else
        {
            logger.error("CommandRegistry.addCommand(null)");
        }
    }

    @Override
    public IJSONCommand getCommand(String name)
    {
        name = StringOps.toTrimOrNull(name);

        if (null != name)
        {
            return m_commands.get(name);
        }
        return null;
    }

    @Override
    public IJSONCommand getBinding(String bind)
    {
        bind = StringOps.toTrimOrNull(bind);

        if (null != bind)
        {
            if (false == bind.startsWith("/"))
            {
                bind = "/" + bind;
            }
            return m_bindings.get(bind);
        }
        return null;
    }

    @Override
    @ManagedAttribute(description = "Get IJSONCommand names.")
    public List<String> getCommandNames()
    {
        return Collections.unmodifiableList(new ArrayList<String>(m_commands.keySet()));
    }
    
    @Override
    @ManagedAttribute(description = "Get IJSONCommand RequestBindings.")
    public List<String> getRequestBindings()
    {
        return Collections.unmodifiableList(new ArrayList<String>(m_bindings.keySet()));
    }


    @Override
    public List<IJSONCommand> getCommands()
    {
        return Collections.unmodifiableList(new ArrayList<IJSONCommand>(m_commands.values()));
    }

    @Override
    public void setBeanFactory(final BeanFactory factory) throws BeansException
    {
        if (factory instanceof DefaultListableBeanFactory)
        {
            for (IJSONCommand command : ((DefaultListableBeanFactory) factory).getBeansOfType(IJSONCommand.class).values())
            {
                addCommand(command);
            }
        }
    }

    @Override
    public void close() throws IOException
    {
        for (IJSONCommand command : getCommands())
        {
            if (null != command)
            {
                try
                {
                    logger.info("CommandRegistry.close(" + command.getName() + ")");

                    command.close();
                }
                catch (Exception e)
                {
                    logger.error("CommandRegistry.close(" + command.getName() + ") ERROR ", e);
                }
            }
        }
    }
}
