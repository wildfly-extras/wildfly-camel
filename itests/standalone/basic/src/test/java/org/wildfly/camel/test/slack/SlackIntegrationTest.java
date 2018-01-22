/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.slack;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.slack.SlackComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class SlackIntegrationTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-slack-tests");
        return archive;
    }

    @Test
    public void testSlackMessage() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                onException(Exception.class).handled(true).to("mock:errors");
                from("direct:test").to("slack:#general?iconEmoji=:camel:&username=CamelTest");
                from("direct:error").to("slack:#badchannel?iconEmoji=:camel:&username=CamelTest");
            }
        });

        SlackComponent comp = camelctx.getComponent("slack", SlackComponent.class);
        comp.setWebhookUrl("https://hooks.slack.com/services/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");

        MockEndpoint mockErrors = camelctx.getEndpoint("mock:errors", MockEndpoint.class);
        mockErrors.expectedMessageCount(0);

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.sendBody("direct:test", "Hello from Camel!");
            mockErrors.assertIsSatisfied();

            mockErrors.reset();
            mockErrors.expectedMessageCount(1);

            producer.sendBody("direct:error", "Error from Camel!");
            mockErrors.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }

    }
}
