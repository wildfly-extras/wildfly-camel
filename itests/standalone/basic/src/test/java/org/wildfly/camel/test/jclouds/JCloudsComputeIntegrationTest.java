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
package org.wildfly.camel.test.jclouds;

import java.util.List;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.apache.camel.component.jclouds.JcloudsConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jclouds.compute.domain.Image;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
public class JCloudsComputeIntegrationTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-jclouds-compute-tests.jar")
            .addAsResource("jclouds/jclouds-camel-context.xml", "jclouds-camel-context.xml");
    }

    @Test
    public void testListImages() throws Exception {
        CamelContext camelctx = getCamelContext();
        ProducerTemplate template = camelctx.createProducerTemplate();

        MockEndpoint result = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedMessageCount(1);
        template.sendBodyAndHeader("direct:start", null, JcloudsConstants.OPERATION, JcloudsConstants.LIST_IMAGES);
        result.assertIsSatisfied();

        List<Exchange> exchanges = result.getExchanges();
        if (exchanges != null && !exchanges.isEmpty()) {
            for (Exchange exchange : exchanges) {
                Set<?> images = exchange.getIn().getBody(Set.class);
                Assert.assertTrue(images.size() > 0);
                for (Object obj : images) {
                    Assert.assertTrue(obj instanceof Image);
                }
            }
        }
    }

    private CamelContext getCamelContext() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("jclouds-camel-context");
        Assert.assertNotNull("Expected jclouds-camel-context to not be null", camelctx);
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        return camelctx;
    }
}
