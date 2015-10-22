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

package org.wildfly.camel.test.cxf;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.types.Endpoint;
import org.wildfly.camel.test.cxf.ws.subA.EndpointImpl;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
public class CXFEndpointXmlConfigurationTest {

    private static final String SIMPLE_WAR = "simple.war";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "cxf-spring-context-tests");
        archive.addClasses(Endpoint.class);
        archive.addAsResource("cxf/spring/cxfws-epconfig-camel-context.xml");
        return archive;
    }

    @Deployment(name = SIMPLE_WAR, managed = false, testable = false)
    public static Archive<?> getSimpleWar() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, SIMPLE_WAR);
        archive.addClasses(Endpoint.class, EndpointImpl.class);
        return archive;
    }

    @Test
    public void testCxfSpringContext() throws Exception {
        deployer.deploy(SIMPLE_WAR);
        try {
            CamelContext camelctx = contextRegistry.getCamelContext("cxf-epconfig");
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            deployer.undeploy(SIMPLE_WAR);
        }
    }
}
