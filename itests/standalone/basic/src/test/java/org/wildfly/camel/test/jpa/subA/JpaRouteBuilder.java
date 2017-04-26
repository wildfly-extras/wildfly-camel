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

import java.util.List;
import java.util.Map;

import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jpa.JpaComponent;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.wildfly.extension.camel.CamelAware;

@Startup
@CamelAware
public class JpaRouteBuilder extends RouteBuilder {

    @PersistenceContext
    EntityManager em;

    @Inject
    JtaTransactionManager transactionManager;

    @Override
    public void configure() throws Exception {

        // Configure JPA component
        JpaComponent jpaComponent = new JpaComponent();
        jpaComponent.setEntityManagerFactory(em.getEntityManagerFactory());
        jpaComponent.setTransactionManager(transactionManager);
        getContext().addComponent("jpa", jpaComponent);

        onException(IllegalArgumentException.class)
            .maximumRedeliveries(1)
            .handled(true)
            .convertBodyTo(String.class)
            .to("file:{{jboss.server.data.dir}}/deadletter?fileName=deadLetters.xml")
            .markRollbackOnly();

        from("direct:start")
            .transacted()
            .setHeader("targetAccountId", simple("${body[targetAccountId]}"))
            .setHeader("amount", simple("${body[amount]}"))

        // Take amount from the source account and decrement balance
        .to("sql:update account set balance = balance - :#amount where id = :#sourceAccountId?dataSource=wildFlyExampleDS")
        .choice()
            .when(simple("${header.amount} > 500"))
                .log("Amount is too large! Rolling back transaction")
                .throwException(new IllegalArgumentException("Amount too large"))
            .otherwise()
                .to("direct:txsmall");

        from("direct:txsmall")
        .to("sql:select balance from account where id = :#targetAccountId?dataSource=wildFlyExampleDS")
        .process(new Processor() {
            @Override
            @SuppressWarnings("unchecked")
            public void process(Exchange exchange) throws Exception {
                List<Map<String, Object>> result = (List<Map<String, Object>>) exchange.getIn().getBody();
                int id = exchange.getIn().getHeader("targetAccountId", Integer.class);
                int amount = exchange.getIn().getHeader("amount", Integer.class);
                int balance = (int) result.get(0).get("BALANCE");

                // Update target account with new balance
                Account account = em.find(Account.class, id);

                account.setBalance(balance + amount);
                exchange.getOut().setBody(account);
            }
        })
        .to("jpa:org.wildfly.camel.test.jpa.subA.Account");
    }
}
