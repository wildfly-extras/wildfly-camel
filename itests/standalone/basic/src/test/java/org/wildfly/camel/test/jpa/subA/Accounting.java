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

package org.wildfly.camel.test.jpa.subA;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

@Stateless
public class Accounting {

    @PersistenceContext
    EntityManager em;

    @Resource
    SessionContext context;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void transfer(int amount) {
        Account accountA = em.getReference(Account.class, 1);
        Account accountB = em.getReference(Account.class, 2);

        // update the from balance
        accountA.setBalance(accountA.getBalance() - amount);

        // update the mirror balance
        em.createQuery("update Account set balance = " + accountA.getBalance() + " where id = 3").executeUpdate();

        // rollback if from balance is < 0
        if (accountA.getBalance() < 0) {
            context.setRollbackOnly();
            return;
        }

        // update the to balance
        accountB.setBalance(accountB.getBalance() + amount);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void transferCamel(int amount) throws Exception {
        Account accountA = em.getReference(Account.class, 1);
        Account accountB = em.getReference(Account.class, 2);

        // update the from balance
        accountA.setBalance(accountA.getBalance() - amount);

        // do something in camel that is transactional
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .wireTap("log:org.wildfly.camel.test.jpa?level=WARN")
                .to("sql:update Account set balance = :#${body} where id = 3?dataSource=java:jboss/datasources/ExampleDS");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.requestBody("direct:start", accountA.getBalance());
        } finally {
            camelctx.stop();
        }

        // rollback if from balance is < 0
        if (accountA.getBalance() < 0) {
            context.setRollbackOnly();
            return;
        }

        // update the to balance
        accountB.setBalance(accountB.getBalance() + amount);
    }
}
