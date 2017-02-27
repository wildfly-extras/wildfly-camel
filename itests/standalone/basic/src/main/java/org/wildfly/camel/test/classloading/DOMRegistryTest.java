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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class DOMRegistryTest {

    @Deployment
    public static JavaArchive deployment() {
        return ShrinkWrap.create(JavaArchive.class, "dom-registry-tests")
            .addClass(EnvironmentUtils.class);
    }

    @Test
    public void testSystemClassloader() throws Exception {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementation domImpl = registry.getDOMImplementation("LS 3.0");
            Assert.assertNotNull("DOMImplementation not null", domImpl);
            Assert.assertEquals(domImpl.getClass().getName(), getExpectedDOMImplClassName());
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    @Test
    public void testDeploymentClassloader() throws Exception {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader classLoader = DOMRegistryTest.class.getClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementation domImpl = registry.getDOMImplementation("LS 3.0");
            Assert.assertNotNull("DOMImplementation not null", domImpl);
            Assert.assertEquals(domImpl.getClass().getName(), getExpectedDOMImplClassName());
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    @Test
    public void testDefaultClassloader() throws Exception {
        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        DOMImplementation domImpl = registry.getDOMImplementation("LS 3.0");
        Assert.assertNotNull("DOMImplementation not null", domImpl);
        Assert.assertEquals(domImpl.getClass().getName(), getExpectedDOMImplClassName());
    }

    private String getExpectedDOMImplClassName() {
        if (EnvironmentUtils.isIbmJDK()) {
            return "org.apache.xerces.dom.CoreDOMImplementationImpl";
        }

        return "com.sun.org.apache.xerces.internal.dom.CoreDOMImplementationImpl";
    }
}
