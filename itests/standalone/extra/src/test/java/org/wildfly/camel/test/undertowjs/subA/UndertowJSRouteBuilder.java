package org.wildfly.camel.test.undertowjs.subA;

import org.apache.camel.builder.RouteBuilder;

public class UndertowJSRouteBuilder extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:start").transform(body().prepend("Hello "));
    }
}
