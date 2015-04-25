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

package com.ait.tooling.server.rpc.support.spring;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.ait.tooling.common.api.java.util.StringOps;
import com.ait.tooling.server.rpc.IJSONCommand;

public class CommandRegistry implements ICommandRegistry, BeanFactoryAware
{
    private static final Logger                 logger     = Logger.getLogger(CommandRegistry.class);

    private final HashMap<String, IJSONCommand> m_commands = new HashMap<String, IJSONCommand>();

    protected void addCommand(IJSONCommand command)
    {
        if (null != command)
        {
            String name = StringOps.toTrimOrNull(command.getName());

            if (null != name)
            {
                if (false == m_commands.containsKey(name))
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
    public Collection<String> getCommandNames()
    {
        return Collections.unmodifiableCollection(m_commands.keySet());
    }

    @Override
    public Collection<IJSONCommand> getCommands()
    {
        return Collections.unmodifiableCollection(m_commands.values());
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
        for (String name : getCommandNames())
        {
            final IJSONCommand command = getCommand(name);

            if (null != command)
            {
                try
                {
                    logger.info("CommandRegistry.close(" + name + ")");

                    command.close();
                }
                catch (Exception e)
                {
                    logger.error("CommandRegistry.close(" + name + ") ERROR ", e);
                }
            }
            else
            {
                logger.error("CommandRegistry.close(" + name + ") doesn't exist");
            }
        }
    }
}
