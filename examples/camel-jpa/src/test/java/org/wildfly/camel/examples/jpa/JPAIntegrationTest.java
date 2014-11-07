/*
 * #%L
 * Wildfly Camel :: Example :: Camel CDI
 * %%
 * Copyright (C) 2013 - 2014 RedHat
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
package org.wildfly.camel.examples.jpa;

import org.apache.camel.Exchange;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.examples.jpa.model.Customer;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


@RunWith(Arquillian.class)
public class JPAIntegrationTest extends CamelTestSupport {

    @Deployment
    public static Archive<?> createDeployment() {
        StringAsset jaxbIndex = new StringAsset("Customer");

        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addPackage(Customer.class.getPackage())
                .addPackage(CamelTestSupport.class.getPackage())
                .addPackage(JpaRouteBuilder.class.getPackage())
                .addPackage(JaxbDataFormat.class.getPackage())
                .addAsResource(jaxbIndex, "org/wildfly/camel/examples/jpa/model/jaxb.index")
                .addAsResource("customer.xml", "org/wildfly/camel/examples/jpa/customer.xml")
                .addAsResource("test-persistence.xml", "META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    EntityManager em;

    @Test
    public void testFileToJpaRoute() throws Exception {
        // Send a test customer XML file to our file endpoint
        template.sendBodyAndHeader("file://input/customers", readResourceFromClasspath("customer.xml"),
                Exchange.FILE_NAME, "customer.xml");

        // Wait for the file to be consumed
        Thread.sleep(2000);

        // Query the in memory database customer table to verify that a record was saved
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        query.select(criteriaBuilder.count(query.from(Customer.class)));

        long recordCount = em.createQuery(query).getSingleResult();

        Assert.assertEquals(1L, recordCount);
    }

    private String readResourceFromClasspath(String resourcePath) throws IOException {
        StringBuilder builder = new StringBuilder();

        BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(resourcePath), "UTF-8"));
        for (int c = br.read(); c != -1; c = br.read()) {
            builder.append((char) c);
        }

        return builder.toString();
    }

}
