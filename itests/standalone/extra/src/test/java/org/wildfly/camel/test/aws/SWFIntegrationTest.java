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
package org.wildfly.camel.test.aws;

import javax.inject.Inject;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws.swf.SWFConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.aws.subA.SWFClientProducer;
import org.wildfly.camel.test.aws.subA.SWFClientProducer.SWFClientProvider;
import org.wildfly.camel.test.common.aws.BasicCredentialsProvider;
import org.wildfly.camel.test.common.aws.SWFUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;

@CamelAware
@RunWith(Arquillian.class)
public class SWFIntegrationTest {
    
    @Inject
    private SWFClientProvider provider;
    
    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "aws-swf-tests.jar");
        archive.addClasses(SWFClientProducer.class, SWFUtils.class, BasicCredentialsProvider.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }
    
    @Test
    public void deciderAndWorker() throws Exception {
        
        AmazonSimpleWorkflowClient swfClient = provider.getClient();
        Assume.assumeNotNull("AWS client not null", swfClient);
        
        WildFlyCamelContext camelctx = new WildFlyCamelContext();
        camelctx.getNamingContext().bind("swfClient", swfClient);
        SWFUtils.addRoutes(camelctx);
        
        MockEndpoint decider = camelctx.getEndpoint("mock:decider", MockEndpoint.class);
        MockEndpoint worker = camelctx.getEndpoint("mock:worker", MockEndpoint.class);
        MockEndpoint starter = camelctx.getEndpoint("mock:starter", MockEndpoint.class);
        
        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.sendBody("direct:start", "Hello world!");
            
            starter.expectedMessageCount(1);
            decider.expectedMinimumMessageCount(1);
            worker.expectedMessageCount(2);
            
            String workflowId = starter.getReceivedExchanges().get(0).getIn().getHeader(SWFConstants.WORKFLOW_ID, String.class);
            Assert.assertNotNull(SWFConstants.WORKFLOW_ID + " not null", workflowId);
            SWFUtils.terminateWorkflowExecution(swfClient, workflowId);
            
        } finally {
            camelctx.stop();
        }
    }
}
