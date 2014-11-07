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
package org.wildfly.camel.examples.cxf;

import org.apache.camel.test.junit4.CamelTestSupport;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.examples.HttpRequest;

import java.util.concurrent.TimeUnit;


@RunWith(Arquillian.class)
public class CxfIntegrationTest extends CamelTestSupport {

    private static final String ENDPOINT_ADDRESS = "http://localhost:8080/example-camel-cxf-soap/greeting";
    private static final Logger LOG = LoggerFactory.getLogger(CxfIntegrationTest.class);

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addPackage(CamelTestSupport.class.getPackage())
                .addPackage(HttpRequest.class.getPackage())
                .addAsResource("hello-request.xml", "org/wildfly/camel/examples/cxf/hello-request.xml")
                .addAsResource("goodbye-request.xml", "org/wildfly/camel/examples/cxf/goodbye-request.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testSayHelloCxfSoapRoute() throws Exception {
        // Send HTTP request to greeting service sayHello webservice method
        String result = HttpRequest.get(ENDPOINT_ADDRESS, "/org/wildfly/camel/examples/cxf/hello-request.xml",
                10, TimeUnit.SECONDS);

        // Log SOAP response
        LOG.info("*******************************");
        LOG.info(result);
        LOG.info("*******************************");

        // Verify that a hello response was returned
        Assert.assertTrue(result.contains("Hello John Doe"));
    }

    @Test
    public void testSayGoodbyeCxfSoapRoute() throws Exception {
        // Send HTTP request to greeting service sayGoodbye webservice method
        String result = HttpRequest.get(ENDPOINT_ADDRESS, "/org/wildfly/camel/examples/cxf/goodbye-request.xml",
                10, TimeUnit.SECONDS);

        // Log SOAP response
        LOG.info("*******************************");
        LOG.info(result);
        LOG.info("*******************************");

        // Verify that a goodbye response was returned
        Assert.assertTrue(result.contains("Goodbye John Doe"));
    }
}
