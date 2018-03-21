package org.wildfly.camel.test.cxf.rs;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.cxf.rs.subA.AsyncService;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class CXFRSAsyncTest {

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "cxfrs-async-tests.jar")
            .addClasses(AsyncService.class)
            .addAsResource("cxf/spring/cxfrs-async-camel-context.xml", "cxfrs-async-camel-context.xml");
    }

    @Test
    public void testCxfJaxRsServer() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("cxfrs-async-context");
        Assert.assertNotNull("Expected cxfrs-async-context to not be null", camelctx);
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

        ProducerTemplate template = camelctx.createProducerTemplate();
        String result = template.requestBody("direct:start", null, String.class);
        Assert.assertEquals("Hello Kermit", result);
    }
}
