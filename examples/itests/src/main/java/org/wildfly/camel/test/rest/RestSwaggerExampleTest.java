/*
 * #%L
 * Wildfly Camel :: Example :: Camel Rest Swagger
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
package org.wildfly.camel.test.rest;

import java.io.File;
import java.net.MalformedURLException;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.camel.test.common.utils.TestUtils;

@RunAsClient
@RunWith(Arquillian.class)
public class RestSwaggerExampleTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.createFromZipFile(WebArchive.class, new File("target/examples/example-camel-rest-swagger.war"));
    }

    @Test
    public void testCustomerCreate() throws Exception {
        String json = TestUtils.getResourceValue(RestSwaggerExampleTest.class, "/rest/customer.json");

        HttpResponse response = HttpRequest.post(getResourceAddress("/customers"))
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .content(json)
            .getResponse();

        Assert.assertEquals(201, response.getStatusCode());
        Assert.assertTrue(response.getBody().matches("\\{\"id\":[0-9],\"firstName\":\"John\",\"lastName\":\"Doe\"}"));
    }

    @Test
    public void testCustomerUpdate() throws Exception {
        String json = TestUtils.getResourceValue(RestSwaggerExampleTest.class, "/rest/customer.json");

        // Create customer
        HttpResponse response = HttpRequest.post(getResourceAddress("/customers"))
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .content(json)
            .getResponse();
        String id = response.getBody().replaceAll("[^0-9]", "");

        Assert.assertEquals(201, response.getStatusCode());

        // Update customer
        String updatedJson = response.getBody().replace("John", "Bob");

        response = HttpRequest.put(getResourceAddress("/customers/" + id))
            .content(updatedJson)
            .getResponse();

        Assert.assertEquals(204, response.getStatusCode());

        // Fetch updated
        response = HttpRequest.get(getResourceAddress("/customers/" + id)).getResponse();
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertEquals(updatedJson, response.getBody());
    }

    @Test
    public void testCustomerDelete() throws Exception {
        String json = TestUtils.getResourceValue(RestSwaggerExampleTest.class, "/rest/customer.json");

        // Create customer
        HttpResponse response = HttpRequest.post(getResourceAddress("/customers"))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .content(json)
                .getResponse();

        Assert.assertEquals(201, response.getStatusCode());

        // Delete customer
        String id = response.getBody().replaceAll("[^0-9]", "");
        response = HttpRequest.delete(getResourceAddress("/customers/" + id)).getResponse();
        Assert.assertEquals(204, response.getStatusCode());
    }

    @Test
    public void testCustomerFindById() throws Exception {
        String json = TestUtils.getResourceValue(RestSwaggerExampleTest.class, "/rest/customer.json");

        // Create customer
        HttpResponse response = HttpRequest.post(getResourceAddress("/customers"))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .content(json)
                .getResponse();

        Assert.assertEquals(201, response.getStatusCode());

        // Find customer
        String createdJson = response.getBody();
        String id = createdJson.replaceAll("[^0-9]", "");
        response = HttpRequest.get(getResourceAddress("/customers/" + id)).getResponse();

        Assert.assertEquals(createdJson, response.getBody());
    }

    @Test
    public void testCustomerFindWithInvalidId() throws Exception {
        HttpResponse response = HttpRequest.get(getResourceAddress("/customers/9999")).getResponse();
        Assert.assertEquals(404, response.getStatusCode());
    }

    @Test
    public void testCustomerFindAll() throws Exception {
        String json = TestUtils.getResourceValue(RestSwaggerExampleTest.class, "/rest/customer.json");

        // Create customers
        HttpRequest.post(getResourceAddress("/customers"))
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .content(json)
            .getResponse();

        HttpRequest.post(getResourceAddress("/customers"))
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .content(json)
            .getResponse();

        // Get all customers
        HttpResponse response = HttpRequest.get(getResourceAddress("/customers")).getResponse();
        Assert.assertTrue(response.getBody().split("},").length >= 2);
    }

    @Test
    public void testPostBadData() throws Exception {
        HttpResponse response = HttpRequest.post(getResourceAddress("/customers"))
            .throwExceptionOnFailure(false)
            .content("some bad content which should be rejected")
            .getResponse();

        Assert.assertEquals(400, response.getStatusCode());
    }

    @Test
    public void testSwaggerEndpoint() throws Exception {
        HttpResponse response = HttpRequest.get(getResourceAddress("/swagger")).getResponse();
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertTrue(response.getBody().contains("\"title\" : \"WildFly Camel REST API\""));
    }

    @Test
    public void testSwaggerUI() throws Exception {
        HttpResponse response = HttpRequest.get("http://127.0.0.1:8080/example-camel-rest-swagger/").getResponse();
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertTrue(response.getBody().contains("Swagger UI"));
    }

    private String getResourceAddress(String resourcePath) throws MalformedURLException {
        return "http://localhost:8080/rest/api" + resourcePath;
    }
}
