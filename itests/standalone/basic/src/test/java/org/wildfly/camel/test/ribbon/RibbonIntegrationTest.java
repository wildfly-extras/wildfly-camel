/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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

package org.wildfly.camel.test.ribbon;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.ribbon.RibbonConfiguration;
import org.apache.camel.component.ribbon.cloud.RibbonServiceLoadBalancer;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.cloud.DefaultServiceDefinition;
import org.apache.camel.impl.cloud.StaticServiceDiscovery;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@CamelAware
@RunWith(Arquillian.class)
public class RibbonIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-ribbon-tests.jar");
    }

    @Test
    public void testServiceCall() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                StaticServiceDiscovery servers = new StaticServiceDiscovery();
                /* It would be more natural to test against two services running on two distinct ports or
                 * at least on two distinct context paths, but none of those is possible because of
                 * https://github.com/wildfly-extras/wildfly-camel/issues/2129
                 * and https://issues.apache.org/jira/browse/CAMEL-11882 */
                servers.addServer(new DefaultServiceDefinition("my-service", "localhost", 8080));

                RibbonConfiguration configuration = new RibbonConfiguration();
                RibbonServiceLoadBalancer loadBalancer = new RibbonServiceLoadBalancer(configuration);

                from("direct:start") //
                        .serviceCall() //
                        .name("my-service") //
                        .uri("undertow:http://my-service/my-app")//
                        .loadBalancer(loadBalancer) //
                        .serviceDiscovery(servers) //
                        .end() //
                        .to("mock:result");

                from("undertow:http://localhost:8080/my-app") //
                        .to("mock:8080") //
                        .transform().constant("8080");
            }
        });

        MockEndpoint mockEndpoint8080 = camelctx.getEndpoint("mock:8080", MockEndpoint.class);
        mockEndpoint8080.expectedMessageCount(2);
        MockEndpoint mockEndpointResult = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpointResult.expectedMessageCount(2);

        try {
            camelctx.start();
            ProducerTemplate template = camelctx.createProducerTemplate();

            Thread.sleep(20000);
            String out = template.requestBody("direct:start", null, String.class);
            String out2 = template.requestBody("direct:start", null, String.class);
            Assert.assertEquals("8080", out);
            Assert.assertEquals("8080", out2);

            mockEndpoint8080.assertIsSatisfied();
            mockEndpointResult.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

}
