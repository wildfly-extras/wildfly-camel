/*
 * #%L
 * Wildfly Camel :: Testsuite
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

package org.wildfly.camel.test.jmx;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.monitor.MonitorNotification;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.DMRUtils;
import org.wildfly.camel.utils.ObjectNameFactory;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

/**
 * Deploys a test which monitors an JMX attrbute of a route.
 *
 * @author thomas.diesler@jboss.com
 * @since 03-Jun-2013
 */
@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({JMXIntegrationTest.SystemContextSetupTask.class})
public class JMXIntegrationTest  {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    static class SystemContextSetupTask implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, String containerId) throws Exception {
            String contextXml = "" +
                    "\t\t     <route>\n" +
                    "\t\t       <from uri=\"direct:start\"/>\n" +
                    "\t\t       <transform>\n" +
                    "\t\t         <simple>Hello #{body}</simple>\n" +
                    "\t\t       </transform>\n" +
                    "\t\t     </route>\n" +
                    "";

            // Add a system context
            ModelNode contextOpAdd = DMRUtils.createOpNode("subsystem=camel/context=jmx-context-1/", "add");
            contextOpAdd.get("value").set(contextXml);
            managementClient.getControllerClient().execute(DMRUtils.createCompositeNode(new ModelNode[]{contextOpAdd}));
        }

        @Override
        public void tearDown(final ManagementClient managementClient, String containerId) throws Exception {
            // Removes a system context
            ModelNode contextOpAdd = DMRUtils.createOpNode("subsystem=camel/context=jmx-context-1/", "remove");
            managementClient.getControllerClient().execute(DMRUtils.createCompositeNode(new ModelNode[]{contextOpAdd}));
        }
    }

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "jmx-integration-tests");
        return archive;
    }

    @Test
    public void testMonitorMBeanAttribute() throws Exception {
        CamelContext context = contextRegistry.getCamelContext("jmx-context-1");
        Assert.assertNotNull("jmx-context-1 not null", context);
        final String routeName = context.getRoutes().get(0).getId();

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName onameAll = ObjectNameFactory.create("org.apache.camel:*");
        Set<ObjectInstance> mbeans = server.queryMBeans(onameAll, null);
        System.out.println(">>>>>>>>> MBeans: " + mbeans.size());
        mbeans.forEach(mb -> System.out.println(mb.getObjectName()));

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jmx:platform?format=raw&objectDomain=org.apache.camel&key.context=jmx-context-1&key.type=routes&key.name=\"" + routeName + "\"" +
                "&monitorType=counter&observedAttribute=ExchangesTotal&granularityPeriod=500").
                to("direct:end");
            }
        });

        camelctx.start();
        try {
            ConsumerTemplate consumer = camelctx.createConsumerTemplate();
            MonitorNotification notifcation = consumer.receiveBody("direct:end", MonitorNotification.class);
            Assert.assertEquals("ExchangesTotal", notifcation.getObservedAttribute());
        } finally {
            camelctx.stop();
        }
    }
}
