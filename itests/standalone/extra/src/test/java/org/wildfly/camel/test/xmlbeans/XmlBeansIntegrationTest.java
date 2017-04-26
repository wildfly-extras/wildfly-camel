/*
 * #%L
 * Wildfly Camel :: Testsuite
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

package org.wildfly.camel.test.xmlbeans;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.camel.xmlbeans.Customer;
import org.wildfly.camel.xmlbeans.LineItem;
import org.wildfly.camel.xmlbeans.PurchaseOrderDocument;
import org.wildfly.camel.xmlbeans.PurchaseOrderDocument.PurchaseOrder;
import org.wildfly.camel.xmlbeans.impl.PurchaseOrderDocumentImpl;
import org.wildfly.extension.camel.CamelAware;


@CamelAware
@RunWith(Arquillian.class)
public class XmlBeansIntegrationTest {

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "xmlbeans-tests.war");
        archive.addAsResource("xmlbeans/xml/purchaseOrder.xml", "purchaseOrder.xml");
        archive.addAsResource("xmlbeans/xsd/purchaseOrder.xsd", "org/wildfly/camel/xsd/purchaseOrder.xsd");
        // [#643] Cannot add schemaorg_apache_xmlbeans when sourced from jar
        archive.addAsResource(new File("target/generated-classes/xmlbeans/schemaorg_apache_xmlbeans"));
        archive.addPackage(PurchaseOrderDocument.class.getPackage());
        archive.addPackage(PurchaseOrderDocumentImpl.class.getPackage());
        archive.addClasses(TestUtils.class);
        return archive;
    }

    @Test
    public void testXmlBeansUnmarshal() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                        .unmarshal().xmlBeans();
            }
        });

        try {
            camelctx.start();

            String orderXml = readPurchaseOrderXml();

            ProducerTemplate producer = camelctx.createProducerTemplate();
            PurchaseOrderDocument purchaseOrderDocument = producer.requestBody("direct:start", orderXml, PurchaseOrderDocument.class);

            PurchaseOrder purchaseOrder = purchaseOrderDocument.getPurchaseOrder();
            Customer customer = purchaseOrder.getCustomer();
            LineItem[] lineItems = purchaseOrder.getLineItemArray();

            Assert.assertEquals(customer.getName(), "John Doe");
            Assert.assertEquals(customer.getAddress(), "Foo, Bar");
            Assert.assertEquals(lineItems.length, 2);
            Assert.assertEquals(lineItems[0].getDescription(), "Bird Food");
            Assert.assertEquals(lineItems[0].getPrice(), new BigDecimal("10.99"));
            Assert.assertEquals(lineItems[0].getQuantity(), new BigInteger("5"));
            Assert.assertEquals(lineItems[1].getDescription(), "Cat Food");
            Assert.assertEquals(lineItems[1].getPrice(), new BigDecimal("5.99"));
            Assert.assertEquals(lineItems[1].getQuantity(), new BigInteger("4"));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testXmlBeansMarshal() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .marshal().xmlBeans();
            }
        });

        try {
            camelctx.start();

            PurchaseOrderDocument purchaseOrderDocument = PurchaseOrderDocument.Factory.newInstance();
            PurchaseOrder purchaseOrder = purchaseOrderDocument.addNewPurchaseOrder();
            Customer customer = purchaseOrder.addNewCustomer();
            customer.setName("John Doe");
            customer.setAddress("Foo, Bar");

            LineItem birdFoodLineItem = purchaseOrder.addNewLineItem();
            birdFoodLineItem.setDescription("Bird Food");
            birdFoodLineItem.setPrice(new BigDecimal("10.99"));
            birdFoodLineItem.setQuantity(new BigInteger("5"));

            LineItem catFoodLineItem = purchaseOrder.addNewLineItem();
            catFoodLineItem.setDescription("Cat Food");
            catFoodLineItem.setPrice(new BigDecimal("5.99"));
            catFoodLineItem.setQuantity(new BigInteger("4"));

            String expected = readPurchaseOrderXml();

            ProducerTemplate producer = camelctx.createProducerTemplate();
            String actual = producer.requestBody("direct:start", purchaseOrderDocument, String.class);

            Assert.assertEquals(expected.replaceAll("\\s+", ""), actual.replaceAll("\\s+",""));
        } finally {
            camelctx.stop();
        }
    }

    private String readPurchaseOrderXml() throws Exception {
        return TestUtils.getResourceValue(getClass(), "/purchaseOrder.xml");
    }
}
