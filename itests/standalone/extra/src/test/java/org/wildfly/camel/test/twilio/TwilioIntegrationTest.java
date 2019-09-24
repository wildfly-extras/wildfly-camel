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
package org.wildfly.camel.test.twilio;

import com.twilio.rest.api.v2010.Account;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.twilio.TwilioComponent;
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

@CamelAware
@RunWith(Arquillian.class)
public class TwilioIntegrationTest {

    private static final String TWILIO_ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    private static final String TWILIO_PASSWORD = System.getenv("TWILIO_PASSWORD");

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-twilio-tests");
    }

    @Test
    public void testTwilioProducer() throws Exception {
        Assume.assumeNotNull("TWILIO_ACCOUNT_SID is null", TWILIO_ACCOUNT_SID);
        Assume.assumeNotNull("TWILIO_PASSWORD is null", TWILIO_PASSWORD);

        CamelContext camelctx = new DefaultCamelContext();

        TwilioComponent component = camelctx.getComponent("twilio", TwilioComponent.class);
        component.setUsername(TWILIO_ACCOUNT_SID);
        component.setPassword(TWILIO_PASSWORD);
        component.setAccountSid(TWILIO_ACCOUNT_SID);

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct://start").to("twilio://account/fetch");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            Account account = template.requestBodyAndHeader("direct:start", null,
                    "CamelTwilioPathSid", TWILIO_ACCOUNT_SID,
                    Account.class);

            Assert.assertNotNull("Twilio fetcher result was null", account);
            Assert.assertEquals("Account SID did not match", TWILIO_ACCOUNT_SID, account.getSid());
        } finally {
            camelctx.close();
        }
    }

}
