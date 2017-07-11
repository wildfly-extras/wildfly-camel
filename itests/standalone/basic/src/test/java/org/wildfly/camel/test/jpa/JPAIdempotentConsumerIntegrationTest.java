/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2015 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package org.wildfly.camel.test.jpa;

import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import javax.transaction.UserTransaction;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.idempotent.jpa.JpaMessageIdRepository;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class JPAIdempotentConsumerIntegrationTest {

    @Resource(mappedName = "java:jboss/IdempotentTestManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    @Resource(mappedName = "java:jboss/UserTransaction")
    private UserTransaction userTransaction;

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "jpa-idempotent-tests.jar");
        archive.addAsResource("jpa/persistence-idempotent.xml", "META-INF/persistence.xml");
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    @Test
    public void testJPAIdempotentConsumer() throws Exception {
        final TransactionTemplate transactionTemplate = new TransactionTemplate();
        transactionTemplate.setTransactionManager(new JtaTransactionManager(userTransaction));
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        final JpaMessageIdRepository messageIdRepository = new JpaMessageIdRepository(entityManagerFactory, transactionTemplate, "myProcessorName");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .idempotentConsumer(simple("${header.messageId}"), messageIdRepository)
                .to("mock:result");
            }
        });

        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(1);

            // Send 5 messages with the same messageId header. Only 1 should be forwarded to the mock:result endpoint
            ProducerTemplate template = camelctx.createProducerTemplate();
            for (int i = 0; i < 5; i++) {
                template.requestBodyAndHeader("direct:start", null, "messageId", "12345");
            }

            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

}
