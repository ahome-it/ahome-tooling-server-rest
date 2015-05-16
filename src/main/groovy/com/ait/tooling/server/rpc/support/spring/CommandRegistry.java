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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.ait.tooling.common.api.java.util.StringOps;
import com.ait.tooling.server.rpc.IJSONCommand;

/**
 * CommandRegistry - Registry of all IJSONCommand services found in the application.
 */
public class CommandRegistry implements ICommandRegistry, BeanFactoryAware
{
    private static final Logger                       logger     = Logger.getLogger(CommandRegistry.class);

    private final LinkedHashMap<String, IJSONCommand> m_commands = new LinkedHashMap<String, IJSONCommand>();

    public CommandRegistry()
    {
    }

    protected void addCommand(final IJSONCommand command)
    {
        if (null != command)
        {
            final String name = StringOps.requireTrimOrNull(command.getName(), "CommandRegistry.addCommand(name: " + command.getName() + ") blank or null");

            if (null == m_commands.get(name))
            {
                m_commands.put(name, command);

                logger.info("CommandRegistry.addCommand(" + name + ") Registered");
            }
            else
            {
                logger.error("CommandRegistry.addCommand(" + name + ") Duplicate ignored");
            }
        }
    }

    @Override
    public IJSONCommand getCommand(final String name)
    {
        return m_commands.get(StringOps.requireTrimOrNull(name, "CommandRegistry.getCommand(" + name + ") blank or null"));
    }

    @Override
    public List<String> getCommandNames()
    {
        return Collections.unmodifiableList(new ArrayList<String>(m_commands.keySet()));
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
    public void close()
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
