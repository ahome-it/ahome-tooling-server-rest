/*
 * Copyright (c) 2017 Ahome' Innovation Technologies. All rights reserved.
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

package com.ait.tooling.server.rest.support.spring;

import java.util.List;

import com.ait.tooling.server.core.support.spring.IServerContext;
import com.ait.tooling.server.rest.IRESTService;

public interface IRESTContext extends IServerContext
{
    public IRESTService getService(String name);

    public IRESTService getBinding(String bind);

    public String fixRequestBinding(String bind);

    public IServiceRegistry getServiceRegistry();
    
    public List<IRESTService> getServices();
}
