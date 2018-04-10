package org.wildfly.camel.test.tika;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.startsWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.txt.UniversalEncodingDetector;
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
public class TikaIntegrationTest {
    private static Logger log = LoggerFactory.getLogger(TikaIntegrationTest.class);

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-tika-tests.jar");
    }

    private static Context createJndiContext() throws Exception {
        Properties properties = new Properties();

        // jndi.properties is optional
        InputStream in = TikaIntegrationTest.class.getClassLoader().getResourceAsStream("jndi.properties");
        if (in != null) {
            log.debug("Using jndi.properties from classpath root");
            properties.load(in);
        } else {
            properties.put("java.naming.factory.initial", "org.apache.camel.util.jndi.CamelInitialContextFactory");
        }
        return new InitialContext(new Hashtable<Object, Object>(properties));
    }

    private static JndiRegistry createRegistryWithEmptyConfig() throws Exception {
        JndiRegistry reg = new JndiRegistry(createJndiContext());
        reg.bind("testConfig", new TikaConfig(new File("src/test/resources/tika/tika-empty.xml")));
        return reg;
    }

    @Test
    public void detectDoc() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("tika:detect").to("mock:result");
            }
        });
        MockEndpoint resultEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);

        try {
            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();
            File document = new File("src/test/resources/tika/test.doc");
            template.sendBody("direct:start", document);

            resultEndpoint.setExpectedMessageCount(1);

            resultEndpoint.expectedMessagesMatches(new Predicate() {
                @Override
                public boolean matches(Exchange exchange) {
                    Object body = exchange.getIn().getBody(String.class);
                    Assert.assertThat(body, instanceOf(String.class));
                    Assert.assertThat((String) body, containsString("application/x-tika-msoffice"));
                    return true;
                }
            });
            resultEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void detectGif() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("tika:detect").to("mock:result");
            }
        });
        MockEndpoint resultEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);

        try {
            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();
            File document = new File("src/test/resources/tika/testGIF.gif");
            template.sendBody("direct:start", document);

            resultEndpoint.setExpectedMessageCount(1);

            resultEndpoint.expectedMessagesMatches(new Predicate() {
                @Override
                public boolean matches(Exchange exchange) {
                    Object body = exchange.getIn().getBody(String.class);
                    Assert.assertThat(body, instanceOf(String.class));
                    Assert.assertThat((String) body, containsString("image/gif"));
                    return true;
                }
            });
            resultEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void parseDoc() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("tika:parse").to("mock:result");
            }
        });
        MockEndpoint resultEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);

        try {
            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();

            File document = new File("src/test/resources/tika/test.doc");
            template.sendBody("direct:start", document);

            resultEndpoint.setExpectedMessageCount(1);
            resultEndpoint.expectedMessagesMatches(new Predicate() {
                @Override
                public boolean matches(Exchange exchange) {
                    Object body = exchange.getIn().getBody(String.class);
                    Map<String, Object> headerMap = exchange.getIn().getHeaders();
                    Assert.assertThat(body, instanceOf(String.class));

                    Charset detectedCharset = null;
                    try {
                        InputStream bodyIs = new ByteArrayInputStream(((String) body).getBytes());
                        UniversalEncodingDetector encodingDetector = new UniversalEncodingDetector();
                        detectedCharset = encodingDetector.detect(bodyIs, new Metadata());
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }

                    Assert.assertThat((String) body, containsString("test"));
                    Assert.assertThat(detectedCharset.name(), startsWith(Charset.defaultCharset().name()));

                    Assert.assertThat(headerMap.get(Exchange.CONTENT_TYPE), equalTo("application/msword"));
                    return true;
                }
            });
            resultEndpoint.assertIsSatisfied();

        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void parseDocWithEmptyRegistryConfig() throws Exception {
        CamelContext camelctx = new DefaultCamelContext(createRegistryWithEmptyConfig());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("tika:parse?tikaConfig=#testConfig").to("mock:result");
            }
        });
        MockEndpoint resultEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);

        try {
            camelctx.start();
            ProducerTemplate template = camelctx.createProducerTemplate();

            File document = new File("src/test/resources/tika/test.doc");
            template.sendBody("direct:start", document);

            resultEndpoint.setExpectedMessageCount(1);

            resultEndpoint.expectedMessagesMatches(new Predicate() {
                @Override
                public boolean matches(Exchange exchange) {
                    Object body = exchange.getIn().getBody(String.class);
                    Map<String, Object> headerMap = exchange.getIn().getHeaders();
                    Assert.assertThat(body, instanceOf(String.class));
                    Assert.assertThat((String) body, containsString("<body"));
                    Assert.assertThat(headerMap.get(Exchange.CONTENT_TYPE), equalTo("application/msword"));
                    return true;
                }
            });
            resultEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void parseDocWithRegistryConfig() throws Exception {
        CamelContext camelctx = new DefaultCamelContext(createRegistryWithEmptyConfig());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("tika:parse?tikaConfig=#testConfig").to("mock:result");
            }
        });
        MockEndpoint resultEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);

        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            camelctx.start();
            File document = new File("src/test/resources/tika/test.doc");
            template.sendBody("direct:start", document);

            resultEndpoint.setExpectedMessageCount(1);

            resultEndpoint.expectedMessagesMatches(new Predicate() {
                @Override
                public boolean matches(Exchange exchange) {
                    Object body = exchange.getIn().getBody(String.class);
                    Map<String, Object> headerMap = exchange.getIn().getHeaders();
                    Assert.assertThat(body, instanceOf(String.class));
                    Assert.assertThat((String) body, containsString("<body"));
                    Assert.assertThat(headerMap.get(Exchange.CONTENT_TYPE), equalTo("application/msword"));
                    return true;
                }
            });
            resultEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void parseGif() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("tika:parse").to("mock:result");
            }
        });
        MockEndpoint resultEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);

        try {
            camelctx.start();
            ProducerTemplate template = camelctx.createProducerTemplate();

            File document = new File("src/test/resources/tika/testGIF.gif");
            template.sendBody("direct:start", document);

            resultEndpoint.setExpectedMessageCount(1);

            resultEndpoint.expectedMessagesMatches(new Predicate() {
                @Override
                public boolean matches(Exchange exchange) {
                    Object body = exchange.getIn().getBody(String.class);
                    Map<String, Object> headerMap = exchange.getIn().getHeaders();
                    Assert.assertThat(body, instanceOf(String.class));
                    Assert.assertThat((String) body, containsString("<body"));
                    Assert.assertThat(headerMap.get(Exchange.CONTENT_TYPE), equalTo("image/gif"));
                    return true;
                }
            });
            resultEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void parseOdtWithEncoding() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("tika:parse?tikaParseOutputEncoding=" + StandardCharsets.UTF_16.name())
                        .to("mock:result");
            }
        });
        MockEndpoint resultEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);

        try {
            camelctx.start();
            ProducerTemplate template = camelctx.createProducerTemplate();

            File document = new File("src/test/resources/tika/testOpenOffice2.odt");
            template.sendBody("direct:start", document);

            resultEndpoint.setExpectedMessageCount(1);

            resultEndpoint.expectedMessagesMatches(new Predicate() {
                @Override
                public boolean matches(Exchange exchange) {
                    Object body = exchange.getIn().getBody(String.class);
                    Map<String, Object> headerMap = exchange.getIn().getHeaders();
                    Assert.assertThat(body, instanceOf(String.class));

                    Charset detectedCharset = null;
                    try {
                        InputStream bodyIs = new ByteArrayInputStream(
                                ((String) body).getBytes(StandardCharsets.UTF_16));
                        UniversalEncodingDetector encodingDetector = new UniversalEncodingDetector();
                        detectedCharset = encodingDetector.detect(bodyIs, new Metadata());
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }

                    Assert.assertThat(detectedCharset.name(), startsWith(StandardCharsets.UTF_16.name()));
                    Assert.assertThat(headerMap.get(Exchange.CONTENT_TYPE),
                            equalTo("application/vnd.oasis.opendocument.text"));
                    return true;
                }
            });
            resultEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

}
