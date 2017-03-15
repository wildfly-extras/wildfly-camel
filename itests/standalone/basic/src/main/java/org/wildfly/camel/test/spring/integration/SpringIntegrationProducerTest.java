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
package org.wildfly.camel.test.spring.integration;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.spring.SpringCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.SpringUtils;
import org.wildfly.camel.test.spring.subA.HelloWorldService;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
public class SpringIntegrationProducerTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "spring-integration-tests");
        archive.addClasses(SpringUtils.class, HelloWorldService.class);
        archive.addAsResource("spring/integration/producer-camel-context.xml");
        return archive;
    }

    @Test
    public void testSendingOneWayMessage() throws Exception {
        SpringCamelContext camelctx = (SpringCamelContext) contextRegistry.getCamelContext("camel");
        Assert.assertNotNull(camelctx);

        ProducerTemplate template = camelctx.createProducerTemplate();
        template.sendBody("direct:onewayMessage", "Greet");

        HelloWorldService service = SpringUtils.getMandatoryBean(camelctx, HelloWorldService.class, "helloService");
        Assert.assertEquals("We should call the service", service.getGreetName(), "Greet");        
    }
}
