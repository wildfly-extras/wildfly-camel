/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2014 RedHat
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

package org.wildfly.camel.test.aws;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.ec2.EC2Constants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;

@CamelAware
@RunWith(Arquillian.class)
public class EC2IntegrationTest {

    @Deployment
    public static JavaArchive deployment() {
        return ShrinkWrap.create(JavaArchive.class, "aws-ec2-tests");
    }

    @Test
    public void testCreateInstance() throws Exception {

        String accessId = System.getenv("AWS_ACCESS_ID");
        String secretKey = System.getenv("AWS_SECRET_KEY");
        Assume.assumeNotNull("AWS_ACCESS_ID not null", accessId);
        Assume.assumeNotNull("AWS_SECRET_KEY not null", secretKey);

        WildFlyCamelContext camelctx = new WildFlyCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                String clientref = "amazonEc2Client=#ec2Client";
                from("direct:createAndRun").to("aws-ec2://TestDomain?" + clientref + "&operation=createAndRunInstances");
                from("direct:terminate").to("aws-ec2://TestDomain?" + clientref + "&operation=terminateInstances");
            }
        });

        AmazonEC2 client = AmazonEC2ClientBuilder.standard()
                .withCredentials(new StaticCredentialsProvider(new BasicAWSCredentials(accessId, secretKey)))
                .withRegion("eu-west-1")
                .build();
        
        camelctx.getNamingContext().bind("ec2Client", client);
        
        camelctx.start();
        try {

            // Create and run an instance
            Map<String, Object> headers = new HashMap<>();
            headers.put(EC2Constants.IMAGE_ID, "ami-02ace471");
            headers.put(EC2Constants.INSTANCE_TYPE, InstanceType.T2Micro);
            headers.put(EC2Constants.SUBNET_ID, "subnet-4a8b2f3d");
            headers.put(EC2Constants.INSTANCE_MIN_COUNT, 1);
            headers.put(EC2Constants.INSTANCE_MAX_COUNT, 1);
            
            ProducerTemplate template = camelctx.createProducerTemplate();
            RunInstancesResult result1 = template.requestBodyAndHeaders("direct:createAndRun", null, headers, RunInstancesResult.class);
            String instanceId = result1.getReservation().getInstances().get(0).getInstanceId();
            System.out.println(instanceId);
            
            // Terminate the instance 
            headers = new HashMap<>();
            headers.put(EC2Constants.INSTANCES_IDS, Collections.singleton(instanceId));
            
            TerminateInstancesResult result2 = template.requestBodyAndHeaders("direct:terminate", null, headers, TerminateInstancesResult.class);
            Assert.assertEquals(instanceId, result2.getTerminatingInstances().get(0).getInstanceId());
        } finally {
            camelctx.stop();
        }
    }
    
    static class StaticCredentialsProvider implements AWSCredentialsProvider {

        private final AWSCredentials credentials;

        public StaticCredentialsProvider(AWSCredentials credentials) {
            this.credentials = credentials;
        }

        @Override
        public AWSCredentials getCredentials() {
            return credentials;
        }

        @Override
        public void refresh() {
        }
    }
}
