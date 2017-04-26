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

package org.wildfly.camel.test.spring;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelContextRegistry;

/**
 * [#206] Provide multiple Camel config files per deployment
 *
 * https://github.com/wildfly-extras/wildfly-camel/issues/206
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Feb-2015
 */
@RunWith(Arquillian.class)
public class SpringMultipleDescriptorsTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-multiple-tests");
        archive.addAsResource("spring/transform1-camel-context.xml", "transform1-camel-context.xml");
        archive.addAsResource("spring/transform2-camel-context.xml", "somedir/transform2-camel-context.xml");
        archive.addAsResource("spring/transform3-camel-context.xml", "transform3-camel-context.xml");
        return archive;
    }

    @Test
    public void testTransform1() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("transform1");
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        ProducerTemplate producer = camelctx.createProducerTemplate();
        String result = producer.requestBody("direct:start", "Kermit", String.class);
        Assert.assertEquals("Hello Kermit", result);
    }

    @Test
    public void testTransform2() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("transform2");
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        ProducerTemplate producer = camelctx.createProducerTemplate();
        String result = producer.requestBody("direct:start", "Kermit", String.class);
        Assert.assertEquals("Hello2 Kermit", result);
    }

    @Test
    public void testTransform3() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("transform3");
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        ProducerTemplate producer = camelctx.createProducerTemplate();
        String result = producer.requestBody("direct:start", "Kermit", String.class);
        Assert.assertEquals("Hello3 Kermit", result);
    }

    @Test
    public void testTransform4() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("transform4");
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        ProducerTemplate producer = camelctx.createProducerTemplate();
        String result = producer.requestBody("direct:start", "Kermit", String.class);
        Assert.assertEquals("Hello4 Kermit", result);
    }
}
