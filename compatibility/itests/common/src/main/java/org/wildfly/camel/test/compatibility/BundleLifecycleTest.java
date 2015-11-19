/*
 * #%L
 * Gravia :: Integration Tests :: Common
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
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
package org.wildfly.camel.test.compatibility;

import java.io.InputStream;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.osgi.StartLevelAware;
import org.jboss.gravia.Constants;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceReference;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Test simple bundle lifecycle
 *
 * @author thomas.diesler@jboss.com
 * @since 11-Jun-2014
 */
@RunWith(Arquillian.class)
public class BundleLifecycleTest {

    @Deployment
    @StartLevelAware(autostart = true)
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "simple-bundle.jar");
        archive.addClasses(BundleLifecycleTest.class, Activator.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName("simple-bundle");
                builder.addBundleVersion("1.0.0");
                builder.addBundleActivator(Activator.class);
                builder.addManifestHeader(Constants.GRAVIA_ENABLED, Boolean.TRUE.toString());
                builder.addImportPackages(RuntimeLocator.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testModuleLifecycle() throws Exception {

        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        Module modA = runtime.getModule(getClass().getClassLoader());
        Assert.assertEquals(Module.State.ACTIVE, modA.getState());

        String symbolicName = modA.getHeaders().get("Bundle-SymbolicName");
        Assert.assertEquals("simple-bundle", symbolicName);

        ModuleContext context = modA.getModuleContext();
        Module sysmodule = runtime.getModule(0);
        symbolicName = sysmodule.getHeaders().get("Bundle-SymbolicName");
        Assert.assertNotNull("System bundle symbolic name not null", symbolicName);

        ServiceReference<String> sref = context.getServiceReference(String.class);
        String service = context.getService(sref);
        Assert.assertEquals("Hello", service);

        modA.stop();
        Assert.assertEquals(Module.State.INSTALLED, modA.getState());

        modA.uninstall();
        Assert.assertEquals(Module.State.UNINSTALLED, modA.getState());

        try {
            modA.start();
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    public static class Activator implements BundleActivator {

        ServiceRegistration<String> sreg;

        @Override
        public void start(BundleContext context) throws Exception {
            sreg = context.registerService(String.class, new String("Hello"), null);
        }

        @Override
        public void stop(BundleContext context) throws Exception {
            sreg.unregister();
        }
    }
}
