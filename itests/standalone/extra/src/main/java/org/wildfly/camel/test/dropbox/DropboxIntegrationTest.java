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
package org.wildfly.camel.test.dropbox;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class DropboxIntegrationTest {

    private static final String DROPBOX_ACCESS_TOKEN = System.getenv("DROPBOX_ACCESS_TOKEN");

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-dropbox-tests.jar");
    }

    @Test
    public void testDropboxProducer() throws Exception {
        Assume.assumeNotNull("DROPBOX_ACCESS_TOKEN is null", DROPBOX_ACCESS_TOKEN);

        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("dropbox://get?accessToken=%s&clientIdentifier=CamelTesting&remotePath=/hello.txt", DROPBOX_ACCESS_TOKEN);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            byte[] result = template.requestBody("direct:start", null, byte[].class);
            Assert.assertEquals("Hello Kermit\n", new String(result));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testDropboxConsumer() throws Exception {
        Assume.assumeNotNull("DROPBOX_ACCESS_TOKEN is null", DROPBOX_ACCESS_TOKEN);

        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("dropbox://get?accessToken=%s&clientIdentifier=CamelTesting&remotePath=/hello.txt", DROPBOX_ACCESS_TOKEN)
                .to("mock:result");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived("Hello Kermit\n".getBytes());

        camelctx.start();
        try {
            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }
}
