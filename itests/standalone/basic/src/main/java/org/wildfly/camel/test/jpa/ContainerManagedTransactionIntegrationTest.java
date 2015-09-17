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
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.jpa.subA.Account;
import org.wildfly.camel.test.jpa.subA.Accounting;

@RunWith(Arquillian.class)
public class ContainerManagedTransactionIntegrationTest {

    @PersistenceContext
    EntityManager em;

    @Resource(mappedName = "java:jboss/UserTransaction")
    private UserTransaction utx;

    @Inject
    Accounting accounting;

    @Before
    public void setUp() throws Exception {
        utx.begin();
        em.persist(new Account(1, 750));
        em.persist(new Account(2, 300));
        em.persist(new Account(3, 0));
        utx.commit();
    }

    @After
    public void tearDown() throws Exception {
        utx.begin();
        em.createQuery("delete from Account").executeUpdate();
        utx.commit();
    }

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "container-tx-tests.jar");
        archive.addClasses(Accounting.class, Account.class);
        archive.addAsResource("jpa/persistence.xml", "META-INF/persistence.xml");
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    @Test
    public void testGoodTransfer() throws Exception {

        accounting.transfer(250);

        Account accountA = em.getReference(Account.class, 1);
        Account accountB = em.getReference(Account.class, 2);
        Account accountC = em.getReference(Account.class, 3);

        Assert.assertEquals(500, accountA.getBalance());
        Assert.assertEquals(550, accountB.getBalance());
        Assert.assertEquals(500, accountC.getBalance());
    }

    @Test
    public void testBadTransfer() throws Exception {

        accounting.transfer(1250);

        Account accountA = em.getReference(Account.class, 1);
        Account accountB = em.getReference(Account.class, 2);
        Account accountC = em.getReference(Account.class, 3);

        Assert.assertEquals(750, accountA.getBalance());
        Assert.assertEquals(300, accountB.getBalance());
        Assert.assertEquals(0, accountC.getBalance());
    }

    @Test
    public void testGoodCamelTransfer() throws Exception {

        accounting.transferCamel(250);

        Account accountA = em.getReference(Account.class, 1);
        Account accountB = em.getReference(Account.class, 2);
        Account accountC = em.getReference(Account.class, 3);

        Assert.assertEquals(500, accountA.getBalance());
        Assert.assertEquals(550, accountB.getBalance());
        Assert.assertEquals(500, accountC.getBalance());
    }


    @Test
    public void testBadCamelTransfer() throws Exception {

        accounting.transferCamel(1250);

        Account accountA = em.getReference(Account.class, 1);
        Account accountB = em.getReference(Account.class, 2);
        Account accountC = em.getReference(Account.class, 3);

        Assert.assertEquals(750, accountA.getBalance());
        Assert.assertEquals(300, accountB.getBalance());
        Assert.assertEquals(0, accountC.getBalance());
    }
}
