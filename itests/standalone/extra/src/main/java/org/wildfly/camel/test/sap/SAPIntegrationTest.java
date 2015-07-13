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
package org.wildfly.camel.test.sap;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.impl.DefaultCamelContext;
import org.fusesource.camel.component.sap.SapQueuedIDocDestinationComponent;
import org.fusesource.camel.component.sap.SapQueuedIDocListDestinationComponent;
import org.fusesource.camel.component.sap.SapQueuedRfcDestinationComponent;
import org.fusesource.camel.component.sap.SapSynchronousRfcDestinationComponent;
import org.fusesource.camel.component.sap.SapSynchronousRfcServerComponent;
import org.fusesource.camel.component.sap.SapTransactionalIDocListDestinationComponent;
import org.fusesource.camel.component.sap.SapTransactionalIDocListServerComponent;
import org.fusesource.camel.component.sap.SapTransactionalRfcDestinationComponent;
import org.fusesource.camel.component.sap.SapTransactionalRfcServerComponent;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class SAPIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-sap-tests.jar");
    }

    @Before
    public void setUp() {
        try {
            ModuleLoader moduleLoader = Module.getCallerModuleLoader();
            moduleLoader.loadModule(ModuleIdentifier.create("com.sap.conn.jco"));
        } catch (ModuleLoadException ex) {
            Assume.assumeNoException(ex);
        }
    }

    @Test
    public void testIDocDestinationComponentLoads() {
        SapTransactionalIDocListDestinationComponent component = getComponent("sap-idoclist-destination", SapTransactionalIDocListDestinationComponent.class);
        Assert.assertNotNull(component);
    }

    @Test
    public void testIDocListServerComponentLoads() {
        SapTransactionalIDocListServerComponent component = getComponent("sap-idoclist-server", SapTransactionalIDocListServerComponent.class);
        Assert.assertNotNull(component);
    }

    @Test
    public void testQueuedIDocDestinationComponentLoads() {
        SapQueuedIDocDestinationComponent component = getComponent("sap-qidoc-destination", SapQueuedIDocDestinationComponent.class);
        Assert.assertNotNull(component);
    }

    @Test
    public void testQueuedIDocListDestinationComponentLoads() {
        SapQueuedIDocListDestinationComponent component = getComponent("sap-qidoclist-destination", SapQueuedIDocListDestinationComponent.class);
        Assert.assertNotNull(component);
    }

    @Test
    public void testQueuedRfcDestinationComponentLoads() {
        SapQueuedRfcDestinationComponent component = getComponent("sap-qrfc-destination", SapQueuedRfcDestinationComponent.class);
        Assert.assertNotNull(component);
    }

    @Test
    public void testSynchronousRfcDestinationComponentLoads() {
        SapSynchronousRfcDestinationComponent component = getComponent("sap-srfc-destination", SapSynchronousRfcDestinationComponent.class);
        Assert.assertNotNull(component);
    }

    @Test
    public void testSynchronousRfcServerComponentLoads() {
        SapSynchronousRfcServerComponent component = getComponent("sap-srfc-server", SapSynchronousRfcServerComponent.class);
        Assert.assertNotNull(component);
    }

    @Test
    public void testTransactionalRfcDestinationComponentLoads() {
        SapTransactionalRfcDestinationComponent component = getComponent("sap-trfc-destination", SapTransactionalRfcDestinationComponent.class);
        Assert.assertNotNull(component);
    }

    @Test
    public void testTransactionalRfcServerComponentLoads() {
        SapTransactionalRfcServerComponent component = getComponent("sap-trfc-server", SapTransactionalRfcServerComponent.class);
        Assert.assertNotNull(component);
    }

    private <T extends Component> T getComponent(String name, Class<T> componentType) {
        CamelContext camelctx = new DefaultCamelContext();
        return camelctx.getComponent(name, componentType);
    }
}
