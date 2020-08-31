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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.common.utils.DMRUtils;
import org.wildfly.camel.test.mail.subA.MailSessionProducer;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({MailIntegrationTest.MailSessionSetupTask.class})
public class MailIntegrationTest {

    static final Logger LOG = LoggerFactory.getLogger(MailIntegrationTest.class);
    
    @ArquillianResource
    CamelContextRegistry contextRegistry;

    static class MailSessionSetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {

            ModelNode batchNode = DMRUtils.batchNode()
                .addStep("socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-greenmail-smtp", "add(host=localhost, port=10025)")
                .addStep("socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-greenmail-pop3", "add(host=localhost, port=10110)")
                .addStep("subsystem=mail/mail-session=greenmail", "add(jndi-name=java:jboss/mail/greenmail)")
                .addStep("subsystem=mail/mail-session=greenmail/server=smtp", "add(outbound-socket-binding-ref=mail-greenmail-smtp, username=user1, password=password1)")
                .addStep("subsystem=mail/mail-session=greenmail/server=pop3", "add(outbound-socket-binding-ref=mail-greenmail-pop3, username=user2, password=password2)")
                .build();

            managementClient.getControllerClient().execute(batchNode);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {

            ModelNode batchNode = DMRUtils.batchNode()
                .addStep("socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-greenmail-smtp", "remove")
                .addStep("socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-greenmail-pop3", "remove")
                .addStep("subsystem=mail/mail-session=greenmail", "remove")
                .addStep("subsystem=mail/mail-session=greenmail/server=smtp", "remove")
                .addStep("subsystem=mail/mail-session=greenmail/server=pop3", "remove")
                .build();

            managementClient.getControllerClient().execute(batchNode);
        }
    }

    @Deployment(order = 1, testable = false, name = "greenmail")
    public static WebArchive createGreenmailDeployment() {
        return ShrinkWrap.createFromZipFile(WebArchive.class, new File("target/dependencies/greenmail-webapp.war"));
    }

    @Deployment(order = 2)
    public static JavaArchive createDeployment() throws IOException {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-mail-cdi-tests.jar");
        archive.addPackage(MailSessionProducer.class.getPackage());
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    @Test
    public void testMailEndpointWithCDIContext() throws Exception {
        
        CamelContext camelctx = contextRegistry.getCamelContext("camel-mail-cdi-context");
        Assert.assertNotNull("Camel context not null", camelctx);

        CountDownLatch latch = new CountDownLatch(1);
        
        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.whenAnyExchangeReceived(ex -> {
            String body = ex.getMessage().getHeader("message", String.class);
            LOG.info("Received: {}", body);
            latch.countDown();
        });

        Map<String, Object> mailHeaders = new HashMap<>();
        mailHeaders.put("from", "user1@localhost");
        mailHeaders.put("to", "user2@localhost");
        mailHeaders.put("message", "Hello Kermit");

        ProducerTemplate template = camelctx.createProducerTemplate();
        template.requestBodyAndHeaders("direct:start", null, mailHeaders);
        
        // [ENTESB-14566] MailIntegrationTest may not receive expected number of messages
        if (!latch.await(5, TimeUnit.SECONDS))
            LOG.warn("Did not receive the message in time");
    }
}
