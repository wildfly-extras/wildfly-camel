package org.wildfly.camel.test.nats;

import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nats.Connection;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class NatsIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
    	return ShrinkWrap.create(JavaArchive.class, "came-nats-tests.jar");
    }

    @Test
    public void testNatsComponent() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        Endpoint endpoint = camelctx.getEndpoint("nats://localhost:4222?topic=test");
        Assert.assertNotNull(endpoint);
        Assert.assertEquals(endpoint.getClass().getName(), "org.apache.camel.component.nats.NatsEndpoint");
    }
    
    @Ignore("We need a bit of tuning on CI server to make this test works")
    @Test
    public void testNatsRoutes() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        try {
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                	from("nats://demo.nats.io:4222?topic=test")
                	.to("mock:result");
                }
            });
            
            MockEndpoint to = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            to.expectedBodiesReceivedInAnyOrder("message");
            to.expectedMessageCount(1);

            camelctx.start();
            
            Properties opts = new Properties();
            opts.put("servers", "nats://demo.nats.io:4222");

            Connection conn = Connection.connect(opts);
            conn.publish("test", "message");
            
            to.assertIsSatisfied(3000);

        } finally {
            camelctx.stop();
        }
    }
}
