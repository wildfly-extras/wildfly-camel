package org.wildfly.camel.test.sshd;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.ssh.EmbeddedSSHServer;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class SSHIntegrationTest {

    private static final Path SSHD_ROOT_DIR = Paths.get("target/sshd");
    private EmbeddedSSHServer sshServer;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "sshd-tests.jar")
            .addClasses(EmbeddedSSHServer.class, TestUtils.class, EnvironmentUtils.class)
            .setManifest(() -> {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "com.jcraft.jsch,org.apache.sshd");
                return builder.openStream();
            });
    }

    @Before
    public void setUp() throws Exception {
        sshServer = new EmbeddedSSHServer(SSHD_ROOT_DIR);
        sshServer.start();
    }

    @After
    public void tearDown() throws Exception {
        if (sshServer != null) {
            sshServer.stop();
        }
    }

    @Test
    public void testSSHConsumer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("ssh://admin@" + sshServer.getConnection() + "?username=admin&password=admin&pollCommand=echo Hello Kermit")
                .to("mock:end");
            }
        });

        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:end", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(1);
            mockEndpoint.expectedBodiesReceived("Hello Kermit" + System.lineSeparator());
            mockEndpoint.assertIsSatisfied(5000);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testSSHProducer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("ssh://admin@" + sshServer.getConnection() + "?username=admin&password=admin");
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
}
