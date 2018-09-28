/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2018 RedHat
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
package org.wildfly.camel.test.smoke;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.CamelContextTrackerRegistry;
import org.apache.camel.spi.CamelContextTracker;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.utils.WildFlyCli;
import org.wildfly.extension.camel.CamelAware;

/**
 * Verifies that there is only ever 1 CamelContextTracker registered
 */
@RunAsClient
@RunWith(Arquillian.class)
public class ContextTrackerRegistryTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-context-tracker-tests.jar")
            .addClass(TrackerCountReportingRouteBuilder.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testRegisteredContextTrackerCount() throws Exception {
        String responseA = HttpRequest.get("http://localhost:8080/trackerCount").getResponse().getBody();
        Assert.assertEquals("1", responseA);

        reloadAppServer();

        String responseB = HttpRequest.get("http://localhost:8080/trackerCount").getResponse().getBody();
        Assert.assertEquals("1", responseB);
    }

    private void reloadAppServer() throws IOException, InterruptedException {
        WildFlyCli.run("reload").assertSuccess();
    }

    @ApplicationScoped
    @CamelAware
    static class TrackerCountReportingRouteBuilder  extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("undertow:http://localhost:8080/trackerCount")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Field field = CamelContextTrackerRegistry.class.getDeclaredField("trackers");
                        field.setAccessible(true);
                        Set<CamelContextTracker> trackers = (Set<CamelContextTracker>) field.get(CamelContextTrackerRegistry.INSTANCE);
                        exchange.getOut().setBody(String.valueOf(trackers.size()));
                        field.setAccessible(false);
                    }
                });
        }
    }

}
