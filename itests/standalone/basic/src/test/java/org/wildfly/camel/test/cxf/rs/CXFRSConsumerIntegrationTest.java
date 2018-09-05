/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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
package org.wildfly.camel.test.cxf.rs;

import java.io.InputStream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.types.GreetingService;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@org.junit.Ignore("https://github.com/wildfly-extras/wildfly-camel/issues/2601")
@CamelAware
@RunWith(Arquillian.class)
public class CXFRSConsumerIntegrationTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "cxfrs-consumer-tests");
        archive.addClasses(GreetingService.class);
        archive.addAsResource("cxf/spring/cxfrs-consumer-camel-context.xml", "cxfrs-consumer-camel-context.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.jboss.resteasy.resteasy-jaxrs");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testCXFConsumer() throws Exception {

        CamelContext camelctx = contextRegistry.getCamelContext("cxfrs-undertow");
        Assert.assertNotNull("Expected cxfrs-undertow to not be null", camelctx);
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            // The JAXRS Client API needs to see resteasy
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            Client client = ClientBuilder.newClient();
            Object result = client.target("http://localhost:8080/cxfrestful/greet/hello/Kermit").request(MediaType.APPLICATION_JSON).get(String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

}
