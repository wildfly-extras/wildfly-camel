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

package org.wildfly.camel.test.switchyard;

import java.io.InputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultClassResolver;
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
import org.wildfly.camel.test.switchyard.subA.JavaDSL;
import org.wildfly.camel.test.switchyard.subA.JavaDSLBuilder;
import org.wildfly.extension.camel.CamelContextRegistry;

/**
 * Verify that a deployment with a META-INF/switchyard.xml file
 * does not have the CamelContexts enhanced by wildfly-camel
 */
@RunWith(Arquillian.class)
public class CamelContextHooksTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "switchyard-camelctx-tests.jar");
        archive.addClasses(JavaDSL.class, JavaDSLBuilder.class);
        archive.addAsManifestResource("switchyard/switchyard.xml", "switchyard.xml");
        archive.addAsManifestResource("switchyard/route.xml", "route.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.wildfly.extension.camel,org.apache.camel.core");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testCamelContextNotEnhanced () throws Exception {

        // Verify that the CamelContext works with the explicit module deps from above
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").transform(body().prepend("Hello "));
            }
        });

        camelctx.start();
        try {

            // These properties would be set by the wildfly-camel integration
            Assert.assertNull("Null ApplicationContextClassLoader", camelctx.getApplicationContextClassLoader());
            Assert.assertTrue("Has DefaultClassResolver", camelctx.getClassResolver() instanceof DefaultClassResolver);
            Assert.assertNull("Not in CamelContextRegistry", contextRegistry.getCamelContext(camelctx.getName()));

            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            camelctx.stop();
        }
    }
}
