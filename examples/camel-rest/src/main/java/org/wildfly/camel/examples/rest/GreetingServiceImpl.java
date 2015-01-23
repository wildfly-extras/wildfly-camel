/*
 * #%L
 * Wildfly Camel :: Example :: Camel REST
 * %%
 * Copyright (C) 2013 - 2014 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wildfly.camel.examples.rest;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.component.bean.ProxyHelper;

public class GreetingServiceImpl implements GreetingService {

    @Inject
    @ContextName("system-context-1")
    private CamelContext context;

    private GreetingService greetingServiceProxy;

    @PostConstruct
    public void initServiceProxy() throws Exception {
        Endpoint endpoint = context.getEndpoint("direct:start");
        greetingServiceProxy = ProxyHelper.createProxy(endpoint, GreetingService.class);
    }

    public Response sayHello(String name) {
        return greetingServiceProxy.sayHello(name);
    }
}
