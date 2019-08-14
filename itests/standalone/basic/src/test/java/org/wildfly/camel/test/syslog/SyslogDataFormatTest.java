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

package org.wildfly.camel.test.syslog;

import java.util.Calendar;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.syslog.SyslogDataFormat;
import org.apache.camel.component.syslog.SyslogMessage;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class SyslogDataFormatTest {

    private final String SYSLOG_RAW_MESSAGE = "<14>Sep 26 19:30:55 camel-test-host Hello Kermit!";


    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-syslog-tests.jar")
            .addClass(AvailablePortFinder.class);
    }

    @Test
    public void testSyslogMarshal() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .marshal(new SyslogDataFormat());
            }
        });

        camelctx.start();
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.set(2016, Calendar.SEPTEMBER, 26, 19, 30, 55);

            SyslogMessage message = new SyslogMessage();
            message.setHostname("camel-test-host");
            message.setLogMessage("Hello Kermit!");
            message.setTimestamp(calendar);

            ProducerTemplate template = camelctx.createProducerTemplate();
            String result = template.requestBody("direct:start", message, String.class);

            Assert.assertEquals(SYSLOG_RAW_MESSAGE, result);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testSyslogUnmarshal() throws Exception {
        int port = AvailablePortFinder.getNextAvailable();

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("netty:udp://localhost:" + port + "?sync=false&allowDefaultCodec=false")
                .unmarshal(new SyslogDataFormat())
                .to("mock:result");
            }
        });

        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(1);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.requestBody("netty:udp://127.0.0.1:" + port + "?sync=false&allowDefaultCodec=false&useByteBuf=true", SYSLOG_RAW_MESSAGE);

            mockEndpoint.assertIsSatisfied();

            Exchange exchange = mockEndpoint.getReceivedExchanges().get(0);
            SyslogMessage message = exchange.getIn().getBody(SyslogMessage.class);

            Assert.assertEquals("camel-test-host", message.getHostname());
            Assert.assertEquals("Hello Kermit!", message.getLogMessage());
        } finally {
            camelctx.stop();
        }
    }
}
