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

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployer;
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
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.DMRUtils;
import org.wildfly.camel.test.mail.subA.MailSessionProducer;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@ServerSetup({ MailIntegrationCDITest.MailSessionSetupTask.class })
@RunWith(Arquillian.class)
public class MailIntegrationCDITest {

    private static final String GREENMAIL_WAR = "greenmail.war";

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @ArquillianResource
    Deployer deployer;

    static class MailSessionSetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {

            ModelNode batchNode = DMRUtils.batchNode()
                .addStep("socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-greenmail-smtp", "add(host=localhost, port=10025)")
                .addStep("socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-greenmail-pop3", "add(host=localhost, port=10110)")
                .addStep("subsystem=mail/mail-session=greenmail", "add(jndi-name=java:jboss/mail/greenmail)")
                .addStep("subsystem=mail/mail-session=greenmail/server=smtp", "add(outbound-socket-binding-ref=mail-greenmail-smtp, username=user1, password=password)")
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

    @Deployment
    public static JavaArchive createDeployment() throws IOException {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-mail-cdi-tests.jar");
        archive.addPackage(MailSessionProducer.class.getPackage());
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    @Deployment(managed = false, testable = false, name = GREENMAIL_WAR)
    public static WebArchive createGreenmailDeployment() {
        File mailDependencies = Maven.configureResolverViaPlugin().
            resolve("com.icegreen:greenmail-webapp:war:1.4.0").
            withoutTransitivity().
            asSingleFile();
        return ShrinkWrap.createFromZipFile(WebArchive.class, mailDependencies);
    }

    @Test
    public void testMailEndpointWithCDIContext() throws Exception {
        try {
            deployer.deploy(GREENMAIL_WAR);

            CamelContext camelctx = contextRegistry.getCamelContext("camel-mail-cdi-context");
            Assert.assertNotNull("Camel context not null", camelctx);

            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockEndpoint.setExpectedMessageCount(1);

            Map<String, Object> mailHeaders = new HashMap<>();
            mailHeaders.put("from", "user1@localhost");
            mailHeaders.put("to", "user2@localhost");
            mailHeaders.put("message", "Hello Kermit");

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.requestBodyAndHeaders("direct:start", null, mailHeaders);

            mockEndpoint.assertIsSatisfied(5000);
        } finally {
            deployer.undeploy(GREENMAIL_WAR);
        }
    }
}
