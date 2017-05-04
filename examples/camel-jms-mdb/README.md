Camel JMS MDB example
---------------------

This example demonstrates using the camel-jms component in conjunction with Message Driven Beans (MDB).

In this example, a Camel route sends a JMS message to an in-memory ActiveMQ Artemis queue named 'OrdersQueue'. An MDB consumes any messages from
'OrdersQueue' and uses a CDI injected `ProducerTemplate` to send the JMS message payload to a direct route named `direct:jmsIn`.

The direct consumer on `direct:jmsIn` outputs the exchange message body to the console.

Prerequisites
-------------

* Maven
* An application server with the wildfly-camel subsystem installed

Running the example
-------------------

To run the example.

1. Start the application server in standalone mode `${JBOSS_HOME}/bin/standalone.sh -c standalone-full-camel.xml`
2. Build and deploy the project `mvn install -Pdeploy`

Testing Camel JMS
-----------------

Keep watching the WildFly console output. Every 5 seconds a new JMS message will be produced by Camel and consumed by the MDB.

You should see log entries like the following:

```
09:48:08,218 INFO  [route6] (Thread-33 (ActiveMQ-client-global-threads-1610269076)) Received message: Message 1 created at Thu May 04 09:48:08 BST 2017
09:48:13,208 INFO  [route6] (Thread-34 (ActiveMQ-client-global-threads-1610269076)) Received message: Message 2 created at Thu May 04 09:48:13 BST 2017
09:48:18,203 INFO  [route6] (Thread-35 (ActiveMQ-client-global-threads-1610269076)) Received message: Message 3 created at Thu May 04 09:48:18 BST 2017
```

Undeploy
--------

To undeploy the example run `mvn clean -Pdeploy`.

Learn more
----------

Additional camel-jms documentation can be found at the [WildFly Camel User Guide](http://wildfly-extras.github.io/wildfly-camel/#_jms
) site.
