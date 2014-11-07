camel-jpa: Demonstrates JPA with the Camel subsystem on JBoss EAP
==========================

What is it?
-----------
This quickstart demonstrates how to use JPA with the Camel subsystem on EAP. The example application uses the [Camel file endpoint](http://camel.apache.org/file.html)
to read an XML representation of a customer from an XML file, then unmarshalls it to a Customer object using JAXB and finally
persists the customer entity to an in-memory H2 database within a single table named 'customer' using the [Camel JPA endpoint](http://camel.apache.org/jpa.html).


In studying this quick start you will learn:

* how to configure JPA endpoints with Camel using the [JPA Component](http://camel.apache.org/jpa.html)
* how to use JAXB annotations to define beans
* how to unmarshall XML content to objects for persistence using JPA


## Building this example

To build from the source code:

1. Change your working directory to `examples/camel-jpa`.
1. Run `mvn clean install` to build the quickstart.


## How to run this example

After building from the source code, you can deploy the generated war file to your EAP application server.

Alternatively, you can run the example by using Maven to start a Wildfly instance. To do this:

1. Change your working directory to `examples/camel-jpa`.
1. Run `mvn wildfly:run`. If a war file is present in within the 'target' directory, it is automatically
deployed to Wildfly.


## How to try this example

Having deployed the application using any of the aforementioned deployment methods, new customers can be added to
the database by copying the example customer XML file from src/test/resources into the input/customers directory.

For example (assuming that the application is deployed and running):

1. Open a command prompt and change directory to `examples/camel-jpa`.
1. Copy src/test/resources/customer.xml file into the input/customers directory being polled by the Camel file endpoint:
        
        cp src/test/resources/customer.xml input/customers/
        
1. After copying the XML file, the Camel route will have persisted the data within to the H2 database customer table.
To see the contents of this table, open a web browser and navigate to:

        http://localhost:8080/example-camel-jpa/customers

The web page automatically refreshes every 5 seconds so you can add new customers and watch the page update.


## Undeploy this example

If the example application was started using Maven, simply press `ctrl+c` to exit. If the example application was
manually deployed to a EAP instance, then use the management console to undeploy it.
