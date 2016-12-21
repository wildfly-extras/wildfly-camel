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

import org.apache.camel.CamelContext;
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

    private static final String SAP_GATEWAY_URL = "https4://sapes4.sapdevcenter.com/sap/opu/odata/IWFND/RMTSAMPLEFLIGHT";
    private static final String SAP_COMMAND = "FlightCollection(carrid='AA',connid='0017',fldate=datetime'2016-04-20T00%3A00%3A00')";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-sap-netweaver-tests.jar");
    }

    @Test
    public void testSAPNetweaverEndpoint() throws Exception {
        String username = System.getenv("SAP_USERNAME");
        String password = System.getenv("SAP_PASSWORD");

        Assume.assumeTrue(username != null && password != null);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("sap-netweaver:%s?username=%s&password=%s", SAP_GATEWAY_URL, username, password);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            String result = template.requestBodyAndHeader("direct:start", null, NetWeaverConstants.COMMAND, SAP_COMMAND, String.class);
            Assert.assertTrue(result.contains("PRICE=422.94, CURRENCY=USD"));
        } finally {
            camelctx.stop();
        }
    }
}
