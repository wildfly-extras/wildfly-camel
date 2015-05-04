/*
 * #%L
 * Wildfly Camel :: Example :: Camel REST
 * %%
 * Copyright (C) 2013 - 2014 RedHat
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
package org.wildfly.camel.examples.rest;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.ws.rs.core.MediaType;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.HttpRequest;
import org.wildfly.camel.test.common.HttpRequest.HttpResponse;


@RunWith(Arquillian.class)
@RunAsClient
public class RestExampleTest {

    @After
    public void tearDown() throws Exception {
        HttpResponse response = HttpRequest.delete(getEndpointAddress("/rest/customer/"))
                .throwExceptionOnFailure(false)
                .getResponse();
        Assert.assertEquals(200, response.getStatusCode());
    }

    @Test
    public void testCreateCustomer() throws Exception {
        HttpResponse result = createCustomer();

        Assert.assertEquals(200, result.getStatusCode());
        Assert.assertTrue(result.getBody().contains("\"firstName\":\"John\",\"lastName\":\"Doe\""));
    }

    @Test
    public void testReadAllCustomers() throws Exception {
        String customersBefore = HttpRequest.get(getEndpointAddress("/rest/customer/")).getResponse().getBody();
        String customersAfter = createCustomer().getBody();

        Assert.assertEquals("[]", customersBefore);
        Assert.assertTrue(customersAfter.contains("\"firstName\":\"John\",\"lastName\":\"Doe\""));
    }

    @Test
    public void testReadCustomer() throws Exception {
        HttpResponse createCustomerResponse = createCustomer();

        String responseBody = createCustomerResponse.getBody();
        String customerId = getCustomerIdFromJson(responseBody.split(":")[1]);

        HttpResponse getCustomerResponse = HttpRequest.get(getEndpointAddress("/camel/customer/" + customerId))
            .getResponse();

        Assert.assertEquals(200, getCustomerResponse.getStatusCode());
        Assert.assertTrue(getCustomerResponse.getBody().contains("\"firstName\":\"John\",\"lastName\":\"Doe\""));
    }

    @Test
    public void testReadNonExistentCustomer() throws Exception {
        HttpResponse getCustomerResponse = HttpRequest.get(getEndpointAddress("/camel/customer/99"))
            .throwExceptionOnFailure(false)
            .getResponse();

        Assert.assertEquals(404, getCustomerResponse.getStatusCode());
    }

    @Test
    public void testUpdateCustomer() throws Exception {
        HttpResponse newCustomerResponse = createCustomer();

        // Update the first name / last name
        String customerJson = newCustomerResponse.getBody()
            .replace("John", "Foo")
            .replace("Doe", "Bar");

        HttpResponse updateCustomerResponse = HttpRequest.put(getEndpointAddress("/rest/customer/"))
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .content(customerJson)
            .getResponse();

        Assert.assertEquals(200, updateCustomerResponse.getStatusCode());
    }

    @Test
    public void testUpdateUnmodifiedCustomer() throws Exception {
        HttpResponse newCustomerResponse = createCustomer();

        HttpResponse updateCustomerResponse = HttpRequest.put(getEndpointAddress("/rest/customer/"))
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .content(newCustomerResponse.getBody())
            .throwExceptionOnFailure(false)
            .getResponse();

        Assert.assertEquals(304, updateCustomerResponse.getStatusCode());
    }

    @Test
    public void testUpdateNonExistentCustomer() throws Exception {
        HttpResponse updateCustomerResponse = HttpRequest.put(getEndpointAddress("/rest/customer/"))
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .content(readFileFromClasspath("/update-customer.json"))
            .throwExceptionOnFailure(false)
            .getResponse();

        Assert.assertEquals(404, updateCustomerResponse.getStatusCode());
    }

    @Test
    public void testDeleteCustomer() throws Exception {
        HttpResponse createCustomerResponse = createCustomer();

        String responseBody = createCustomerResponse.getBody();
        String customerId = getCustomerIdFromJson(responseBody.split(":")[1]);

        HttpResponse deleteCustomerResponse = HttpRequest.delete(getEndpointAddress("/rest/customer/" + customerId))
            .throwExceptionOnFailure(false)
            .getResponse();

        Assert.assertEquals(200, deleteCustomerResponse.getStatusCode());
    }

    @Test
    public void testDeleteNonExistentCustomer() throws Exception {
        HttpResponse response = HttpRequest.delete(getEndpointAddress("/rest/customer/99"))
            .throwExceptionOnFailure(false)
            .getResponse();
        Assert.assertEquals(404, response.getStatusCode());
    }

    @Test
    public void testGreet() throws Exception {
        HttpResponse response = HttpRequest.get(getEndpointAddress("/rest/greet/hello/Kermit")).getResponse();
        Assert.assertTrue(response.getBody().startsWith("Hello Kermit"));
    }

    private HttpResponse createCustomer() throws Exception {
        return HttpRequest.post(getEndpointAddress("/camel/customer/"))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .content(readFileFromClasspath("/create-customer.json"))
                .getResponse();
    }

    private String getCustomerIdFromJson(String s) {
        return s.replaceAll("[^\\d.]", "");
    }

    private String readFileFromClasspath(String filePath) throws Exception {
        return new String(Files.readAllBytes(Paths.get(getClass().getResource(filePath).toURI())));
    }

    private String getEndpointAddress(String restPath) throws MalformedURLException {
        return "http://localhost:8080/example-camel-rest" + restPath;
    }
}
