package org.wildfly.camel.test.undertowjs;

import org.apache.camel.CamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.undertowjs.subA.UndertowJSRouteBuilder;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
@CamelAware
public class UndertowJSIntegrationTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
            .addAsWebInfResource(new StringAsset("camelRoutes.js"), "undertow-scripts.conf")
            .addAsWebInfResource(new StringAsset("<jboss-web><context-root>test-undertowjs</context-root></jboss-web>"), "jboss-web.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsWebResource("undertowjs/camelRoutes.js", "camelRoutes.js")
            .addClasses(HttpRequest.class, UndertowJSRouteBuilder.class);
    }

    @Test
    public void testUndertowJSCamelIntegration() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("undertowjs-context");
        Assert.assertNotNull("Expected camel context to not be null", camelctx);

        HttpRequest.HttpResponse response = HttpRequest.get("http://localhost:8080/test-undertowjs/greeting/Kermit").getResponse();
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertEquals("Hello Kermit", response.getBody());
    }
}
