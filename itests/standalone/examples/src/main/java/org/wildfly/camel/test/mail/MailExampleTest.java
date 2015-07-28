/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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
import java.net.MalformedURLException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.batch.Batch;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.common.HttpRequest;
import org.wildfly.camel.test.common.HttpRequest.HttpResponse;

@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup({ MailExampleTest.MailSessionSetupTask.class })
public class MailExampleTest {

    private static final String GREENMAIL_WAR = "greenmail.war";
    private static final String EXAMPLE_CAMEL_MAIL_WAR = "example-camel-mail.war";
    private static final Logger LOG = LoggerFactory.getLogger(MailSessionSetupTask.class.getPackage().getName());

    @ArquillianResource
    Deployer deployer;

    static class MailSessionSetupTask implements ServerSetupTask {

        public  MailSessionSetupTask() {
            System.setProperty("jboss.dist", System.getProperty("jboss.home"));
        }

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {

            final CommandContext ctx = CLITestUtil.getCommandContext();
            ctx.connectController();

            ctx.getBatchManager().activateNewBatch();
            final Batch batch = ctx.getBatchManager().getActiveBatch();
            batch.add(ctx.toBatchedCommand("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-greenmail-smtp:add(host=localhost, port=10025)"));
            batch.add(ctx.toBatchedCommand("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-greenmail-pop3:add(host=localhost, port=10110)"));
            batch.add(ctx.toBatchedCommand("/subsystem=mail/mail-session=greenmail:add(jndi-name=\"java:jboss/mail/greenmail\")"));
            batch.add(ctx.toBatchedCommand("/subsystem=mail/mail-session=greenmail/server=smtp:add(outbound-socket-binding-ref=mail-greenmail-smtp, username=user1, password=password)"));
            batch.add(ctx.toBatchedCommand("/subsystem=mail/mail-session=greenmail/server=pop3:add(outbound-socket-binding-ref=mail-greenmail-pop3, username=user2, password=password2)"));

            ModelNode request = batch.toRequest();
            batch.clear();
            ctx.getBatchManager().discardActiveBatch();

            ModelNode execute = managementClient.getControllerClient().execute(request);
            LOG.info(execute.toString());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {

            final CommandContext ctx = CLITestUtil.getCommandContext();
            ctx.connectController();

            ctx.getBatchManager().activateNewBatch();
            final Batch batch = ctx.getBatchManager().getActiveBatch();
            batch.add(ctx.toBatchedCommand("/subsystem=mail/mail-session=greenmail/server=smtp:remove"));
            batch.add(ctx.toBatchedCommand("/subsystem=mail/mail-session=greenmail/server=pop3:remove"));
            batch.add(ctx.toBatchedCommand("/subsystem=mail/mail-session=greenmail:remove"));
            batch.add(ctx.toBatchedCommand("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-greenmail-smtp:remove"));
            batch.add(ctx.toBatchedCommand("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=mail-greenmail-pop3:remove"));

            ModelNode request = batch.toRequest();
            batch.clear();
            ctx.getBatchManager().discardActiveBatch();

            ModelNode execute = managementClient.getControllerClient().execute(request);
            LOG.info(execute.toString());
        }
    }

    @Deployment(managed = false, name = EXAMPLE_CAMEL_MAIL_WAR)
    public static WebArchive createDeployment() {
        return ShrinkWrap.createFromZipFile(WebArchive.class, new File("target/examples/example-camel-mail.war"));
    }

    @Deployment(managed = false, testable = false, name = GREENMAIL_WAR)
    public static WebArchive createGreenmailDeployment() {
        return ShrinkWrap.createFromZipFile(WebArchive.class, new File("target/examples/greenmail-webapp.war"));
    }

    @Test
    public void sendEmailTest() throws Exception {
        try {
            deployer.deploy(GREENMAIL_WAR);
            deployer.deploy(EXAMPLE_CAMEL_MAIL_WAR);

            StringBuilder endpointURL = new StringBuilder("from=user1@localhost");
            endpointURL.append("&to=user2@localhost")
                    .append("&subject=Greetings")
                    .append("&message=Hello");

            HttpResponse result = HttpRequest.post(getEndpointAddress("/example-camel-mail/send"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .content(endpointURL.toString())
                    .getResponse();

            String responseBody = result.getBody();
            Assert.assertTrue("Sent successful: " + responseBody, responseBody.contains("Message sent successfully"));
        } finally {
            deployer.undeploy(GREENMAIL_WAR);
            deployer.undeploy(EXAMPLE_CAMEL_MAIL_WAR);
        }
    }

    private String getEndpointAddress(String contextPath) throws MalformedURLException {
        return "http://localhost:8080" + contextPath;
    }
}
