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
package org.wildfly.camel.test.ganglia;

import static org.apache.camel.component.ganglia.GangliaConfiguration.DEFAULT_DMAX;
import static org.apache.camel.component.ganglia.GangliaConfiguration.DEFAULT_METRIC_NAME;
import static org.apache.camel.component.ganglia.GangliaConfiguration.DEFAULT_SLOPE;
import static org.apache.camel.component.ganglia.GangliaConfiguration.DEFAULT_TMAX;
import static org.apache.camel.component.ganglia.GangliaConfiguration.DEFAULT_TYPE;
import static org.apache.camel.component.ganglia.GangliaConfiguration.DEFAULT_UNITS;

import info.ganglia.gmetric4j.xdr.v31x.Ganglia_metadata_msg;
import info.ganglia.gmetric4j.xdr.v31x.Ganglia_value_msg;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.camel.test.ganglia.subA.FakeGangliaAgent;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class GangliaIntegrationTest {

    @ArquillianResource
    private CamelContextRegistry registry;

    @Deployment(testable = false, order = 1, name = "fake-ganglia-agent.jar")
    public static JavaArchive createFakeGangliaAgentDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "fake-ganglia-agent.jar")
            .addClasses(FakeGangliaAgent.class, AvailablePortFinder.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            .setManifest(() -> {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "io.netty,info.ganglia.gmetric4j");
                return builder.openStream();
            });
    }

    @Deployment(order = 2)
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-ganglia-tests.jar");
    }

    @Test
    public void sendDefaultConfigurationShouldSucceed() throws Exception {
        CamelContext camelctx = registry.getCamelContext("ganglia-camel-context");
        Assert.assertNotNull("Expected ganglia-camel-context to not be null", camelctx);

        ProducerTemplate template = camelctx.createProducerTemplate();
        Integer port = template.requestBody("direct:getPort", null, Integer.class);

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.setMinimumExpectedMessageCount(0);
        mockEndpoint.setAssertPeriod(100L);
        mockEndpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                Ganglia_metadata_msg metadataMessage = exchange.getIn().getBody(Ganglia_metadata_msg.class);
                if (metadataMessage != null) {
                    Assert.assertEquals(DEFAULT_METRIC_NAME, metadataMessage.gfull.metric.name);
                    Assert.assertEquals(DEFAULT_TYPE.getGangliaType(), metadataMessage.gfull.metric.type);
                    Assert.assertEquals(DEFAULT_SLOPE.getGangliaSlope(), metadataMessage.gfull.metric.slope);
                    Assert.assertEquals(DEFAULT_UNITS, metadataMessage.gfull.metric.units);
                    Assert.assertEquals(DEFAULT_TMAX, metadataMessage.gfull.metric.tmax);
                    Assert.assertEquals(DEFAULT_DMAX, metadataMessage.gfull.metric.dmax);
                } else {
                    Ganglia_value_msg valueMessage = exchange.getIn().getBody(Ganglia_value_msg.class);
                    if (valueMessage != null) {
                        Assert.assertEquals("28.0", valueMessage.gstr.str);
                        Assert.assertEquals("%s", valueMessage.gstr.fmt);
                    } else {
                        Assert.fail("Mock endpoint should only receive non-null metadata or value messages");
                    }
                }
            }
        });

        template.sendBody(String.format("ganglia:localhost:%d?mode=UNICAST", port), "28.0");

        mockEndpoint.assertIsSatisfied();
    }
}
