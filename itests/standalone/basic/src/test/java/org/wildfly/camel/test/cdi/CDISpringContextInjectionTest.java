/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2016 RedHat
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

package org.wildfly.camel.test.cdi;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.cdi.Uri;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.cdi.subA.RouteBuilderG;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
public class CDISpringContextInjectionTest {

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Inject
    @ContextName("contextG")
    @Uri(value = "direct:start")
    ProducerTemplate producerG;

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        archive.addAsResource("cdi/camel-context.xml");
        archive.addClasses(RouteBuilderG.class);
        return archive;
    }

    @Test
    public void testCDIContextCreation() throws InterruptedException {

        assertCamelContext("contextG");

        String result = producerG.requestBody("direct:start", "Kermit", String.class);
        Assert.assertEquals("Hello Kermit", result);
    }

    private CamelContext assertCamelContext(String contextName) {
        CamelContext camelctx = contextRegistry.getCamelContext(contextName);
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        return camelctx;
    }
}
