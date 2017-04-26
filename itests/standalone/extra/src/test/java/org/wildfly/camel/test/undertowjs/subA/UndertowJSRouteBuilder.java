package org.wildfly.camel.test.undertowjs.subA;

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;

@ApplicationScoped
@Startup
@ContextName("undertowjs-context")
public class UndertowJSRouteBuilder extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:start").transform(body().prepend("Hello "));
    }
}
