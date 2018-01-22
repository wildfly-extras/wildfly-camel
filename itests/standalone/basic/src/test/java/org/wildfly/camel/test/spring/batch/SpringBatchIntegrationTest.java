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
package org.wildfly.camel.test.spring.batch;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
public class SpringBatchIntegrationTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "spring-batch-tests");
        archive.addAsResource("spring/batch/springbatch-camel-context.xml");
        return archive;
    }

    @Test
    public void testEchoInBatch() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("camel");
        Assert.assertNotNull(camelctx);

        MockEndpoint outputEndpoint = camelctx.getEndpoint("mock:output", MockEndpoint.class);
        outputEndpoint.expectedBodiesReceived("Echo foo", "Echo bar", "Echo baz");

        ProducerTemplate template = camelctx.createProducerTemplate();
        for (String message : new String[] { "foo", "bar", "baz", null }) {
            template.sendBody("seda:inputQueue", message);
        }

        template.sendBody("direct:start", "Start batch!");
        outputEndpoint.assertIsSatisfied();
    }
}
