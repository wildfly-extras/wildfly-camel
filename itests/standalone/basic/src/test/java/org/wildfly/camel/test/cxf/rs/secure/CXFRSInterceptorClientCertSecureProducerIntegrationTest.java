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
package org.wildfly.camel.test.cxf.rs.secure;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

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
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.security.SecurityInInterceptor;
import org.wildfly.camel.test.common.security.SecurityUtils;
import org.wildfly.camel.test.common.security.SslSetup;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.camel.test.cxf.rs.secure.subA.GreetingsProcessor;
import org.wildfly.camel.test.cxf.rs.secure.subA.GreetingsService;
import org.wildfly.extension.camel.CamelAware;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@CamelAware
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(SslSetup.class)
public class CXFRSInterceptorClientCertSecureProducerIntegrationTest {

    private static final Path WILDFLY_HOME = EnvironmentUtils.getWildFlyHome();

    @Deployment
    public static Archive<?> deployment() {
        final WebArchive archive = ShrinkWrap
                .create(WebArchive.class, CXFRSInterceptorClientCertSecureProducerIntegrationTest.class.getSimpleName() + ".war")
                .addClasses(SslSetup.class, CXFRSSecureUtils.class, EnvironmentUtils.class, GreetingsService.class,
                        GreetingsProcessor.class, SecurityInInterceptor.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
        ;
        SecurityUtils.addSpringXml(archive, "cxfrs-interceptor-camel-context.xml", CXFRSSecureUtils.SPRING_CONSUMER_ENDPOINT_BASE_ADDRESS);
        archive.as(ZipExporter.class).exportTo(new File("target/"+ CXFRSInterceptorClientCertSecureProducerIntegrationTest.class.getSimpleName() + ".war"), true);
        return archive;
    }

    @Test
    public void greetAnonymous() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setSSLSocketFactory(SecurityUtils.createUntrustedClientCertSocketFactory(WILDFLY_HOME)).build()) {
            HttpPost request = new HttpPost(CXFRSSecureUtils.SPRING_CONSUMER_ENDPOINT_ADDRESS);
            request.setHeader("Content-Type", "text/plain");

            request.setEntity(new StringEntity("Joe", StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = httpclient.execute(request)) {
                Assert.assertEquals(403, response.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    public void greetClientCert() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setSSLSocketFactory(SecurityUtils.createTrustedClientCertSocketFactory(WILDFLY_HOME))
                .build()) {
            HttpPost request = new HttpPost(CXFRSSecureUtils.SPRING_CONSUMER_ENDPOINT_ADDRESS);
            request.setHeader("Content-Type", "text/plain");

            request.setEntity(new StringEntity("Joe", StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = httpclient.execute(request)) {
                final int actualCode = response.getStatusLine().getStatusCode();
                Assert.assertEquals(200, actualCode);
                if (actualCode == 200) {
                    HttpEntity entity = response.getEntity();
                    String body = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                    Assert.assertEquals("Hi Joe", body);
                }
            }
        }
    }
}
