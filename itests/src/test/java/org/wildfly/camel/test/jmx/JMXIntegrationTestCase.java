/*
 * #%L
 * Wildfly Camel Testsuite
 * %%
 * Copyright (C) 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package org.wildfly.camel.test.jmx;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.List;

import javax.management.monitor.MonitorNotification;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.gravia.provision.Provisioner;
import org.jboss.gravia.provision.Provisioner.ResourceHandle;
import org.jboss.gravia.resource.IdentityNamespace;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.CamelContextFactory;
import org.wildfly.camel.test.ProvisionerSupport;

/**
 * Deploys a test which monitors an JMX attrbute of a route.
 *
 * @author thomas.diesler@jboss.com
 * @since 03-Jun-2013
 */
@RunWith(Arquillian.class)
public class JMXIntegrationTestCase {

    @ArquillianResource
    CamelContextFactory contextFactory;

    @ArquillianResource
    Provisioner provisioner;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "jmx-integration-tests");
        archive.addClasses(ProvisionerSupport.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.apache.camel,org.jboss.gravia,org.wildfly.camel");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testMonitorMBeanAttribute() throws Exception {
        ProvisionerSupport provisionerSupport = new ProvisionerSupport(provisioner);
        List<ResourceHandle> reshandles = provisionerSupport.installCapabilities(IdentityNamespace.IDENTITY_NAMESPACE, "camel.jmx.feature");
        try {
            CamelContext camelctx = contextFactory.createWildflyCamelContext(getClass().getClassLoader());
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    String host = InetAddress.getLocalHost().getHostName();
                    from("jmx:platform?format=raw&objectDomain=org.apache.camel&key.context=" + host + "/system-context-1&key.type=routes&key.name=\"route1\"" +
                    "&monitorType=counter&observedAttribute=ExchangesTotal&granularityPeriod=500").
                    to("direct:end");
                }
            });
            camelctx.start();

            ConsumerTemplate consumer = camelctx.createConsumerTemplate();
            MonitorNotification notifcation = consumer.receiveBody("direct:end", MonitorNotification.class);
            Assert.assertEquals("ExchangesTotal", notifcation.getObservedAttribute());
        } finally {
            for (ResourceHandle handle : reshandles) {
                handle.uninstall();
            }
        }
    }
}
