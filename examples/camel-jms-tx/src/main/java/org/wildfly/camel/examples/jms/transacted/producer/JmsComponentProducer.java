package org.wildfly.camel.examples.jms.transacted.producer;

import javax.annotation.Resource;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.jms.ConnectionFactory;

import org.apache.camel.component.jms.JmsComponent;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Creates an instance of the Camel JmsComponent and configures it to support JMS transactions.
 */
public class JmsComponentProducer {

    @Resource(mappedName = "java:/JmsXA")
    private ConnectionFactory connectionFactory;

    @Produces
    @Named("jms")
    public JmsComponent createJmsComponent(PlatformTransactionManager transactionManager) {
        return JmsComponent.jmsComponentTransacted(connectionFactory, transactionManager);
    }
}
