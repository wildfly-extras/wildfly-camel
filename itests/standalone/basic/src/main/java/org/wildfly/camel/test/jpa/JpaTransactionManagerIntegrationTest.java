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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.camel.CamelContext;
import org.apache.camel.component.jpa.JpaComponent;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.jpa.subA.Account;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
public class JpaTransactionManagerIntegrationTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-jpa-tests.jar");
        archive.addClass(Account.class);
        archive.addAsResource("jpa/persistence-local.xml", "META-INF/persistence.xml");
        archive.addAsResource("jpa/jpa-camel-context.xml", "META-INF/jboss-camel-context.xml");
        return archive;
    }

    @Test
    public void testJpaTransactionManagerRouteRoute() throws Exception {

        CamelContext camelctx = contextRegistry.getCamelContext("jpa-context");
        Assert.assertNotNull("Expected jpa-context to not be null", camelctx);

        // Persist a new account entity
        Account account = new Account(1, 500);
        camelctx.createProducerTemplate().sendBody("direct:start", account);

        JpaComponent component = camelctx.getComponent("jpa", JpaComponent.class);
        EntityManagerFactory entityManagerFactory = component.getEntityManagerFactory();

        // Read the saved entity back from the database
        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();
        Account result = em.getReference(Account.class, 1);
        em.getTransaction().commit();

        Assert.assertEquals(account, result);
    }

}
