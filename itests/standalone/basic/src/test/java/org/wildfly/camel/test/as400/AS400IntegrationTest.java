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
package org.wildfly.camel.test.as400;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jt400.Jt400Endpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.ibm.as400.access.AS400ConnectionPool;

@CamelAware
@RunWith(Arquillian.class)
public class AS400IntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-as400-tests");
    }


    @Test
    public void testToString() throws Exception {

        String endpointUri = "jt400://user:password@host/qsys.lib/library.lib/queue.dtaq?ccsid=500&format=binary&connectionPool=#mockPool";

        WildFlyCamelContext camelctx = new WildFlyCamelContext();
        camelctx.getNamingContext().bind("mockPool", new AS400ConnectionPool());

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(endpointUri).to("mock:end");
            }
        });

        Jt400Endpoint endpoint = camelctx.getEndpoint(endpointUri, Jt400Endpoint.class);
        Assert.assertEquals("host", endpoint.getSystemName());
    }
}
