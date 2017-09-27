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

package org.wildfly.camel.test.olingo4;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.olingo4.Olingo4Component;
import org.apache.camel.component.olingo4.Olingo4Configuration;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.IntrospectionSupport;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.domain.ClientComplexValue;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.domain.ClientObjectFactory;
import org.apache.olingo.client.api.domain.ClientPrimitiveValue;
import org.apache.olingo.client.api.domain.ClientServiceDocument;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class Olingo4IntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(Olingo4IntegrationTest.class);

    private static final String TEST_SERVICE_BASE_URL = "http://services.odata.org/TripPinRESTierService";
    private final ODataClient odataClient = ODataClientFactory.getClient();
    private final ClientObjectFactory objFactory = odataClient.getObjectFactory();

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-olingo4-tests");
        return archive;
    }

    @Test
    public void testRead() throws Exception {
        
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() {
                // test routes for read
                from("direct://readmetadata").to("olingo4://read/$metadata");

                from("direct://readdocument").to("olingo4://read/");

                from("direct://readentities").to("olingo4://read/People?$top=5&$orderby=FirstName asc");

                from("direct://readcount").to("olingo4://read/People/$count");

                from("direct://readvalue").to("olingo4://read/People('russellwhyte')/Gender/$value");

                from("direct://readsingleprop").to("olingo4://read/Airports('KSFO')/Name");

                from("direct://readcomplexprop").to("olingo4://read/Airports('KSFO')/Location");

                from("direct://readentitybyid").to("olingo4://read/People('russellwhyte')");

                from("direct://callunboundfunction").to("olingo4://read/GetNearestAirport(lat=33,lon=-118)");
            }
        });
        camelctx.start();
        
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            final Map<String, Object> headers = new HashMap<String, Object>();

            // Read metadata ($metadata) object
            final Edm metadata = (Edm) template.requestBodyAndHeaders("direct://readmetadata", null, headers);
            Assert.assertNotNull(metadata);
            Assert.assertEquals(1, metadata.getSchemas().size());

            // Read service document object
            final ClientServiceDocument document = (ClientServiceDocument) template.requestBodyAndHeaders("direct://readdocument", null, headers);

            Assert.assertNotNull(document);
            Assert.assertTrue(document.getEntitySets().size() > 1);
            LOG.info("Service document has {} entity sets", document.getEntitySets().size());

            // Read entity set of the People object
            final ClientEntitySet entities = (ClientEntitySet) template.requestBodyAndHeaders("direct://readentities", null, headers);
            Assert.assertNotNull(entities);
            Assert.assertEquals(5, entities.getEntities().size());

            // Read object count with query options passed through header
            final Long count = (Long) template.requestBodyAndHeaders("direct://readcount", null, headers);
            Assert.assertEquals(20, count.intValue());

            final ClientPrimitiveValue value = (ClientPrimitiveValue) template.requestBodyAndHeaders("direct://readvalue", null, headers);
            LOG.info("Client value \"{}\" has type {}", value.toString(), value.getTypeName());
            Assert.assertEquals("Male", value.asPrimitive().toString());

            final ClientPrimitiveValue singleProperty = (ClientPrimitiveValue) template.requestBodyAndHeaders("direct://readsingleprop", null, headers);
            Assert.assertTrue(singleProperty.isPrimitive());
            Assert.assertEquals("San Francisco International Airport", singleProperty.toString());

            final ClientComplexValue complexProperty = (ClientComplexValue) template.requestBodyAndHeaders("direct://readcomplexprop", null, headers);
            Assert.assertTrue(complexProperty.isComplex());
            Assert.assertEquals("San Francisco", complexProperty.get("City").getComplexValue().get("Name").getValue().toString());

            final ClientEntity entity = (ClientEntity) template.requestBodyAndHeaders("direct://readentitybyid", null, headers);
            Assert.assertNotNull(entity);
            Assert.assertEquals("Russell", entity.getProperty("FirstName").getValue().toString());

            final ClientEntity unbFuncReturn = (ClientEntity) template.requestBodyAndHeaders("direct://callunboundfunction", null, headers);
            Assert.assertNotNull(unbFuncReturn);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testCreateUpdateDelete() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() {
                from("direct://create-entity").to("olingo4://create/People");

                from("direct://update-entity").to("olingo4://update/People('lewisblack')");

                from("direct://delete-entity").to("olingo4://delete/People('lewisblack')");

                from("direct://read-deleted-entity").to("olingo4://delete/People('lewisblack')");

                from("direct://batch").to("olingo4://batch");
            }
        });
        camelctx.start();
        
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            final ClientEntity clientEntity = createEntity();

            ClientEntity entity = template.requestBody("direct://create-entity", clientEntity, ClientEntity.class);
            Assert.assertNotNull(entity);
            Assert.assertEquals("Lewis", entity.getProperty("FirstName").getValue().toString());
            Assert.assertEquals("", entity.getProperty("MiddleName").getValue().toString());

            // update
            clientEntity.getProperties().add(objFactory.newPrimitiveProperty("MiddleName", objFactory.newPrimitiveValueBuilder().buildString("Lewis")));

            HttpStatusCode status = template.requestBody("direct://update-entity", clientEntity, HttpStatusCode.class);
            Assert.assertNotNull("Update status", status);
            Assert.assertEquals("Update status", HttpStatusCode.NO_CONTENT.getStatusCode(), status.getStatusCode());
            LOG.info("Update entity status: {}", status);

            // delete
            status = template.requestBody("direct://delete-entity", null, HttpStatusCode.class);
            Assert.assertNotNull("Delete status", status);
            Assert.assertEquals("Delete status", HttpStatusCode.NO_CONTENT.getStatusCode(), status.getStatusCode());
            LOG.info("Delete status: {}", status);

            // check for delete
            try {
                template.requestBody("direct://read-deleted-entity", null, HttpStatusCode.class);
            } catch (CamelExecutionException e) {
                Assert.assertEquals("Resource Not Found [HTTP/1.1 404 Not Found]", e.getCause().getMessage());
            }
        } finally {
            camelctx.stop();
        }
    }

    private ClientEntity createEntity() {
        ClientEntity clientEntity = objFactory.newEntity(null);

        clientEntity.getProperties().add(objFactory.newPrimitiveProperty("UserName", objFactory.newPrimitiveValueBuilder().buildString("lewisblack")));
        clientEntity.getProperties().add(objFactory.newPrimitiveProperty("FirstName", objFactory.newPrimitiveValueBuilder().buildString("Lewis")));
        clientEntity.getProperties().add(objFactory.newPrimitiveProperty("LastName", objFactory.newPrimitiveValueBuilder().buildString("Black")));

        return clientEntity;
    }

    private CamelContext createCamelContext() throws Exception {

        final CamelContext context = new DefaultCamelContext();

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("serviceUri", getRealServiceUrl(TEST_SERVICE_BASE_URL));
        options.put("contentType", "application/json;charset=utf-8");

        final Olingo4Configuration configuration = new Olingo4Configuration();
        IntrospectionSupport.setProperties(configuration, options);

        // add OlingoComponent to Camel context
        final Olingo4Component component = new Olingo4Component(context);
        component.setConfiguration(configuration);
        context.addComponent("olingo4", component);

        return context;
    }

    /*
     * Every request to the demo OData 4.0
     * (http://services.odata.org/TripPinRESTierService) generates unique
     * service URL with postfix like (S(tuivu3up5ygvjzo5fszvnwfv)) for each
     * session This method makes reuest to the base URL and return URL with
     * generated postfix
     */
    private String getRealServiceUrl(String baseUrl) throws ClientProtocolException, IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(baseUrl);
        HttpContext httpContext = new BasicHttpContext();
        httpclient.execute(httpGet, httpContext);
        HttpUriRequest currentReq = (HttpUriRequest) httpContext.getAttribute(HttpCoreContext.HTTP_REQUEST);
        HttpHost currentHost = (HttpHost) httpContext.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
        String currentUrl = (currentReq.getURI().isAbsolute()) ? currentReq.getURI().toString() : (currentHost.toURI() + currentReq.getURI());

        return currentUrl;
    }
}
