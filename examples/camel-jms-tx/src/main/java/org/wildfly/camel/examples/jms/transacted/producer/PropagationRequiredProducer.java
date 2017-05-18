package org.wildfly.camel.examples.jms.transacted.producer;

import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Configures a SpringTransactionPolicy using the PROPAGATION_REQUIRED policy.
 */
public class PropagationRequiredProducer {

    @Produces
    @Named("PROPAGATION_REQUIRED")
    public SpringTransactionPolicy createPropagationRequiresPolicy(PlatformTransactionManager transactionManager) {
        TransactionDefinition definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionTemplate template = new TransactionTemplate(transactionManager, definition);
        return new SpringTransactionPolicy(template);
    }
}
