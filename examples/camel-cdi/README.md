Camel CDI Example
-----------------

This example demonstrates using the camel-cdi component with the WildFly Camel Subsystem to integrate CDI beans with Camel routes.

In this example, a Camel route takes a message payload from a servlet HTTP GET request and passes it on to a direct endpoint. The payload
is then passed onto a Camel CDI bean invocation to produce a message response which is displayed on the web browser page.

Prerequisites
-------------

* Maven
* An application server with the wildfly-camel subsystem installed

Running the example
-------------------

To run the example.

1. Start the application server in standalone mode `${JBOSS_HOME}/bin/standalone.sh -c standalone-full-camel.xml`
2. Build and deploy the project `mvn install -Pdeploy`

Testing Camel CXF
-----------------

Web UI
------

Browse to http://localhost:8080/example-camel-cdi/?name=Kermit.

You should see the message "Hello Kermit" output on the web page.

The Camel route is very simple and looks like this:

```
from("direct:start").beanRef("helloBean");
```

The `beanRef` DSL makes camel look for a bean named 'helloBean' in the bean registry. The magic that makes this bean
available to Camel is found in the `SomeBean` class.

```java
@Named("helloBean")
public class SomeBean {

    public String someMethod(String message) {
        return "Hello " + message;
    }
}
```

By using the `@Named` annotation, camel-cdi will add this bean to the Camel bean registry.

## Undeploy

To undeploy the example run `mvn clean -Pdeploy`.

## Learn more

Additional camel-cdi documentation can be
found at the [WildFly Camel User Guide](http://wildfly-extras.github.io/wildfly-camel/#_camel_cdi) site.
