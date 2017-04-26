/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2016 RedHat
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
package org.wildfly.camel.test.sap.netweaver;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sap.netweaver.NetWeaverConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

/*
 * To run this test you need to set the following environment variables:
 *
 *   SAP_USERNAME
 *   SAP_PASSWORD
 */
@CamelAware
@RunWith(Arquillian.class)
public class SAPNetweaverIntegrationTest {

    static final String SAP_GATEWAY_URL = "https4://sapes4.sapdevcenter.com/sap/opu/odata/IWFND/RMTSAMPLEFLIGHT";
    
    static final String SAP_USERNAME = System.getenv("SAP_USERNAME");
    static final String SAP_PASSWORD = System.getenv("SAP_PASSWORD");

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-sap-netweaver-tests.jar");
    }

    @Test
    public void testSAPNetweaverEndpoint() throws Exception {

        Assume.assumeTrue("[#1675] Enable SAP testing in Jenkins", SAP_USERNAME != null && SAP_PASSWORD != null);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("sap-netweaver:%s?username=%s&password=%s", SAP_GATEWAY_URL, SAP_USERNAME, SAP_PASSWORD);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            ConsumerTemplate consumer = camelctx.createConsumerTemplate();

            // Flight data is constantly updated, so fetch a valid flight from the flight collection feed
            String sapRssFeedUri = String.format("rss:%s/%s?username=%s&password=%s", SAP_GATEWAY_URL.replace("https4", "https"),
                "FlightCollection", SAP_USERNAME, SAP_PASSWORD);
            SyndFeed feed = consumer.receiveBody(sapRssFeedUri, SyndFeed.class);
            Assert.assertNotNull(feed);
            Assert.assertTrue(feed.getEntries().size() > 0);

            SyndEntry entry = (SyndEntry) feed.getEntries().get(0);
            String sapCommand = entry.getTitle();
            String result = producer.requestBodyAndHeader("direct:start", null, NetWeaverConstants.COMMAND, sapCommand, String.class);
            Assert.assertFalse(result.isEmpty());
        } finally {
            camelctx.stop();
        }
    }
}
