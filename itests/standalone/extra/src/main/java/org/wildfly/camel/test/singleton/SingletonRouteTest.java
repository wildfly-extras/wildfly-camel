/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.camel.test.singleton;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.runtime.ServiceLocator;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.clustering.server.singleton.SingletonService;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.camel.SerializableCamelContext;
import org.wildfly.extension.camel.SerializableCamelContextService;

@CamelAware
@RunWith(Arquillian.class)
public class SingletonRouteTest {

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "singleton-route-test");
        archive.addAsResource("singleton/singleton-camel-context.xml", "singleton-service.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.wildfly.clustering.server");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testSerializableContext() throws Exception {

        // Serialize the context definition
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(getContextDefinition());
        }

        SerializableCamelContext contextDef;

        // Deserialize the context definition
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            contextDef = (SerializableCamelContext) ois.readObject();
        }

        // Get the context from the definition
        CamelContext camelctx = contextDef.getCamelContext();

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testSerializableService() throws Exception {

        ServiceName serviceName = CamelConstants.CAMEL_BASE_NAME.append("my-service");
        ServiceContainer container = ServiceLocator.getRequiredService(ServiceContainer.class);

        SerializableCamelContextService service = new SerializableCamelContextService(getContextDefinition());
        ServiceController<SerializableCamelContext> controller = container.addService(serviceName, service).install();

        // Get the context from the service
        CamelContext camelctx = controller.awaitValue().getCamelContext();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            controller.setMode(Mode.REMOVE);
        }
    }

    @Test
    @Ignore("[#77] Provide example for HA singleton service")
    public void testSingletonService() throws Exception {

        ServiceName serviceName = CamelConstants.CAMEL_BASE_NAME.append("my-singleton-service");
        ServiceContainer container = ServiceLocator.getRequiredService(ServiceContainer.class);

        SerializableCamelContextService service = new SerializableCamelContextService(getContextDefinition());
        SingletonService<SerializableCamelContext> singleton = new SingletonService<>(serviceName, service);
        ServiceController<SerializableCamelContext> controller = singleton.build(container).setInitialMode(Mode.ACTIVE).install();

        // Get the context from the service
        CamelContext camelctx = controller.awaitValue().getCamelContext();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            controller.setMode(Mode.REMOVE);
        }
    }

    private SerializableCamelContext getContextDefinition() {
        URL contextUrl = getClass().getResource("/singleton-service.xml");
        Module module = ((ModuleClassLoader)getClass().getClassLoader()).getModule();
        return new SerializableCamelContext(module.getIdentifier(), contextUrl);
    }
}
