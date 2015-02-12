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

package org.wildfly.camel.test.classloading;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.classloading.subA.CustomFailoverException;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
public class CustomFailoverExceptionTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "custom-failover.jar");
        archive.addAsManifestResource("classloading/custom-failover-context.xml", "jboss-camel-context.xml");
        archive.addClasses(CustomFailoverException.class);
        return archive;
    }

    @Test
    public void testExportedPaths() throws Exception {

        CamelContext camelctx = contextRegistry.getContext("custom-failover-context");
        Assert.assertNotNull("Context not null", camelctx);
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

        String name = CustomFailoverException.class.getName();
        Assert.assertNotNull("Class resolved", camelctx.getClassResolver().resolveClass(name));
        Assert.assertNotNull("Class loaded", camelctx.getApplicationContextClassLoader().loadClass(name));
    }
}
