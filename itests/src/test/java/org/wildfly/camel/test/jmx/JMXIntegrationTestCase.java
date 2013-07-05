/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.jmx;

import java.io.InputStream;

import javax.management.monitor.MonitorNotification;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.osgi.metadata.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.CamelContextFactory;

/**
 * Deploys a test which monitors an JMX attrbute of a route.
 *
 * @author thomas.diesler@jboss.com
 * @since 03-Jun-2013
 */
@Ignore
@RunWith(Arquillian.class)
public class JMXIntegrationTestCase {

    @ArquillianResource
    CamelContextFactory contextFactory;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "jmx-integration-tests");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.apache.camel,org.wildfly.camel");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testMonitorMBeanAttribute() throws Exception {

        CamelContext camelctx = contextFactory.createWildflyCamelContext(getClass().getClassLoader());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jmx:platform?format=raw&objectDomain=org.apache.camel&key.context=localhost/system-context-1&key.type=routes&key.name=\"route1\"" +
                "&monitorType=counter&observedAttribute=ExchangesTotal&granularityPeriod=500").
                to("direct:end");
            }
        });
        camelctx.start();

        ConsumerTemplate consumer = camelctx.createConsumerTemplate();
        MonitorNotification notifcation = consumer.receiveBody("direct:end", MonitorNotification.class);
        Assert.assertEquals("ExchangesTotal", notifcation.getObservedAttribute());
    }
}
