/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wildfly.camel.test.twitter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.ObjectHelper;
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
public class TwitterIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() throws Exception {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-twitter-tests");
        return archive;
    }

    @Test
    public void testPostStatusUpdate() throws Exception {

        final CamelTwitterSupport twitter = new CamelTwitterSupport();

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("twitter-timeline://user?" + twitter.getUriTokens());
            }
        });

        Assume.assumeTrue("[#1672] Enable Twitter testing in Jenkins", twitter.hasUriTokens());

        camelctx.start();
        try {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss dd-MMM-yyyy");
            String message = "Its a good day to test Twitter on " + format.format(new Date());
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", message, String.class);
            Assert.assertTrue("Unexpected: " + result, result.contains("(wfcameltest) Its a good day to test Twitter"));
        } finally {
            camelctx.stop();
        }
    }

    static class CamelTwitterSupport {

        private String consumerKey;
        private String consumerSecret;
        private String accessToken;
        private String accessTokenSecret;

        CamelTwitterSupport() {
            Properties properties = new Properties();

            // Load from env
            addProperty(properties, "consumer.key", "CAMEL_TWITTER_CONSUMER_KEY");
            addProperty(properties, "consumer.secret", "CAMEL_TWITTER_CONSUMER_SECRET");
            addProperty(properties, "access.token", "CAMEL_TWITTER_ACCESS_TOKEN");
            addProperty(properties, "access.token.secret", "CAMEL_TWITTER_ACCESS_TOKEN_SECRET");

            // if any of the properties is not set, load test-options.properties
            URL resurl = getClass().getResource("/test-options.properties");
            if (resurl != null) {
                try (InputStream inStream = resurl.openStream()) {
                    properties.load(inStream);
                } catch (IOException ex) {
                    throw new IllegalAccessError("test-options.properties could not be found");
                }
            }

            consumerKey = properties.getProperty("consumer.key");
            consumerSecret = properties.getProperty("consumer.secret");
            accessToken = properties.getProperty("access.token");
            accessTokenSecret = properties.getProperty("access.token.secret");
        }

        boolean hasUriTokens() {
            return consumerKey != null && consumerSecret != null && accessToken != null && accessTokenSecret != null;
        }

        String getUriTokens() {
            return "consumerKey=" + consumerKey
                + "&consumerSecret=" + consumerSecret
                + "&accessToken=" + accessToken
                + "&accessTokenSecret=" + accessTokenSecret;
        }

        private void addProperty(Properties properties, String name, String envName) {
            if (!properties.containsKey(name)) {
                String value = System.getenv(envName);
                if (ObjectHelper.isNotEmpty(value)) {
                    properties.setProperty(name, value);
                }
            }
        }
    }
}
