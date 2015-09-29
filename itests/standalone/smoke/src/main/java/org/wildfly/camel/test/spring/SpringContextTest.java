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

import java.net.URL;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.SpringCamelContextFactory;

/**
 * Deploys a module/bundle that contains a spring context definition.
 *
 * The tests then build a route through the {@link SpringCamelContextFactory} API and perform a simple invokation.
 * This verifies spring context creation from a deployment.
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Apr-2013
 */
@CamelAware
@RunWith(Arquillian.class)
public class SpringContextTest {

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-spring-tests");
        archive.addAsResource("spring/transform1-camel-context.xml", "some-other-name.xml");
        return archive;
    }

    @Test
    public void testSpringContextFromURL() throws Exception {
        URL resourceUrl = getClass().getResource("/some-other-name.xml");
        CamelContext camelctx = SpringCamelContextFactory.createSingleCamelContext(resourceUrl, null);
        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            camelctx.stop();
        }
    }

}
