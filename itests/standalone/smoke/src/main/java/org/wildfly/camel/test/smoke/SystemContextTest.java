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

package org.wildfly.camel.test.smoke;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
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
import org.wildfly.camel.test.common.DMRUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;


/**
 * Performs an invocation on a preconfigured system context.
 * This verifies that the {@link CamelContext} is created/registered at subsystem boot time.
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Apr-2013
 */
@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({SystemContextTest.SystemContextSetupTask.class})
public class SystemContextTest {

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
            ModelNode contextOpAdd = DMRUtils.createOpNode("subsystem=camel/context=system-context-1/", "add");
            contextOpAdd.get("value").set(contextXml);
            managementClient.getControllerClient().execute(DMRUtils.createCompositeNode(new ModelNode[]{contextOpAdd}));
        }

        @Override
        public void tearDown(final ManagementClient managementClient, String containerId) throws Exception {
            // Removes a system context
            ModelNode contextOpAdd = DMRUtils.createOpNode("subsystem=camel/context=system-context-1/", "remove");
            managementClient.getControllerClient().execute(DMRUtils.createCompositeNode(new ModelNode[]{contextOpAdd}));
        }
    }

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-system-tests");
        return archive;
    }

    @Test
    public void testSystemTransformFromModule() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("system-context-1");
        Assert.assertNotNull("Camel context System-context-1 was null", camelctx);
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        ProducerTemplate producer = camelctx.createProducerTemplate();
        String result = producer.requestBody("direct:start", "Kermit", String.class);
        Assert.assertEquals("Hello Kermit", result);
    }
}
