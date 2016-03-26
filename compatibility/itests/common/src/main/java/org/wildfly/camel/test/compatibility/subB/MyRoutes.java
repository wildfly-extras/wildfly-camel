package org.wildfly.camel.test.compatibility.subB;

import javax.inject.Inject;

import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.Uri;
import org.wildfly.extension.camel.CamelAware;

/**
 * Configures all our Camel routes, components, endpoints and beans
 */
@CamelAware
public class MyRoutes extends RouteBuilder {

    @Inject
    @Uri("timer:foo?period=500")
    private Endpoint inputEndpoint;

    @Inject
    @Uri("log:output")
    private Endpoint resultEndpoint;

    @Override
    public void configure() throws Exception {
        // you can configure the route rule with Java DSL here

        from(inputEndpoint)
            .to("bean:counterBean")
            .to(resultEndpoint);
    }

}