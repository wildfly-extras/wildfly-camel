# Camel CXF Example

This example demonstrates using the camel-cxf component with the WildFly Camel Subsystem to consume JAX-WS web services.

> **IMPORTANT: At present the WildFly Camel Subsytem does not support CXF consumers. E.g endpoints defined as from("cxf://...").
Although, it is possible to mimic CXF consumer behaviour using the [CamelProxy](http://camel.apache.org/using-camelproxy.html).
See the [camel-jaxws example](../camel-jaxws/README.md) for more information.**

In this example, a Camel route takes a message payload from a direct endpoint and passes it on to a CXF producer endpoint. The producer uses the payload
to pass arguments to a JAX-WS web service.

## Prerequsites

* Maven
* An application server with the wildfly-camel subsystem installed

## Running the example

To run the example.

1. Start the application server in standalone mode `${JBOSS_HOME}/bin/standalone.sh -c standalone-full-camel.xml`
2. Build and deploy the project `mvn install -Pdeploy`
3. Browse to `http://localhost:8080/example-camel-cxf/`

You should see a page titled 'Send A Greeting'. This UI enables us to interact with the test 'greeting' web service which will have also been
started. The service WSDL is available at `http://localhost:8080/example-camel-cxf/greeting?wsdl`.

There is a single service operation named 'greet' which takes 2 String parameters named 'message' and 'name'. The web method will concatenate these
together and return the result.

## Testing Camel CXF

### Web UI

Browse to `http://localhost:8080/example-camel-cxf/`.

From the 'Send A Greeting' web form, enter a 'message' and 'name' into the text fields and press the 'send' button. You'll then
see the information you entered combined to display a greeting on the UI.

So what just happened there?

`CamelCxfServlet` handles the POST request from the web UI. It retrieves the message and name form parameter values and constructs an
object array. This object array will be the message payload that is sent to the `direct:start` endpoint. A `ProducerTemplate`
sends the message payload to Camel. `CxfRouteBuilder` forwards the payload from the `direct:start` endpoint to a CXF producer endpoint. The CXF endpoint
then invokes the `GreetingServiceImpl` web service.

The web service response is used by `CamelCxfServlet` to display the greeting on the web UI.

## Undeploy
    
To undeploy the example run `mvn clean -Pdeploy`.

## Learn more

Additional camel-cxf documentation can be found at the [WildFly Camel GitBook](http://wildflyext.gitbooks.io/wildfly-camel/content/javaee/jaxws.html
) site.
