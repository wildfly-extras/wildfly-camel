/*
 * #%L
 * Wildfly Camel :: Testsuite
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
package org.wildfly.camel.test.cxf.ws.secure;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.security.ClientCertSecurityDomainSetup;
import org.wildfly.camel.test.common.security.SecurityUtils;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.camel.test.cxf.ws.secure.subA.Application;
import org.wildfly.camel.test.cxf.ws.secure.subA.CxfWsRouteBuilder;
import org.wildfly.extension.camel.CamelAware;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@CamelAware
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(ClientCertSecurityDomainSetup.class)
public class CXFWSClientCertSecureProducerIntegrationTest {

    private static final Path WILDFLY_HOME = EnvironmentUtils.getWildFlyHome();
    private static final Map<String, String> PATH_ROLE_MAP = new LinkedHashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
            try {
                put("//" + new URI(Application.CXF_ENDPOINT_URI).getPath(),
                        ClientCertSecurityDomainSetup.APPLICATION_ROLE);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    };

    private static final String WS_MESSAGE_TEMPLATE = "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<Body>"
            + "<greet xmlns=\"http://subA.secure.ws.cxf.test.camel.wildfly.org/\">"
            + "<message xmlns=\"\">%s</message>"
            + "<name xmlns=\"\">%s</name>"
            + "</greet>"
            + "</Body>"
            + "</Envelope>";

    @Deployment
    public static Archive<?> deployment() throws Exception {

        // [#2935] Cannot deploy CXFWSClientCertSecureProducerIntegrationTest.war
        Thread.sleep(2000);

        final WebArchive archive = ShrinkWrap
                .create(WebArchive.class, CXFWSClientCertSecureProducerIntegrationTest.class.getSimpleName() + ".war")
                .addClasses(ClientCertSecurityDomainSetup.class, CXFWSSecureUtils.class, EnvironmentUtils.class)
                .addPackage(CxfWsRouteBuilder.class.getPackage())
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
        ;
        SecurityUtils.enhanceArchive(archive, ClientCertSecurityDomainSetup.SECURITY_DOMAIN,
                ClientCertSecurityDomainSetup.AUTH_METHOD, PATH_ROLE_MAP);
        return archive;
    }

    @Test
    public void greetAnonymous() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setSSLSocketFactory(SecurityUtils.createUntrustedClientCertSocketFactory(WILDFLY_HOME)).build()) {
            HttpPost request = new HttpPost(Application.CXF_ENDPOINT_URI);
            request.setHeader("Content-Type", "text/xml");
            request.setHeader("soapaction", "\"urn:greet\"");

            request.setEntity(
                    new StringEntity(String.format(WS_MESSAGE_TEMPLATE, "Hi", "Joe"), StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = httpclient.execute(request)) {
                Assert.assertEquals(403, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    public void greetClientCert() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setSSLSocketFactory(SecurityUtils.createTrustedClientCertSocketFactory(WILDFLY_HOME)).build()) {
            HttpPost request = new HttpPost(Application.CXF_ENDPOINT_URI);
            request.setHeader("Content-Type", "text/xml");
            request.setHeader("soapaction", "\"urn:greet\"");

            request.setEntity(
                    new StringEntity(String.format(WS_MESSAGE_TEMPLATE, "Hi", "Joe"), StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = httpclient.execute(request)) {
                Assert.assertEquals(200, response.getStatusLine().getStatusCode());

                HttpEntity entity = response.getEntity();
                String body = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                Assert.assertTrue(body.contains("Hi Joe"));
            }
        }
    }
}
