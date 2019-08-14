/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.cdi;

import java.util.List;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.apache.camel.ValueHolder;
import org.apache.camel.cdi.Uri;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.engine.EndpointKey;
import org.apache.camel.spi.EndpointRegistry;
import org.apache.camel.support.CamelContextHelper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.cdi.subA.Constants;
import org.wildfly.camel.test.cdi.subA.RouteBuilderD;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
public class CDIIntegrationTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Inject
    RouteBuilderD routesD;

    @Inject
    @Uri(value = "seda:foo")
    ProducerTemplate producerD;

    @Deployment
    public static JavaArchive createDeployment() {
        // Note, this needs to have the *.jar suffix
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-cdi-tests.jar");
        archive.addClasses(Constants.class, RouteBuilderD.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    @Test
    public void checkContextsHaveCorrectEndpointsAndRoutes() throws Exception {

        CamelContext contextD = assertSingleCamelContext();
        assertHasEndpoints(contextD, "seda://D.a", "mock://D.b");

        MockEndpoint mockEndpointD = routesD.b;
        mockEndpointD.expectedBodiesReceived(Constants.EXPECTED_BODIES_D);
        routesD.sendMessages();
        mockEndpointD.assertIsSatisfied();

        MockEndpoint mockDb = CamelContextHelper.getMandatoryEndpoint(contextD, "mock://D.b", MockEndpoint.class);
        mockDb.reset();
        mockDb.expectedBodiesReceived(Constants.EXPECTED_BODIES_D_A);
        for (Object body : Constants.EXPECTED_BODIES_D_A) {
            producerD.sendBody("seda:D.a", body);
        }
        mockDb.assertIsSatisfied();
    }

    private void assertHasEndpoints(CamelContext context, String... uris) {
        EndpointRegistry<? extends ValueHolder<String>> registry = context.getEndpointRegistry();
        for (String uri : uris) {
            Endpoint endpoint = registry.get(new EndpointKey(uri));
            Assert.assertNotNull("CamelContext " + context + " does not have an Endpoint with URI " + uri + " but has " + registry.keySet(), endpoint);
        }
    }

    private CamelContext assertSingleCamelContext() {
        List<String> ctxnames = contextRegistry.getCamelContextNames();
        Assert.assertEquals("Expected single camel context: " + ctxnames, 1, ctxnames.size());
        CamelContext camelctx = contextRegistry.getCamelContext(ctxnames.iterator().next());
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        return camelctx;
    }
}
