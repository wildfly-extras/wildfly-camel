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
package org.wildfly.camel.test.mail;

import java.io.File;
import java.net.MalformedURLException;

import javax.annotation.Resource;
import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.utils.DMRUtils;

public abstract class AbstractMailExampleTest {

    private static final String GREENMAIL_WAR = "greenmail.war";

    @ArquillianResource
    Deployer deployer;

    @Resource(lookup = "java:jboss/mail/greenmail")
    private Session mailSession;

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
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
            .addClasses(HttpRequest.class, AbstractMailExampleTest.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Deployment(managed = false, testable = false, name = GREENMAIL_WAR)
    public static WebArchive createGreenmailDeployment() {
        return ShrinkWrap.createFromZipFile(WebArchive.class, new File("target/examples/greenmail-webapp.war"));
    }

    @Test
    public void sendEmailTest() throws Exception {
        try {
            deployer.deploy(GREENMAIL_WAR);
            deployer.deploy(getDeploymentName());

            StringBuilder endpointURL = new StringBuilder("from=user1@localhost");
            endpointURL.append("&to=user2@localhost")
                    .append("&subject=Greetings")
                    .append("&message=Hello");

            HttpRequest.HttpResponse result = HttpRequest.post(getEndpointAddress("send"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .content(endpointURL.toString())
                    .getResponse();

            String responseBody = result.getBody();
            Assert.assertTrue("Sent successful: " + responseBody, responseBody.contains("Message sent successfully"));

            // Verify that the email made it to the target address
            Store store = mailSession.getStore("pop3");
            store.connect();

            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);
            Assert.assertEquals(1, folder.getMessageCount());
        } finally {
            deployer.undeploy(getDeploymentName());
            deployer.undeploy(GREENMAIL_WAR);
        }
    }

    private String getEndpointAddress(String contextPath) throws MalformedURLException {
        return "http://localhost:8080/" + getContextPath() + "/" + contextPath;
    }

    abstract String getContextPath();
    abstract String getDeploymentName();
}
