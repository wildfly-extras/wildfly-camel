#camel-cxf-soap: Demonstrates a code first SOAP web service with the Camel subsystem and Apache CXF on JBoss EAP

What is it?
-----------
This quick start demonstrates how to create a code first Web service with the Camel subsystem and CXF on JBoss EAP. The demonstration exposes
a 'greeting' web service which returns a hello or goodbye greeting.

In studying this quick start you will learn:

* how to configure JAX-WS Web services with the Camel subsystem using the [CXF Component](http://camel.apache.org/cxf.html)
* how to use standard Java Web Service annotations to define a Web service interface
* how to use use an HTTP URL to invoke a remote Web service


Build and Deploy the Quickstart
-------------------------------
To build and deploy the quick start:

1. Change your working directory to `examples/camel-cxf-soap` directory.
* Run `mvn clean install` to build the quick start.
* Copy the resulting 'camel-cxf-soap' WAR artifact to your JBoss EAP deployment directory **OR** run with Maven:

        mvn wildfly:run

### Browsing web service WSDL

After deploying this quick start, you can browse the generated WSDL for the 'greeting' service by browsing to the following URL:

    http://localhost:8080/example-camel-cxf-soap/greeting?wsdl


### To run the test:

In this quick start, we also provide an integration test which starts a JBoss EAP server, deploys the CXF web application and performs test SOAP requests
against the 'greeting' web service.

To run the integration test:

1. Change to the `camel-cxf-soap` directory.
2. Run the following command:

        mvn integration-test

The test sends two HTTP SOAP requests to the 'greeting' web service. One request is sent to the 'sayHello' web method and the other
is sent to the 'sayGoodbye' method. The SOAP request XML content can be found within files `hello-request.xml` and `goodbye-request.xml` 
in `src/test/resources`.

The integration tests output the SOAP responses receieved from the web services to the console.

You will see this response from the `sayHello` method:

    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
       <soap:Body>
          <ns2:sayHelloResponse xmlns:ns2="http://cxf.examples.camel.wildfly.org/">
             <return>Hello John Doe</return>
          </ns2:sayHelloResponse>
       </soap:Body>
    </soap:Envelope>

You will see this response from the `sayGoodbye` method:

    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
       <soap:Body>
          <ns2:sayGoodbyeResponse xmlns:ns2="http://cxf.examples.camel.wildfly.org/">
             <return>Goodbye John Doe</return>
          </ns2:sayGoodbyeResponse>
       </soap:Body>
    </soap:Envelope>

### To run a Web client:

You can use an external tool such as SoapUI to test web services by configuring a project based off of the WSDL URL listed above. 
