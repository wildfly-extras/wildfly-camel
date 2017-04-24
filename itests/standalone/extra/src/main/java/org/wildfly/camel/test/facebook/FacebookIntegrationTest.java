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
package org.wildfly.camel.test.facebook;

import facebook4j.PagableList;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
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
public class FacebookIntegrationTest {

    private static String FACEBOOK_APP_ID = System.getenv("FACEBOOK_APP_ID");
    private static String FACEBOOK_APP_SECRET = System.getenv("FACEBOOK_APP_SECRET");

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-facebook-tests.jar");
    }

    @Test
    public void testFacebookComponent() throws Exception {
        Assume.assumeNotNull("Facebook credentials are null", FACEBOOK_APP_ID, FACEBOOK_APP_SECRET);

        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("facebook://getTestUsers?oAuthAppId=%s&oAuthAppSecret=%s&appId=%s", FACEBOOK_APP_ID,
                    FACEBOOK_APP_SECRET, FACEBOOK_APP_ID);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            PagableList testUserList = template.requestBody("direct:start", (Object)null, PagableList.class);
            Assert.assertNotNull("Facebook app test user list was null", testUserList);
        } finally {
            camelctx.stop();
        }
    }
}
