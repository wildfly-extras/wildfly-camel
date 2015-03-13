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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.support.GenericApplicationContext;
import org.wildfly.camel.test.spring.subB.ScannedComponentSpringRouteBuilder;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
public class SpringRouteBuilderScanTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "spring-routebuilder-tests");
        archive.addAsResource("spring/context-scan-spring-routebuilder-camel-context.xml", "context-scan-spring-routebuilder-camel-context.xml");
        archive.addClasses(ScannedComponentSpringRouteBuilder.class);
        return archive;
    }

    @Test
    public void testSpringRouteBuilderLoads() throws Exception {
        CamelContext camelctx = contextRegistry.getContext("contextScan");
        ProducerTemplate producer = camelctx.createProducerTemplate();
        GenericApplicationContext result = producer.requestBody("direct:start", null, GenericApplicationContext.class);
        Assert.assertNotNull(result);
    }
}
