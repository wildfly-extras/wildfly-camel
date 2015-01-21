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

package org.wildfly.camel.test.ognl;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class OgnlIntegrationTest {

    @Deployment
    public static WebArchive createdeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "camel-test.war");
        archive.addAsResource("ognl/test-ognl-expression.txt", "test-ognl-expression.txt");
        return archive;
    }

    @Test
    public void testOgnlExpression() throws Exception {
        CamelContext camelContext = new DefaultCamelContext();

        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .choice()
                        .when()
                            .ognl("request.body.name == 'Kermit'").transform(simple("Hello ${body.name}"))
                        .otherwise()
                            .to("mock:dlq");
            }
        });

        camelContext.start();

        Person person = new Person();
        person.setName("Kermit");

        ProducerTemplate producer = camelContext.createProducerTemplate();
        String result = producer.requestBody("direct:start", person, String.class);

        Assert.assertEquals("Hello Kermit", result);

        camelContext.stop();
    }

    @Test
    public void testOgnlExpressionFromFile() throws Exception {
        CamelContext camelContext = new DefaultCamelContext();

        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .choice()
                        .when()
                            .ognl("resource:classpath:test-ognl-expression.txt").transform(simple("Hello ${body.name}"))
                         .otherwise()
                            .to("mock:dlq");
            }
        });

        camelContext.start();

        Person person = new Person();
        person.setName("Kermit");

        ProducerTemplate producer = camelContext.createProducerTemplate();
        String result = producer.requestBody("direct:start", person, String.class);

        Assert.assertEquals("Hello Kermit", result);

        camelContext.stop();
    }

    public static final class Person {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }
}
