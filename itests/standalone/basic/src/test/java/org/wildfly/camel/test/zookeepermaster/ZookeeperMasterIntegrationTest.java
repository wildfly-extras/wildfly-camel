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
package org.wildfly.camel.test.zookeepermaster;

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class ZookeeperMasterIntegrationTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "zookeeper-master-tests");
        archive.addClasses(CuratorFactoryBean.class, ZKServerFactoryBean.class);
        archive.addAsResource("zookeepermaster/MasterQuartz2-camel-context.xml");
        return archive;
    }

    @Test
    public void testEndpoint() throws Exception {
        
        CamelContext camelctx = contextRegistry.getCamelContext("master-quartz2");
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        
        MockEndpoint mockResult = camelctx.getEndpoint("mock:results", MockEndpoint.class);
        mockResult.expectedMinimumMessageCount(2);
        
        MockEndpoint.assertIsSatisfied(10, TimeUnit.SECONDS, mockResult);
    }
}
