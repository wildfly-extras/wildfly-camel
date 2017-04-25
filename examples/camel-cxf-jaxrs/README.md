Camel CXF JAX-RS Example
------------------------

This example demonstrates using the camel-cxf component with the WildFly Camel Subsystem to produce and consume JAX-RS REST services.

In this example, a Camel route takes a message payload from a direct endpoint and passes it on to a CXF producer endpoint. The producer uses the payload
to pass arguments to a CXF JAX-RS REST service.

Prerequisites
-------------

* Maven
* An application server with the wildfly-camel subsystem installed

Running the example
-------------------

To run the example.

1. Start the application server in standalone mode `${JBOSS_HOME}/bin/standalone.sh -c standalone-full-camel.xml`
2. Build and deploy the project `mvn install -Pdeploy`
3. Browse to http://localhost:8080/example-camel-cxfrs/

You should see a page titled 'Send A Greeting'. This UI enables us to interact with the test 'greeting' REST service which will have also been
started.

There is a single service operation named 'greet' which takes a String parameter called 'name'. Invoking the service will return
a response as a simple String greeting message.

Testing Camel CXF JAX-RS
------------------------

Web UI
------

Browse to http://localhost:8080/example-camel-cxfrs/.

From the 'Send A Greeting' web form, enter a 'name' into the text field and press the 'send' button. You'll then
see a simple greeting message displayed on the UI.

So what just happened there?

`CamelCxfRsServlet` handles the POST request from the web UI. It retrieves the name form parameter value and constructs an
object array. This object array will be the message payload that is sent to the `direct:start` endpoint. A `ProducerTemplate`
sends the message payload to Camel. `The direct:start` endpoint passes the object array to a `cxfrs:bean` REST service producer.
The REST service response is used by `CamelCxfRsServlet` to display the greeting on the web UI.

The full Camel route can be seen in `src/main/webapp/WEB-INF/cxfrs-camel-context.xml`.

## Undeploy

To undeploy the example run `mvn clean -Pdeploy`.

## Learn more

Additional camel-cxf documentation can be found at the [WildFly Camel User Guide](http://wildfly-extras.github.io/wildfly-camel/#_jax_rs
) site.
