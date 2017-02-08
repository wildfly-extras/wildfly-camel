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

package org.wildfly.camel.test.snmp;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.snmp.SnmpMessage;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.snmp4j.PDU;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.extension.camel.CamelAware;

@RunWith(Arquillian.class)
@CamelAware
public class SNMPIntegrationTest {

    private static final String SNMP_OIDS = "1.3.6.1.2.1.1.3.0,1.3.6.1.2.1.25.3.2.1.5.1,1.3.6.1.2.1.25.3.5.1.1.1,1.3.6.1.2.1.43.5.1.1.11.1";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-snmp-tests.jar")
            .addClass(AvailablePortFinder.class);
    }

    @Test
    public void testSnmpEndpoint() throws Exception {
        int port = AvailablePortFinder.getNextAvailable();

        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("snmp://localhost:%d?protocol=tcp&type=TRAP", port)
                .to("mock:result");
            }
        });

        camelctx.start();
        try {
            String uri = String.format("snmp://localhost:%d?oids=%s&protocol=tcp", port, SNMP_OIDS);

            ProducerTemplate template = camelctx.createProducerTemplate();
            SnmpMessage message = template.requestBody(uri, null, SnmpMessage.class);
            PDU snmpMessage = message.getSnmpMessage();

            Assert.assertNotNull(snmpMessage);
            Assert.assertEquals(0, snmpMessage.getErrorStatus());
        } finally {
            camelctx.stop();
        }
    }
}
