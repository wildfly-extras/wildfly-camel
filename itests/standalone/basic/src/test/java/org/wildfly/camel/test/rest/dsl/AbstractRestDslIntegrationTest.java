package org.wildfly.camel.test.rest.dsl;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.RestConfiguration;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest.HttpRequestBuilder;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public abstract class AbstractRestDslIntegrationTest {

    private static final String[] CORS_HEADERS = { "Access-Control-Allow-Origin", "Access-Control-Allow-Methods",
        "Access-Control-Allow-Headers", "Access-Control-Max-Age" };

    @ArquillianResource
    protected Deployer deployer;

    protected HttpClient client;

    @Before
    public void setUp() {
        this.client = createHttpClient();
    }

    @Test
    public void testRestDsl() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.setRestConfiguration(createRestConfiguration());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                rest()
                    .get("/test")
                        .route()
                            .setBody(constant("GET: /test"))
                        .endRest()

                    .get("/foo/bar")
                        .route()
                           .setBody(constant("GET: /foo/bar"))
                        .endRest()

                    .get("/test/{id}")
                        .route()
                            .setBody(simple("GET: /test/${header.id}"))
                        .endRest()

                    .post("/test")
                        .route()
                            .setBody(constant("POST: /test"))
                        .endRest()

                    .put("/test/{id}")
                        .route()
                            .setBody(simple("PUT: /test/${header.id}"))
                        .endRest()

                    .delete("/test/{id}")
                        .route()
                            .setBody(simple("DELETE: /test/${header.id}"))
                        .endRest();
            }
        });
        camelctx.start();
        try {
            String body = client.getResponse("test", "GET").getBody();
            Assert.assertEquals("GET: /test", body);

            body = client.getResponse("foo/bar", "GET").getBody();
            Assert.assertEquals("GET: /foo/bar", body);

            body = client.getResponse("test/1", "GET").getBody();
            Assert.assertEquals("GET: /test/1", body);

            body = client.getResponse("test", "POST").getBody();
            Assert.assertEquals("POST: /test", body);

            body = client.getResponse("test/1", "PUT").getBody();
            Assert.assertEquals("PUT: /test/1", body);

            body = client.getResponse("test/1", "DELETE").getBody();
            Assert.assertEquals("DELETE: /test/1", body);

            int statusCode = client.getResponse("test/foo/bar", "GET").getStatusCode();
            Assert.assertEquals(404, statusCode);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testRestDslWithContextPath() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.setRestConfiguration(createRestConfiguration("camel-rest-dsl-tests"));
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                rest("/test")
                    .get("/")
                        .route()
                            .setBody(constant("GET: /test"))
                        .endRest()

                    .get("/foo/bar")
                        .route()
                            .setBody(constant("GET: /test/foo/bar"))
                        .endRest()

                    .get("/{id}")
                        .route()
                            .setBody(simple("GET: /test/${header.id}"))
                        .endRest()

                    .post("/")
                        .route()
                            .setBody(constant("POST: /test"))
                        .endRest()

                    .put("/{id}")
                        .route()
                            .setBody(simple("PUT: /test/${header.id}"))
                        .endRest()

                    .delete("/{id}")
                        .route()
                            .setBody(simple("DELETE: /test/${header.id}"))
                        .endRest();
            }
        });

        camelctx.start();
        try {
            String body = client.getResponse("camel-rest-dsl-tests/test", "GET").getBody();
            Assert.assertEquals("GET: /test", body);

            body = client.getResponse("camel-rest-dsl-tests/test/foo/bar", "GET").getBody();
            Assert.assertEquals("GET: /test/foo/bar", body);

            body = client.getResponse("camel-rest-dsl-tests/test/1", "GET").getBody();
            Assert.assertEquals("GET: /test/1", body);

            body = client.getResponse("camel-rest-dsl-tests/test", "POST").getBody();
            Assert.assertEquals("POST: /test", body);

            body = client.getResponse("camel-rest-dsl-tests/test/1", "PUT").getBody();
            Assert.assertEquals("PUT: /test/1", body);

            body = client.getResponse("camel-rest-dsl-tests/test/1", "DELETE").getBody();
            Assert.assertEquals("DELETE: /test/1", body);

            int statusCode = client.getResponse("camel-rest-dsl-tests/foo/bar", "GET").getStatusCode();
            Assert.assertEquals(404, statusCode);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testRestDslRequestWithInvalidMethod() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.setRestConfiguration(createRestConfiguration());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                rest()
                    .get("/foo/bar")
                        .route()
                            .setBody(constant("GET: /test"))
                        .endRest();
            }
        });

        camelctx.start();
        try {
            int statusCode = client.getResponse("foo/bar", "DELETE").getStatusCode();
            Assert.assertEquals(405, statusCode);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testRestDslCorsEnabled() throws Exception {
        RestConfiguration restConfiguration = createRestConfiguration();
        restConfiguration.setEnableCORS(true);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.setRestConfiguration(restConfiguration);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                rest()
                    .get("/test")
                        .route()
                            .setBody(constant("GET: /test"))
                        .endRest();
            }
        });

        camelctx.start();
        try {
            Map<String, String> headers = client.getResponse("test", "OPTIONS").getHeaders();
            for (String header : CORS_HEADERS) {
                Assert.assertTrue("Expected HTTP response header: " + header, headers.containsKey(header));
            }
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testRestDslCorsDisabled() throws Exception {
        RestConfiguration restConfiguration = createRestConfiguration();
        restConfiguration.setEnableCORS(false);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.setRestConfiguration(createRestConfiguration());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                rest()
                    .get("/test")
                        .route()
                            .setBody(constant("GET: /test"))
                        .endRest();
            }
        });

        camelctx.start();
        try {
            Map<String, String> headers = client.getResponse("test", "OPTIONS").getHeaders();
            for (String header : CORS_HEADERS) {
                Assert.assertFalse(headers.containsKey(header));
            }
        } finally {
            camelctx.stop();
        }
    }

    protected abstract String getComponentName();

    protected String getDefaultContextPath() {
        return "/";
    }

    protected HttpClient createHttpClient() {
        return new HttpClient() {
            @Override
            public HttpResponse getResponse(String url, String method) throws Exception {
                return new HttpRequestBuilder(getEndpointUrl(url), method)
                    .throwExceptionOnFailure(false)
                    .getResponse();
            }
        };
    }

    protected int getPort() {
        return 8080;
    }

    private String getEndpointUrl(String path) {
        return String.format("http://localhost:%d%s%s", getPort(), getDefaultContextPath(), path);
    }

    private RestConfiguration createRestConfiguration() {
        return createRestConfiguration(null);
    }

    private RestConfiguration createRestConfiguration(String contextPath) {
        RestConfiguration configuration = new RestConfiguration();
        configuration.setComponent(getComponentName());
        configuration.setHost("localhost");
        configuration.setPort(getPort());

        // camel-servlet always requires a context path
        if (getComponentName().equals("servlet")) {
            configuration.setContextPath(getDefaultContextPath());
        } else {
            configuration.setContextPath(contextPath);
        }

        return configuration;
    }

    protected interface HttpClient {
        HttpResponse getResponse(String url, String method) throws Exception;
    }
}
