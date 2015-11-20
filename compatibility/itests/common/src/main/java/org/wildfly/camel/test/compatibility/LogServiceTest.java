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
import org.jboss.gravia.resource.Resource;
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
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test LogService access
 *
 * @author thomas.diesler@jboss.com
 * @since 04-Oct-2013
 */
@RunWith(Arquillian.class)
public class LogServiceTest  {

    @Deployment
    @StartLevelAware(autostart = true)
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "logging-test.jar");
        archive.addClasses(LogServiceTest.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleVersion("1.0.0");
                builder.addImportPackages(RuntimeLocator.class, LogService.class, LoggerFactory.class, Resource.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testLogService() throws Exception {
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        Module module = runtime.getModule(getClass().getClassLoader());

        LogService log = getLogService(module.getModuleContext());
        String msg = "LogServiceTest LogService ";
        log.log(LogService.LOG_ERROR, msg + " ERROR");
        log.log(LogService.LOG_WARNING, msg + " WARNING");
        log.log(LogService.LOG_INFO, msg + " INFO");
        log.log(LogService.LOG_DEBUG, msg + " DEBUG");
    }

    @Test
    public void testSLF4J() throws Exception {
        Logger logsrv = LoggerFactory.getLogger(LogServiceTest.class);
        String msg = "LogServiceTest SLF4J ";
        logsrv.error(msg + " ERROR");
        logsrv.warn(msg + " WARN");
        logsrv.info(msg + " INFO");
        logsrv.debug(msg + " DEBUG");
    }

    private LogService getLogService(ModuleContext context) {
        ServiceReference<LogService> sref = context.getServiceReference(LogService.class);
        Assert.assertNotNull("LogService not null", sref);
        return context.getService(sref);
    }
}
