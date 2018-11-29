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

package org.wildfly.camel.test.jbpm;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jbpm.JBPMConfiguration;
import org.apache.camel.component.jbpm.JBPMEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class JBPMIntegrationTest {

    @Deployment
    public static JavaArchive createdeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-jbpm-tests");
    }

    @Test
    public void interactsOverRest() throws Exception {

        String endpointUri = "jbpm:http://localhost:8080/business-central?userName=bpmsAdmin&password=pa$word1&deploymentId=org.kie.example:project1:1.0.0-SNAPSHOT";

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .to(endpointUri)
                    .to("mock:result");
            }
        });

        /*
         * In the future, we could follow up with something that talks to
         * https://access.redhat.com/containers/?tab=overview#/registry.access.redhat.com/rhpam-7/rhpam71-kieserver-openshift
         */

        JBPMEndpoint endpoint = camelctx.getEndpoint(endpointUri, JBPMEndpoint.class);
        JBPMConfiguration config = endpoint.getConfiguration();
        Assert.assertEquals("bpmsAdmin", config.getUserName());
    }
}
