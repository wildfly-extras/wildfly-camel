package org.wildfly.camel.examples.jms.transacted.producer;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.persistence.EntityManager;

import org.apache.camel.component.jpa.JpaComponent;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Creates an instance of the Camel JpaComponent and configures it to support transactions.
 */
public class JpaComponentProducer {

    @Produces
    @Named("jpa")
    public JpaComponent createJpaComponent(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        JpaComponent jpaComponent = new JpaComponent();
        jpaComponent.setEntityManagerFactory(entityManager.getEntityManagerFactory());
        jpaComponent.setTransactionManager(transactionManager);
        return jpaComponent;
    }
}
