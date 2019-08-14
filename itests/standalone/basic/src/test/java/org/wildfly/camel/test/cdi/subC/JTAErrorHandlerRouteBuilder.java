/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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
package org.wildfly.camel.test.cdi.subC;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.camel.cdi.CdiRouteBuilder;

@ApplicationScoped
public class JTAErrorHandlerRouteBuilder extends CdiRouteBuilder {

    @PersistenceContext
    private EntityManager entityManager;

    private boolean forceRollback = true;

    @Override
    public void configure() throws Exception {

        errorHandler(transactionErrorHandler()
            .setTransactionPolicy("PROPAGATION_REQUIRED")
            .maximumRedeliveries(1)
            .onRedelivery(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                order.setProductName(order.getProductName() + " redelivered");
        }));

        from("direct:start")
        .transacted("PROPAGATION_REQUIRED")
        .process(exchange -> {
            Order order = exchange.getIn().getBody(Order.class);
            entityManager.merge(order);

            if (forceRollback) {
                forceRollback = false;
                throw new IllegalStateException("Forced exception");
            }
        })
        .to("mock:result");
    }
}
