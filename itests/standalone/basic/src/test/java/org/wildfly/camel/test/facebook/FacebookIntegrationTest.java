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

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.facebook.FacebookComponent;
import org.apache.camel.component.facebook.config.FacebookConfiguration;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.camel.test.facebook.subA.FakeFacebookAPIServlet;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class FacebookIntegrationTest {

    private static final String FACEBOOK_APP_ID = "fake-app";
    private static final String FACEBOOK_APP_SECRET = "fake-secret";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "camel-facebook-tests.war")
            .addClasses(FakeFacebookAPIServlet.class, TestUtils.class)
            .addAsResource("facebook/facebook-token-response.json", "facebook-token.json")
            .addAsResource("facebook/facebook-test-users-response.json", "facebook-test-users.json");
    }

    @Test
    public void testFacebookComponent() throws Exception {
        FacebookComponent component = new FacebookComponent();
        FacebookConfiguration configuration = component.getConfiguration();

        String baseURL = "http://localhost:8080/camel-facebook-tests/fake-facebook-api";
        configuration.setClientURL(baseURL);
        configuration.setOAuthAccessTokenURL(baseURL + "/oauth-token");
        configuration.setRestBaseURL(baseURL + "/rest");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addComponent("facebook", component);

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
            PagableList testUserList = template.requestBody("direct:start", null, PagableList.class);
            Assert.assertNotNull("Facebook app test user list was null", testUserList);
        } finally {
            camelctx.stop();
        }
    }
}
