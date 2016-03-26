package org.wildfly.camel.examples.jms.transacted;

import javax.annotation.Resource;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.persistence.EntityManager;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jpa.JpaComponent;
import org.apache.camel.model.dataformat.JaxbDataFormat;
import org.wildfly.camel.examples.jms.transacted.model.Order;
import org.wildfly.extension.camel.CamelAware;

@Startup
@CamelAware
@ApplicationScoped
public class JmsRouteBuilder extends RouteBuilder {

    /**
     * Inject the resources required to configure the JMS and JPA Camel
     * components. The JPA EntityManager, JMS TransactionManager and a JMS
     * ConnectionFactory bound to the JNDI name java:/JmsXA
     */
    @Inject
    private EntityManager entityManager;

    @Inject
    private JmsTransactionManager transactionManager;

    @Resource(mappedName = "java:/JmsXA")
    private ConnectionFactory connectionFactory;

    @Override
    public void configure() throws Exception {
        /**
         * Create an instance of the Camel JmsComponent and configure it to support JMS
         * transactions.
         */
        JmsComponent jmsComponent = JmsComponent.jmsComponentTransacted(connectionFactory, transactionManager);
        getContext().addComponent("jms", jmsComponent);

        /**
         * Create an instance of the Camel JpaComponent and configure it to support transactions.
         */
        JpaComponent jpaComponent = new JpaComponent();
        jpaComponent.setEntityManagerFactory(entityManager.getEntityManagerFactory());
        jpaComponent.setTransactionManager(transactionManager);
        getContext().addComponent("jpa", jpaComponent);

        /**
         * Configure JAXB so that it can discover model classes.
         */
        JaxbDataFormat jaxbDataFormat = new JaxbDataFormat();
        jaxbDataFormat.setContextPath(Order.class.getPackage().getName());

        /**
         * Configure a simple dead letter strategy. Whenever an IllegalStateException
         * is encountered this takes care of rolling back the JMS and JPA transactions. The
         * problem message is sent to the WildFly dead letter JMS queue (DLQ).
         */
        onException(IllegalStateException.class)
            .maximumRedeliveries(1)
            .handled(true)
            .to("jms:queue:DLQ")
            .markRollbackOnly();

        /**
         * This route consumes XML files from JBOSS_HOME/standalone/data/orders and sends
         * the file content to JMS destination OrdersQueue.
         */
        from("file:{{jboss.server.data.dir}}/orders")
            .transacted()
                .to("jms:queue:OrdersQueue");

        /**
         * This route consumes messages from JMS destination OrdersQueue, unmarshalls the XML
         * message body using JAXB to an Order entity object. The order is then sent to the JPA
         * endpoint for persisting within an in-memory database.
         *
         * Whenever an order quantity greater than 10 is encountered, the route throws an IllegalStateException
         * which forces the JMS / JPA transaction to be rolled back and the message to be delivered to the dead letter
         * queue.
         */
        from("jms:queue:OrdersQueue")
                .unmarshal(jaxbDataFormat)
                .to("jpa:Order")
                .choice()
                .when(simple("${body.quantity} > 10"))
                    .log("Order quantity is greater than 10 - rolling back transaction!")
                    .throwException(new IllegalStateException())
                .otherwise()
                    .log("Order processed successfully");

    }
}

