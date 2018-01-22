package org.wildfly.camel.test.ejb.subB;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;

@ApplicationScoped
@ContextName("cdi-ear-context")
public class CDICamelContext extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:start")
        .to("ejb:java:global/camel-ejb-ear/camel-ejb-sub-deployment/HelloBean");
    }
}
