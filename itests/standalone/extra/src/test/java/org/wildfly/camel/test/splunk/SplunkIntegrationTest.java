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

package org.wildfly.camel.test.splunk;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.splunk.event.SplunkEvent;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class SplunkIntegrationTest {

    @Deployment
    public static JavaArchive createdeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-splunk-tests");
        return archive;
    }

    @Test
    public void testSalesforceQuery() throws Exception {

        String SPLUNK_USERNAME = System.getenv("SPLUNK_USERNAME");
        String SPLUNK_PASSWORD = System.getenv("SPLUNK_PASSWORD");
        Assume.assumeNotNull("[#1673] Enable Splunk testing in Jenkins", SPLUNK_USERNAME, SPLUNK_PASSWORD);

        SplunkEvent splunkEvent = new SplunkEvent();
        splunkEvent.addPair("key1", "value1");
        splunkEvent.addPair("key2", "value2");
        splunkEvent.addPair("key3", "value1");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:submit")
                .to("splunk://submit?username=" + SPLUNK_USERNAME + "&password=" + SPLUNK_PASSWORD + "&sourceType=testSource&source=test")
                .to("mock:result");
            }
        });

        MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mock.expectedMinimumMessageCount(1);

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.sendBody("direct:submit", splunkEvent);
            mock.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }
}
