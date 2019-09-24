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
package org.wildfly.camel.test.zendesk;

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
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.camel.test.zendesk.subA.FakeZendeskAPIServlet;
import org.wildfly.extension.camel.CamelAware;
import org.zendesk.client.v2.model.Ticket;

@CamelAware
@RunWith(Arquillian.class)
public class ZendeskIntegrationTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "camel-zendesk-tests.war")
            .addAsResource("zendesk/tickets.json", "tickets.json")
            .addClasses(FakeZendeskAPIServlet.class, TestUtils.class);
    }

    @Test
    public void testZendeskProducer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct://start")
                .to("zendesk://getTickets?serverUrl=http://localhost:8080/camel-zendesk-tests/fake-api");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            Iterable<Ticket> result = template.requestBody("direct:start", null, Iterable.class);

            Assert.assertNotNull("Expected getTickets result to not be null", result);

            Ticket ticket = result.iterator().next();
            Assert.assertEquals(new Long(1), ticket.getId());
            Assert.assertEquals("Hello Kermit", ticket.getSubject());
        } finally {
            camelctx.close();
        }
    }

}
