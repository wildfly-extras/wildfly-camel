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

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.apache.camel.CamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.jpa.subA.Account;
import org.wildfly.camel.test.jpa.subA.JpaRouteBuilder;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
public class UserManagedTransactionIntegrationTest {

    @Inject
    CamelContext camelctx;

    @PersistenceContext
    EntityManager em;

    @Resource(mappedName = "java:jboss/UserTransaction")
    private UserTransaction utx;

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-jpa-tests.jar");
        archive.addPackage(JpaRouteBuilder.class.getPackage());
        archive.addAsResource("jpa/persistence.xml", "META-INF/persistence.xml");
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    @Before
    public void setUp() throws Exception {
        // Insert some test data
        utx.begin();
        em.joinTransaction();
        em.persist(new Account(1, 750));
        em.persist(new Account(2, 300));
        utx.commit();
        em.clear();
    }

    @After
    public void tearDown() throws Exception {
        // Clean up test data
        utx.begin();
        em.joinTransaction();
        em.createQuery("delete from Account").executeUpdate();
        utx.commit();
    }

    @Test
    public void testJpaTransactionalRoute() throws Exception {

        Account accountA = em.getReference(Account.class, 1);
        Account accountB = em.getReference(Account.class, 2);

        Assert.assertEquals(750, accountA.getBalance());
        Assert.assertEquals(300, accountB.getBalance());

        // Update account id 2 with 250
        Map<String, Integer> params = new HashMap<>();
        params.put("sourceAccountId", 1);
        params.put("targetAccountId", 2);
        params.put("amount", 250);

        camelctx.createProducerTemplate().sendBody("direct:start", params);
        accountA = em.getReference(Account.class, 1);
        accountB = em.getReference(Account.class, 2);

        // Account A should have been decremented by 250, Account B should have been credited 250
        Assert.assertEquals(500, accountA.getBalance());
        Assert.assertEquals(550, accountB.getBalance());
    }

    @Test
    public void testJpaTransactionalRouteRollback() throws Exception {

        Account accountA = em.getReference(Account.class, 1);
        Account accountB = em.getReference(Account.class, 2);

        Assert.assertEquals(750, accountA.getBalance());
        Assert.assertEquals(300, accountB.getBalance());

        // Attempt to update account 2 with 550 which is greater than max allowed amount
        Map<String, Integer> params = new HashMap<>();
        params.put("sourceAccountId", 1);
        params.put("targetAccountId", 2);
        params.put("amount", 550);

        camelctx.createProducerTemplate().sendBody("direct:start", params);
        accountA = em.getReference(Account.class, 1);
        accountB = em.getReference(Account.class, 2);

        // Transaction should have been rolled back and accounts left unmodified
        Assert.assertEquals(750, accountA.getBalance());
        Assert.assertEquals(300, accountB.getBalance());

        // Transaction should have been dead lettered
        String deadLetterDir = System.getProperty("jboss.server.data.dir") + "/deadletter/";
        Assert.assertTrue(new File(Paths.get(deadLetterDir, "deadLetters.xml").toUri()).exists());
    }
}
