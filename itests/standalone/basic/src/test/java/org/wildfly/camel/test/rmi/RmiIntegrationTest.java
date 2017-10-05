/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.rmi;

import java.rmi.registry.LocateRegistry;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.ProxyHelper;
import org.apache.camel.component.rmi.RmiEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.jndi.JndiContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class RmiIntegrationTest {

    private int port;
    
    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-rmi-tests.jar");
        archive.addClasses(AvailablePortFinder.class, ISay.class, SayService.class);
        return archive;
    }

    @Before
    public void before() throws Exception {
        port = AvailablePortFinder.getNextAvailable(37503);
        LocateRegistry.createRegistry(port);
    }
    
    @Test
    public void testPojoRoutes() throws Exception {

        JndiContext jndctx = new JndiContext();
        jndctx.bind("bye", new SayService("Good Bye!"));

        CamelContext camelContext = new DefaultCamelContext(jndctx);
        camelContext.addRoutes(createRouteBuilder(camelContext));

        camelContext.start();
        try {
            Endpoint endpoint = camelContext.getEndpoint("direct:hello");
            ISay proxy = ProxyHelper.createProxy(endpoint, false, ISay.class);
            String rc = proxy.say();
            Assert.assertEquals("Good Bye!", rc);
        } finally {
            camelContext.stop();
        }
    }

    private RouteBuilder createRouteBuilder(final CamelContext context) {
        return new RouteBuilder() {
            public void configure() {
                from("direct:hello").toF("rmi://localhost:%s/bye", port);

                // When exposing an RMI endpoint, the interfaces it exposes must be configured.
                RmiEndpoint bye = (RmiEndpoint)endpoint("rmi://localhost:" + port + "/bye");
                bye.setRemoteInterfaces(ISay.class);
                from(bye).to("bean:bye");
            }
        };
    }
}
