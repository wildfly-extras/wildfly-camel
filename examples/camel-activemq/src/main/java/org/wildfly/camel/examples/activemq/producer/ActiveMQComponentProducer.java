package org.wildfly.camel.examples.activemq.producer;

import javax.annotation.Resource;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.jms.ConnectionFactory;

import org.apache.activemq.camel.component.ActiveMQComponent;

public class ActiveMQComponentProducer {

    /**
     * Inject the ActiveMQConnectionFactory that has been configured through the ActiveMQ Resource Adapter
     */
    @Resource(mappedName = "java:/ActiveMQConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Produces
    @Named("activemq")
    public ActiveMQComponent createActiveMQComponent() {
        ActiveMQComponent activeMQComponent = ActiveMQComponent.activeMQComponent();
        activeMQComponent.setConnectionFactory(connectionFactory);
        return activeMQComponent;
    }
}
