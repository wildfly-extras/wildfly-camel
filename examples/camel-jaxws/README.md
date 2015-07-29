# Camel JAX-WS Example

This example demonstrates using the [CamelProxy](http://camel.apache.org/using-camelproxy.html) to mimic the behaviour
of a Camel JAX-WS consumer. This is an alternative to using CXF consumers **which are not currently supported by the WildFly Camel Subsystem**.

## Prerequsites

* Maven
* An application server with the wildfly-camel subsystem installed

## Running the example

To run the example.

1. Start the application server in standalone mode `${JBOSS_HOME}/bin/standalone.sh -c standalone-full-camel.xml`
2. Build and deploy the project `mvn install -Pdeploy`
3. Browse to `http://localhost:8080/example-camel-jaxws/`

You should see a page titled 'Send A Greeting'. This UI enables us to interact with the test 'greeting' web service which will have also been
started. The service WSDL is available at `http://localhost:8080/example-camel-jaxws/greeting?wsdl`.

There are 2 service operations:

* greet - Which takes 2 String parameters named 'message' and 'name'
* greetWithMessage - Which takes a single parameter 'name'

## Testing Camel CXF

### Web UI

Browse to `http://localhost:8080/example-camel-cxf/`.

From the 'Send A Greeting' web form, enter a 'message' and 'name' into the text fields and press the 'send' button. You'll then
see the information you entered combined to display a greeting on the UI.

So what just happened there?

`JaxwsServlet` handles the POST request from the web UI. It retrieves the message and name form parameter values and sends them to the
greeting web service. If both 'message' and 'name' values are present then the `greetWithMessage` method is invoked. Otherwise the `greet` method is invoked.

When the service method is invoked the `CamelProxy` results in the `direct:start` route within `JaxwsRouteBuilder` to run. Notice that a processor class
handles the web service response by figuring out which web service method was invoked. Finally the desired response is set on the exchange 'out' body.

```java
from("direct:start")
    .process(new Processor() {
        @Override
        public void process(Exchange exchange) throws Exception {
            BeanInvocation beanInvocation = exchange.getIn().getBody(BeanInvocation.class);
            String methodName = beanInvocation.getMethod().getName();

            if(methodName.equals("greet")) {
              String name = exchange.getIn().getBody(String.class);
              exchange.getOut().setBody("Hello " + name);
            } else if(methodName.equals("greetWithMessage")) {
              String message = (String) beanInvocation.getArgs()[0];
              String name = (String) beanInvocation.getArgs()[1];
              exchange.getOut().setBody(message + " " + name);
            } else {
              throw new IllegalStateException("Unknown method invocation " + methodName);
            }
        }
     });
```

### cURL

Alternatively, if you would prefer to work with the raw web service SOAP XML. You can use cURL to send SOAP messages to the web service. Example
SOAP requests can be found at `src/main/resources`. To send the file contents to the greeting web service do.

**greet:**
```
curl -v -X POST -H 'Content-Type: text/xml;charset=UTF-8' -H 'SOAPAction: urn:greet' -d @src/main/resources/soap-request-greet.xml http://localhost:8080/example-camel-jaxws/greeting
```

Returns a response of:

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:greetResponse xmlns:ns2="http://jaxws.examples.camel.wildfly.org/">
         <return>Hello Kermit</return>
      </ns2:greetResponse>
   </soap:Body>
</soap:Envelope>
```


**greetWithMessage:**
```
curl -v -X POST -H 'Content-Type: text/xml;charset=UTF-8' -H 'SOAPAction: urn:greetWithMessage' -d @src/main/resources/soap-request-greet-with-message.xml http://localhost:8080/example-camel-jaxws/greeting
```

Returns a response of:

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <ns2:greetWithMessageResponse xmlns:ns2="http://jaxws.examples.camel.wildfly.org/">
         <return>Hi there Kermit</return>
      </ns2:greetWithMessageResponse>
   </soap:Body>
</soap:Envelope>
```

### SoapUI

If you do not have cURL then you can use a tool like SoapUI to send SOAP requests. Simply configure a new project using
the web service WSDL URL `http://localhost:8080/example-camel-jaxws/greeting?wsdl`.

## Undeploy
    
To undeploy the example run `mvn clean -Pdeploy`.

## Learn more

Additional camel-jaxws documentation can be found at the [WildFly Camel GitBook](http://wildflyext.gitbooks.io/wildfly-camel/content/javaee/jaxws.html
) site.
