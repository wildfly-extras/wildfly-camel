package org.wildfly.camel.test.undertow.subA;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@ApplicationScoped
public class ApplicationScopeRouteBuilder extends RouteBuilder {

	static final Logger LOG = LoggerFactory.getLogger(ApplicationScopeRouteBuilder.class);
	
	@Override
	public void configure() throws Exception {
		
		String httpUrl = "http://127.0.0.1:8080/hello";
		
		LOG.warn("Configure {} with {}", ApplicationScopeRouteBuilder.class.getSimpleName(), httpUrl);
		
		fromF("undertow:%s", httpUrl)
			.transform(simple("Hello ${header.name}"));
	}
}