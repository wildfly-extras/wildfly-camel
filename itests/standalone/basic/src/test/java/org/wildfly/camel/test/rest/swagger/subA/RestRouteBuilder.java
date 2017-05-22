package org.wildfly.camel.test.rest.swagger.subA;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.model.rest.RestBindingMode;

@ContextName("rest-service-camel-context")
public class RestRouteBuilder extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        restConfiguration()
            .host("localhost")
            .port(8080)
            .component("undertow")
            .contextPath("/api")
            .apiProperty("api.title", "WildFly Camel REST API")
            .apiProperty("api.version", "1.0")
            .apiContextPath("swagger");

        rest("/customers").description("Customers REST service")
            .get("/{id}")
                .bindingMode(RestBindingMode.auto)
                .id("getCustomerById")
                .description("Retrieves a customer for the specified id")
                .outType(Customer.class)
                .route()
                    .process(exchange -> {
                        Customer customer = new Customer();
                        customer.setId(exchange.getIn().getHeader("id", Integer.class));
                        customer.setFirstName("Kermit");
                        customer.setLastName("The Frog");
                        exchange.getOut().setBody(customer);
                    })
                .endRest();
    }
}
