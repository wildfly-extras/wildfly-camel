/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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

package org.wildfly.camel.test.undertow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.io.InputStream;
import java.net.HttpURLConnection;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.runtime.ServiceLocator;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowService;

@CamelAware
@RunWith(Arquillian.class)
public class UndertowHandlerTest {

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "undertow-handler-test");
        archive.addClasses(HttpRequest.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.wildfly.extension.undertow,io.undertow.core");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testHttpEndpoint() throws Exception {

        ServiceContainer container = ServiceLocator.getRequiredService(ServiceContainer.class);
        ServiceName hostServiceName = UndertowService.virtualHostName("default-server", "default-host");
        Host host = (Host) container.getRequiredService(hostServiceName).getValue();

        final StringBuilder result = new StringBuilder();
        host.registerHandler("/myapp/myservice", new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                String name = exchange.getQueryParameters().get("name").getFirst();
                result.append("Hello " + name);
            }});

        HttpResponse response = HttpRequest.get("http://localhost:8080/myapp/myservice?name=Kermit").getResponse();
        Assert.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        Assert.assertEquals("Hello Kermit", result.toString());
    }
}
