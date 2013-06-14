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
package org.wildfly.camel.test.smoke;

import java.io.InputStream;
import java.util.Collection;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.osgi.metadata.ManifestBuilder;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.wildfly.camel.CamelConstants;
import org.wildfly.camel.CamelContextRegistry;
import org.wildfly.camel.test.smoke.subA.HelloBean;

/**
 * Deploys a module/bundle which contain a {@link HelloBean} referenced from a spring context definition.
 *
 * The tests expect the {@link CamelContext} to be created/started during deployment.
 * The tests then perfom a {@link CamelContext} lookup and do a simple invokation.
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Apr-2013
 */
@RunWith(Arquillian.class)
public class SpringBeanDeploymentTestCase {

    static final String SPRING_CAMEL_CONTEXT_XML = "bean-transform-camel-context.xml";

    static final String CAMEL_MODULE = "camel-module.jar";
    static final String CAMEL_BUNDLE = "camel-bundle.jar";

    @ArquillianResource
    BundleContext context;

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @ArquillianResource
    Deployer deployer;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-deployment-tests");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.apache.camel,org.jboss.msc,org.wildfly.camel");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testBeanTransformFromModule() throws Exception {
        deployer.deploy(CAMEL_MODULE);
        try {
            CamelContext camelctx = contextRegistry.getCamelContext("spring-context");
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            deployer.undeploy(CAMEL_MODULE);
        }
    }

    @Test
    public void testBeanTransformFromBundle() throws Exception {
        deployer.deploy(CAMEL_BUNDLE);
        try {
            String filter = "(name=spring-context)";
            Collection<ServiceReference<CamelContext>> srefs = context.getServiceReferences(CamelContext.class, filter);
            CamelContext camelctx = context.getService(srefs.iterator().next());
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            deployer.undeploy(CAMEL_BUNDLE);
        }
    }

    @Deployment(name = CAMEL_MODULE, managed = false, testable = false)
    public static JavaArchive getModule() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CAMEL_MODULE);
        archive.addClasses(HelloBean.class);
        archive.addAsResource("camel/simple/" + SPRING_CAMEL_CONTEXT_XML, CamelConstants.CAMEL_CONTEXT_FILE_NAME);
        return archive;
    }

    @Deployment(name = CAMEL_BUNDLE, managed = false, testable = false)
    public static JavaArchive getBundle() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CAMEL_BUNDLE);
        archive.addClasses(HelloBean.class);
        archive.addAsResource("camel/simple/" + SPRING_CAMEL_CONTEXT_XML, CamelConstants.CAMEL_CONTEXT_FILE_NAME);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                return builder.openStream();
            }
        });
        return archive;
    }
}
