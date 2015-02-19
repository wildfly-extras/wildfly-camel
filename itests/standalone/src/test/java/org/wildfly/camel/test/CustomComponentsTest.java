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

package org.wildfly.camel.test;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ResolveEndpointFailedException;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.smoke.subA.HelloBean;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.camel.CamelContextRegistry;

/**
 * Verify that a deployment with a META-INF/jboss-camel-components.properties file
 * only has the specified components on the classpath.
 */
@RunWith(Arquillian.class)
public class CustomComponentsTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "no-mqtt-tests");
        archive.addAsResource("jboss-camel-components.properties", "META-INF/jboss-camel-components.properties");
        return archive;
    }

    @Test
    public void testMQTTComponentDoesNotLoad() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        try {
            camelctx.getEndpoint("mqtt://dummy");
            Assert.fail("Expected a ResolveEndpointFailedException");
        } catch (ResolveEndpointFailedException e) {
        }
    }

    @Test
    public void testFtpComponentLoads() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        Endpoint endpoint = camelctx.getEndpoint("ftp://localhost/foo");
        Assert.assertNotNull(endpoint);
        Assert.assertEquals(endpoint.getClass().getName(), "org.apache.camel.component.file.remote.FtpEndpoint");
        camelctx.stop();
    }


    @Test
    public void testRssComponentLoads() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        Endpoint endpoint = camelctx.getEndpoint("rss://https://developer.jboss.org/blogs/feeds/posts?splitEntries=true");
        Assert.assertNotNull(endpoint);
        Assert.assertEquals(endpoint.getClass().getName(), "org.apache.camel.component.rss.RssEndpoint");
        camelctx.stop();
    }
}
