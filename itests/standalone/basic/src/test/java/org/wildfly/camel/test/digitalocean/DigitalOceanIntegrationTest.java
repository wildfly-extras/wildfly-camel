/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.digitalocean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.digitalocean.constants.DigitalOceanHeaders;
import org.apache.camel.component.digitalocean.constants.DigitalOceanOperations;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.PropertiesComponent;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

import com.myjeeva.digitalocean.impl.DigitalOceanClient;
import com.myjeeva.digitalocean.pojo.Account;
import com.myjeeva.digitalocean.pojo.Delete;
import com.myjeeva.digitalocean.pojo.Droplet;
import com.myjeeva.digitalocean.pojo.Image;
import com.myjeeva.digitalocean.pojo.Region;
import com.myjeeva.digitalocean.pojo.Size;
import com.myjeeva.digitalocean.pojo.Tag;

@CamelAware
@RunWith(Arquillian.class)
public class DigitalOceanIntegrationTest {

    private static final String DIGITALOCEAN_OAUTH_TOKEN = "DIGITALOCEAN_OAUTH_TOKEN";

    private String oauthToken;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-digitalocean-tests.jar");
    }

    @Before
    public void beforeClass () {
        oauthToken = System.getenv(DIGITALOCEAN_OAUTH_TOKEN);
        Assume.assumeNotNull("OAuth Token required", oauthToken);
    }

    @AfterClass
    public static void afterClass() {
        String oauthToken = System.getenv(DIGITALOCEAN_OAUTH_TOKEN);
        if (oauthToken != null) {
            DigitalOceanClient client = new DigitalOceanClient(oauthToken);
            try {
                List<Droplet> droplets = client.getAvailableDroplets(1, 10).getDroplets();
                for (Droplet droplet : droplets) {
                    client.deleteDroplet(droplet.getId());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Test
    public void testGetAccountInfo() throws Exception {

        CamelContext camelctx = createCamelContext(oauthToken);
        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            MockEndpoint mockResult = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockResult.expectedMinimumMessageCount(1);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            Account result = producer.requestBody("direct:getAccountInfo", null, Account.class);
            Assert.assertTrue(result.isEmailVerified());
            mockResult.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testCreateDroplet() throws Exception {
        CamelContext camelctx = createCamelContext(oauthToken);
        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            MockEndpoint mockResult = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockResult.expectedMinimumMessageCount(1);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            Droplet droplet = producer.requestBody("direct:createDroplet", null, Droplet.class);

            mockResult.assertIsSatisfied();
            Assert.assertNotNull(droplet.getId());
            Assert.assertEquals(droplet.getRegion().getSlug(), "fra1");
            Assert.assertEquals(2, droplet.getTags().size());

            mockResult.reset();
            mockResult.expectedMinimumMessageCount(1);

            Droplet resDroplet = producer.requestBodyAndHeader("direct:getDroplet", null, DigitalOceanHeaders.ID, droplet.getId(), Droplet.class);
            mockResult.assertIsSatisfied();
            Assert.assertEquals(droplet.getId(), resDroplet.getId());

            Delete delres = producer.requestBodyAndHeader("direct:deleteDroplet", null, DigitalOceanHeaders.ID, droplet.getId(), Delete.class);
            Assert.assertTrue("Droplet deleted", delres.getIsRequestSuccess());
        } finally {
            camelctx.close();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateMultipleDroplets() throws Exception {
        CamelContext camelctx = createCamelContext(oauthToken);
        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            MockEndpoint mockResult = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockResult.expectedMinimumMessageCount(1);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            List<Droplet> createDroplets = producer.requestBody("direct:createMultipleDroplets", null, List.class);

            mockResult.assertIsSatisfied();
            Assert.assertEquals(2, createDroplets.size());
            List<Droplet> getDroplets = producer.requestBody("direct:getDroplets", null, List.class);
            Assert.assertTrue("At least as many droplets as created", getDroplets.size() >= 2);

        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testCreateTag() throws Exception {
        CamelContext camelctx = createCamelContext(oauthToken);
        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            MockEndpoint mockResult = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockResult.expectedMinimumMessageCount(1);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            Tag tag = producer.requestBody("direct:createTag", null, Tag.class);
            Assert.assertEquals("tag1", tag.getName());
            mockResult.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }


    @Test
    @SuppressWarnings("unchecked")
    public void testGetTags() throws Exception {
        CamelContext camelctx = createCamelContext(oauthToken);
        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            MockEndpoint mockResult = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockResult.expectedMinimumMessageCount(1);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            List<Tag> tags = producer.requestBody("direct:getTags", null, List.class);
            Assert.assertEquals("tag1", tags.get(0).getName());
            mockResult.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getImages() throws Exception {
        CamelContext camelctx = createCamelContext(oauthToken);
        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            MockEndpoint mockResult = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockResult.expectedMinimumMessageCount(1);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            List<Image> images = producer.requestBody("direct:getImages", null, List.class);

            mockResult.assertIsSatisfied();
            Assert.assertNotEquals(1, images.size());
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void getImage() throws Exception {
        CamelContext camelctx = createCamelContext(oauthToken);
        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            MockEndpoint mockResult = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockResult.expectedMinimumMessageCount(1);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            Image image = producer.requestBody("direct:getImage", null, Image.class);
            Assert.assertEquals("ubuntu-14-04-x64", image.getSlug());
            mockResult.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getSizes() throws Exception {
        CamelContext camelctx = createCamelContext(oauthToken);
        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            MockEndpoint mockResult = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockResult.expectedMinimumMessageCount(1);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            List<Size> sizes = producer.requestBody("direct:getSizes", null, List.class);
            Assert.assertNotEquals(1, sizes.size());
            mockResult.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }


    @Test
    @SuppressWarnings("unchecked")
    public void getRegions() throws Exception {
        CamelContext camelctx = createCamelContext(oauthToken);
        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            MockEndpoint mockResult = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockResult.expectedMinimumMessageCount(1);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            List<Region> regions = producer.requestBody("direct:getRegions", null, List.class);
            Assert.assertNotEquals(1, regions.size());
            mockResult.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }


    private RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {

                from("direct:getAccountInfo")
                .to("digitalocean:account?operation=" + DigitalOceanOperations.get + "&oAuthToken={{oAuthToken}}")
                .to("mock:result");

                from("direct:createMultipleDroplets")
                .setHeader(DigitalOceanHeaders.OPERATION, constant(DigitalOceanOperations.create))
                .process(e -> {
                    Collection<String> names = new ArrayList<String>();
                    names.add("droplet1");
                    names.add("droplet2");
                    e.getIn().setHeader(DigitalOceanHeaders.NAMES, names);

                })
                .setHeader(DigitalOceanHeaders.REGION, constant("fra1"))
                .setHeader(DigitalOceanHeaders.DROPLET_IMAGE, constant("ubuntu-14-04-x64"))
                .setHeader(DigitalOceanHeaders.DROPLET_SIZE, constant("512mb"))
                .process(e -> {
                    Collection<String> tags = new ArrayList<String>();
                    tags.add("tag1");
                    tags.add("tag2");
                    e.getIn().setHeader(DigitalOceanHeaders.DROPLET_TAGS, tags);

                })
                .to("digitalocean://droplets?oAuthToken={{oAuthToken}}")
                .to("mock:result");

                from("direct:getTags")
                .setHeader(DigitalOceanHeaders.OPERATION, constant(DigitalOceanOperations.list))
                .to("digitalocean://tags?oAuthToken={{oAuthToken}}")
                .to("mock:result");

                from("direct:getImages")
                .setHeader(DigitalOceanHeaders.OPERATION, constant(DigitalOceanOperations.list))
                .to("digitalocean://images?oAuthToken={{oAuthToken}}")
                .to("mock:result");

                from("direct:createDroplet")
                .setHeader(DigitalOceanHeaders.OPERATION, constant(DigitalOceanOperations.create))
                .setHeader(DigitalOceanHeaders.NAME, constant("camel-test"))
                .setHeader(DigitalOceanHeaders.REGION, constant("fra1"))
                .setHeader(DigitalOceanHeaders.DROPLET_IMAGE, constant("ubuntu-14-04-x64"))
                .setHeader(DigitalOceanHeaders.DROPLET_SIZE, constant("512mb"))
                .process(e -> {
                    Collection<String> tags = new ArrayList<String>();
                    tags.add("tag1");
                    tags.add("tag2");
                    e.getIn().setHeader(DigitalOceanHeaders.DROPLET_TAGS, tags);

                })
                .to("digitalocean:droplets?oAuthToken={{oAuthToken}}")
                .to("mock:result");

                from("direct:getDroplet")
                .to("digitalocean:droplets?operation=get&oAuthToken={{oAuthToken}}")
                .to("mock:result");

                from("direct:deleteDroplet")
                .to("digitalocean:droplets?operation=delete&oAuthToken={{oAuthToken}}")
                .to("mock:result");

                from("direct:createTag")
                .setHeader(DigitalOceanHeaders.OPERATION, constant(DigitalOceanOperations.create))
                .setHeader(DigitalOceanHeaders.NAME, constant("tag1"))
                .to("digitalocean://tags?oAuthToken={{oAuthToken}}")
                .to("mock:result");

                from("direct:getImage")
                .setHeader(DigitalOceanHeaders.OPERATION, constant(DigitalOceanOperations.get))
                .setHeader(DigitalOceanHeaders.DROPLET_IMAGE, constant("ubuntu-14-04-x64"))
                .to("digitalocean://images?oAuthToken={{oAuthToken}}")
                .to("mock:result");

                from("direct:getSizes")
                .setHeader(DigitalOceanHeaders.OPERATION, constant(DigitalOceanOperations.list))
                .to("digitalocean://sizes?oAuthToken={{oAuthToken}}")
                .to("mock:result");

                from("direct:getRegions")
                .setHeader(DigitalOceanHeaders.OPERATION, constant(DigitalOceanOperations.list))
                .to("digitalocean://regions?oAuthToken={{oAuthToken}}")
                .to("mock:result");

                from("direct:getDroplets")
                .setHeader(DigitalOceanHeaders.OPERATION, constant(DigitalOceanOperations.list))
                .to("digitalocean:droplets?oAuthToken={{oAuthToken}}")
                .to("mock:result");
            }
        };
    }

    private CamelContext createCamelContext(String oauthToken) {
        CamelContext camelctx = new DefaultCamelContext();
        PropertiesComponent pc = camelctx.getPropertiesComponent();
        Properties properties = new Properties();
        properties.setProperty("oAuthToken", oauthToken);
        pc.setOverrideProperties(properties);
        return camelctx;
    }
}
