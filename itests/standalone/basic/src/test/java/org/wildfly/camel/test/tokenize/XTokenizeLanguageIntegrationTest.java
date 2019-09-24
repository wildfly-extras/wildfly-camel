/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2019 RedHat
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
package org.wildfly.camel.test.tokenize;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.builder.Namespaces;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class XTokenizeLanguageIntegrationTest {

    private static final String XML = "<?xml version='1.0' encoding='UTF-8'?>"
        + "<c:parent xmlns:c='urn:c'>"
        + "<c:child some_attr='a' anotherAttr='a'>"
        + "</c:child>"
        + "<c:child some_attr='b' anotherAttr='b'>"
        + "</c:child>"
        + "</c:parent>";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-xtokenize-tests.jar");
    }

    @Test
    public void testXTokenize() throws Exception {
        Namespaces ns = new Namespaces("C", "urn:c");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .split().xtokenize("//C:child", ns)
                .to("mock:result");
            }
        });

        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockEndpoint.expectedBodiesReceived("<c:child some_attr='a' anotherAttr='a' xmlns:c=\"urn:c\"></c:child>", "<c:child some_attr='b' anotherAttr='b' xmlns:c=\"urn:c\"></c:child>");

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:start", XML);

            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }
}
