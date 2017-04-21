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

import facebook4j.Page;

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

    private static final String REDHAT_PAGE_ID = "112169325462421";
    private static String FACEBOOK_APP_ID = System.getenv("FACEBOOK_APP_ID");
    private static String FACEBOOK_APP_SECRET = System.getenv("FACEBOOK_APP_SECRET");
    private static String FACEBOOK_ACCESS_TOKEN = System.getenv("FACEBOOK_ACCESS_TOKEN");

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-facebook-tests.jar");
    }

    @Test
    public void testFacebookComponent() throws Exception {
        Assume.assumeNotNull("Facebook credentials are null", FACEBOOK_APP_ID, FACEBOOK_APP_SECRET, FACEBOOK_ACCESS_TOKEN);

        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("facebook://getPage?oAuthAppId=%s&oAuthAppSecret=%s&oAuthAccessToken=%s&pageId=%s", FACEBOOK_APP_ID,
                    FACEBOOK_APP_SECRET, FACEBOOK_ACCESS_TOKEN, REDHAT_PAGE_ID);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            Page page = template.requestBody("direct:start", (Object)null, Page.class);

            Assert.assertNotNull("RedHat Facebook page response is null", page);
            Assert.assertEquals("Red Hat", page.getName());
        } finally {
            camelctx.stop();
        }
    }
}
