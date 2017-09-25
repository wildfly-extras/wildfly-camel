/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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
package org.wildfly.camel.test.jolt;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class JoltIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-jolt-tests.jar")
            .addClass(TestUtils.class)
            .addAsResource("jolt/schema.json", "schema.json")
            .addAsResource("jolt/input.json", "input.json")
            .addAsResource("jolt/output.json", "output.json");
    }

    @Test
    public void testJoltComponent() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("jolt:classpath:/schema.json?inputType=JsonString&outputType=JsonString");
            }
        });

        camelctx.start();
        try {
            String input = TestUtils.getResourceValue(JoltIntegrationTest.class, "/input.json");
            String output = TestUtils.getResourceValue(JoltIntegrationTest.class, "/output.json");

            ProducerTemplate template = camelctx.createProducerTemplate();
            String result = template.requestBody("direct:start", input, String.class);

            Assert.assertEquals(output, result);
        } finally {
            camelctx.stop();
        }
    }
}
