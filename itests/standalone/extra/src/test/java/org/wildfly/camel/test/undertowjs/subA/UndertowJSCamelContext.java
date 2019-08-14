package org.wildfly.camel.test.undertowjs.subA;

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.ws.rs.Produces;

import org.apache.camel.impl.DefaultCamelContext;

@Startup
@Produces
@ApplicationScoped
@Named("undertowjs-context")
public class UndertowJSCamelContext extends DefaultCamelContext {
}
