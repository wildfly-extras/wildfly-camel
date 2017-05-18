Camel Activemq Example
----------------------

This example demonstrates using the camel-activemq component with WildFly Camel susbsystem to produce and consume JMS messages.

In this example, a Camel route consumes files from ${JBOSS_JOME}/standalone/data/orders and places their contents onto an ActiveMQ embedded broker JMS queue
named 'OrdersQueue'. A second route consumes any messages from 'OrdersQueue' and through a simple [content based router](http://camel.apache.org/content-based-router.html)
sorts the orders into individual country directories within JBOSS_JOME/standalone/data/orders/processed.

CLI scripts automatically configure the ActiveMQ resource adapter. These scripts are located within the `src/main/resources/cli` directory.

Prerequisites
-------------

* Maven
* An application server with the wildfly-camel subsystem installed
* An ActiveMQ broker

Running the example
-------------------

To run the example.

1. Ensure your ActiveMQ broker instance is running. By default, this example expects the broker to be accessible on localhost. This can be changed by editing `src/main/resources/cli/configure-resource-adapter.cli` and modifying the `ServerUrl` attribute from `tcp://127.0.0.1:61616` to your desired host name or IP address
2. Start the application server in standalone mode `${JBOSS_HOME}/bin/standalone.sh -c standalone-full-camel.xml`
3. Deploy the ActiveMQ resource adapter `mvn install -Pdeploy-rar`
4. Restart the application server for the resource adapter configuration to take effect
5. Build and deploy the project `mvn install -Pdeploy`
6. Browse to http://localhost:8080/example-camel-activemq/orders

You should see a page titled 'Orders Received'. As we send orders to the example application, a list
of orders per country will be listed on this page.

Testing Camel ActiveMQ
----------------------

There are some example order XML files within the `src/main/resources` directory. To make Camel
consume them and send them to the 'OrdersQueue' JMS destination, simply copy them to the orders input
director.

For Linux / Mac users:

    cp src/main/resources/*.xml ${JBOSS_HOME}/standalone/data/orders/

For Windows users:

    copy src\main\resources\*.xml %JBOSS_HOME%\standalone\data\orders\

The console will output messages detailing what happened to each of the orders. The output
will look something like this.

```
JmsConsumer[OrdersQueue]) Sending order uk-order.xml to the UK
JmsConsumer[OrdersQueue]) Sending order other-order.xml to another country
JmsConsumer[OrdersQueue]) Sending order us-order.xml to the US
```

Once the files have been consumed, you can return to `http://localhost:8080/example-camel-activemq/orders`. The count of
received orders for each country should have been increased by 1.

All processed orders will have been output to:

    ${JBOSS_JOME}/standalone/data/orders/processed/uk
    ${JBOSS_JOME}/standalone/data/orders/processed/us
    ${JBOSS_JOME}/standalone/data/orders/processed/others

Undeploy
--------

To undeploy the example run `mvn clean -Pdeploy`.

This step removes the ActiveMQ resource adapter configuration but this will not take effect until the application server has been restarted.
