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

package org.wildfly.camel.test.mail;

import java.io.File;

import javax.mail.Message;

import org.apache.camel.CamelContext;
import org.apache.camel.PollingConsumer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mail.MailMessage;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.mock_javamail.Mailbox;

@RunWith(Arquillian.class)
public class MailIntegrationTest {

    @Deployment
    public static WebArchive createdeployment() {
        File[] mailDependencies = Maven.configureResolverViaPlugin().
                resolve("org.jvnet.mock-javamail:mock-javamail").
                withTransitivity().
                asFile();

        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "camel-test.war");
        archive.addAsLibraries(mailDependencies);
        return archive;
    }

    @Before
    public void before() {
        Mailbox.clearAll();
    }
    
    @Test
    public void testSendEmail() throws Exception {

        // [FIXME #290] Usage of camel-mail depends on TCCL
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("smtp://localhost?from=bob@localhost&to=kermit@localhost&subject=Greetings");

                from("pop3://kermit@localhost?consumer.delay=1000")
                .to("direct:email");
            }
        });
        camelctx.start();

        PollingConsumer pollingConsumer = camelctx.getEndpoint("direct:email").createPollingConsumer();
        pollingConsumer.start();

        ProducerTemplate producer = camelctx.createProducerTemplate();
        producer.sendBody("direct:start", "Hello Kermit");

        MailMessage mailMessage = pollingConsumer.receive().getIn().getBody(MailMessage.class);
        Message message = mailMessage.getMessage();

        Assert.assertEquals("bob@localhost", message.getFrom()[0].toString());
        Assert.assertEquals("Greetings", message.getSubject());
        Assert.assertEquals("Hello Kermit", message.getContent());

        camelctx.stop();
    }
}
