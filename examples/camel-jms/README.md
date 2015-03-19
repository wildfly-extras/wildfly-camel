# Camel JMS example

This example demonstrates using the camel-jms component with WildFly Camel susbsystem to produce and consume JMS messages.

In this example, a Camel route consumes files from JBOSS_JOME/standalone/data/orders and places their contents onto an in-memory HornetQ JMS queue
named 'OrdersQueue'. A second route consumes any messages from 'OrdersQueue' and through a simple [content based router](http://camel.apache.org/content-based-router.html)
sorts the orders into individual country directories within JBOSS_JOME/standalone/data/orders/processed.

## Running the example

To run the example.

1. Change into the `examples/camel-jms` directory
2. Run `mvn clean wildfly:run`
3. When the WildFly server has started, browse to `http://localhost:8080/example-camel-jms/orders`

You should see a page titled 'Orders Received'. As we send orders to the example application, a list
of orders per country will be listed on this page.

## Testing Camel JMS

There are some example order XML files within the `examples/camel-jms` directory. To make Camel
consume them and send them to the 'OrdersQueue' JMS destination, simply copy them to the orders input
directory.

For Linux / Mac users:

    cp src/main/resources/*.xml target/wildfly-*/standalone/data/orders/

For Windows users:

    copy src\main\resources\*.xml target\wildfly-*\standalone\data\orders\

The console will output messages detailing what happened to each of the orders. The output
will look something like this.

```
JmsConsumer[OrdersQueue]) Sending order uk-order.xml to the UK
JmsConsumer[OrdersQueue]) Sending order other-order.xml to another country
JmsConsumer[OrdersQueue]) Sending order us-order.xml to the US
```

Once the files have been consumed, you can return to `http://localhost:8080/example-camel-jms/orders`. The count of
received orders for each country should have been increased by 1.

All processed orders will have been output to:

    JBOSS_JOME/standalone/data/orders/processed/uk
    JBOSS_JOME/standalone/data/orders/processed/us
    JBOSS_JOME/standalone/data/orders/processed/others

## Learn more

Additional camel-jms documentation can be found at the [WildFly Camel GitBook](http://wildflyext.gitbooks.io/wildfly-camel/content/javaee/jms.html
) site.
