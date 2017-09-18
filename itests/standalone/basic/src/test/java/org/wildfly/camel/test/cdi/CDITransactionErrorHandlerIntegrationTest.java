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
package org.wildfly.camel.test.cdi;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.cdi.subC.JTAErrorHandlerRouteBuilder;
import org.wildfly.camel.test.cdi.subC.Order;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class CDITransactionErrorHandlerIntegrationTest {

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @PersistenceContext
    private EntityManager entityManager;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "jta-err-handler-tests.jar")
            .addPackage(JTAErrorHandlerRouteBuilder.class.getPackage())
            .addAsManifestResource("cdi/persistence.xml", "persistence.xml")
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testJTATransactionErrorHandler() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("cdi-tx-context");
        Assert.assertNotNull("Expected cdi-tx-context to not be null", camelctx);

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        ProducerTemplate template = camelctx.createProducerTemplate();
        Order order = new Order();
        order.setProductSku("12345");
        order.setProductName("Test product");

        template.sendBody("direct:start", order);
        mockEndpoint.assertIsSatisfied();

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Order> query = criteriaBuilder.createQuery(Order.class);
        query.select(query.from(Order.class));
        List<Order> orders = entityManager.createQuery(query).getResultList();

        Assert.assertEquals(1, orders.size());
        Assert.assertEquals("Test product redelivered", orders.get(0).getProductName());
    }
}
