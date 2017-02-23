package org.wildfly.camel.test.sshd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.ssh.EmbeddedSSHServer;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({SSHIntegrationTest.SSHServerSetupTask.class})
public class SSHIntegrationTest {

    static class SSHServerSetupTask implements ServerSetupTask {

        static final EmbeddedSSHServer sshServer = new EmbeddedSSHServer(Paths.get("target/sshd"));

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            sshServer.start();
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            sshServer.stop();
        }
    }

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "sshd-tests.jar")
            .addAsResource(new StringAsset(SSHServerSetupTask.sshServer.getConnection()), "ssh-connection");
    }

    @Test
    public void testSSHConsumer() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("ssh://admin@%s?username=admin&password=admin&pollCommand=echo Hello Kermit", getConnection())
                .to("mock:end");
            }
        });

        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:end", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(1);
            mockEndpoint.setAssertPeriod(100);
            mockEndpoint.expectedBodiesReceived("Hello Kermit" + System.lineSeparator());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    @Ignore("[#1564] SSH producer test frequently swallows the output")
    public void testSSHProducer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("ssh://admin@%s?username=admin&password=admin", getConnection());
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            String result = template.requestBody("direct:start", "echo Hello Kermit", String.class);
            Assert.assertEquals("Hello Kermit" + System.lineSeparator(), result);
        } finally {
            camelctx.stop();
        }
    }

    private String getConnection() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/ssh-connection")))) {
            return br.readLine();
        }
    }
}
